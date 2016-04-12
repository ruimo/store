package models

import scala.concurrent.duration._
import play.api.data.validation.Invalid
import constraints.FormConstraints
import play.api.data.validation.ValidationError
import play.api.Logger
import anorm._
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import helpers.PasswordHash
import java.sql.Connection
import scala.util.{Try, Failure, Success}
import com.ruimo.csv.CsvRecord
import helpers.{PasswordHash, TokenGenerator, RandomTokenGenerator}
import java.sql.SQLException
import scala.collection.{mutable, immutable}

case class StoreUser(
  id: Option[Long] = None,
  userName: String,
  firstName: String,
  middleName: Option[String],
  lastName: String,
  email: String,
  passwordHash: Long,
  salt: Long,
  deleted: Boolean,
  userRole: UserRole,
  companyName: Option[String]
) {
  def passwordMatch(password: String): Boolean =
    PasswordHash.generate(password, salt) == passwordHash
  lazy val isRegistrationIncomplete: Boolean = firstName.isEmpty
  lazy val fullName = firstName + middleName.map(n => " " + n).getOrElse("") + " " + lastName
  def isEmployeeOf(siteId: Long) = userName.startsWith(siteId + "-")
}

case class SiteUser(id: Option[Long] = None, siteId: Long, storeUserId: Long)

case class User(storeUser: StoreUser, siteUser: Option[SiteUser]) {
  lazy val userType: UserType = storeUser.userRole match {
    case UserRole.ADMIN => SuperUser
    case UserRole.ANONYMOUS => AnonymousBuyer
    case _ => siteUser match {
      case None => Buyer
      case Some(u) => SiteOwner(u)
    }
  }
}

case class ListUserEntry(
  user: StoreUser,
  siteUser: Option[SiteUser],
  site: Option[Site],
  sendNoticeMail: Boolean
)

case class RegisteredEmployeeCount(
  registeredCount: Long,
  allCount: Long
)

case class SupplementalUserEmailId(id: Long) extends AnyVal

case class SupplementalUserEmail(
  id: Option[SupplementalUserEmailId] = None,
  email: String,
  storeUserId: Long
)

object StoreUser {
  val EmployeeUserNamePattern = """(\d+)-(.+)""".r

  val logger = Logger(getClass)
  implicit val tokenGenerator: TokenGenerator = RandomTokenGenerator()

  val simple = {
    SqlParser.get[Option[Long]]("store_user.store_user_id") ~
    SqlParser.get[String]("store_user.user_name") ~
    SqlParser.get[String]("store_user.first_name") ~
    SqlParser.get[Option[String]]("store_user.middle_name") ~
    SqlParser.get[String]("store_user.last_name") ~
    SqlParser.get[String]("store_user.email") ~
    SqlParser.get[Long]("store_user.password_hash") ~
    SqlParser.get[Long]("store_user.salt") ~
    SqlParser.get[Boolean]("store_user.deleted") ~
    SqlParser.get[Int]("store_user.user_role") ~
    SqlParser.get[Option[String]]("store_user.company_name") map {
      case id~userName~firstName~middleName~lastName~email~passwordHash~salt~deleted~userRole~companyName =>
        StoreUser(
          id, userName, firstName, middleName, lastName, email, passwordHash, 
          salt, deleted, UserRole.byIndex(userRole), companyName
        )
    }
  }

  val withSiteUser = 
    StoreUser.simple ~
    (SiteUser.simple ?) ~
    (Site.simple ?) ~
    SqlParser.get[Option[Long]]("order_notification.order_notification_id") map {
      case storeUser~siteUser~site~notificationId => ListUserEntry(storeUser, siteUser, site, notificationId.isDefined)
    }

  def count(implicit conn: Connection) = 
    SQL("select count(*) from store_user where deleted = FALSE").as(SqlParser.scalar[Long].single)

  def apply(id: Long)(implicit conn: Connection): StoreUser =
    SQL(
      "select * from store_user where store_user_id = {id} and deleted = FALSE"
    ).on(
      'id -> id
    ).as(StoreUser.simple.single)
  
  def findByUserName(userName: String)(implicit conn: Connection): Option[StoreUser] =
    SQL(
      "select * from store_user where user_name = {user_name} and deleted = FALSE"
    ).on(
      'user_name -> userName
    ).as(StoreUser.simple.singleOpt)

  def all(implicit conn: Connection): Seq[StoreUser] =
    SQL(
      "select * from store_user where deleted = FALSE"
    ).as(StoreUser.simple *)

  def create(
    userName: String, firstName: String, middleName: Option[String], lastName: String,
    email: String, passwordHash: Long, salt: Long, userRole: UserRole, companyName: Option[String]
  )(implicit conn: Connection): StoreUser = {
    SQL(
      """
      insert into store_user (
        store_user_id, user_name, first_name, middle_name, last_name, email, password_hash, 
        salt, deleted, user_role, company_name
      ) values (
        (select nextval('store_user_seq')),
        {user_name}, {first_name}, {middle_name}, {last_name}, {email}, {password_hash},
        {salt}, FALSE, {user_role}, {company_name}
      )
      """
    ).on(
      'user_name -> userName,
      'first_name -> firstName,
      'middle_name -> middleName,
      'last_name -> lastName,
      'email -> email,
      'password_hash -> passwordHash,
      'salt -> salt,
      'user_role -> userRole.ordinal,
      'company_name -> companyName
    ).executeUpdate()

    val storeUserId = SQL("select currval('store_user_seq')").as(SqlParser.scalar[Long].single)
    StoreUser(Some(storeUserId), userName, firstName, middleName, lastName, email, passwordHash,
              salt, deleted = false, userRole, companyName)
  }

  def withSite(userId: Long)(implicit conn: Connection): ListUserEntry = {
    SQL(
      """
      select * from store_user
      left join site_user on store_user.store_user_id = site_user.store_user_id
      left join site on site_user.site_id = site.site_id
      left join order_notification on order_notification.store_user_id = store_user.store_user_id
      where store_user.store_user_id = {storeUserId}
      and store_user.deleted = FALSE
      """
    ).on(
      'storeUserId -> userId
    ).as(
      withSiteUser.single
    )
  }

  def listUsers(
    page: Int = 0, pageSize: Int = 50, 
    orderBy: OrderBy = OrderBy("store_user.user_name"), employeeSiteId: Option[Long] = None
  )(implicit conn: Connection): PagedRecords[ListUserEntry] = {
    val list = SQL(
      s"""
      select 
        *,
        store_user.first_name || coalesce(store_user.middle_name, '') || store_user.last_name as full_name,
        (store_user.user_role + coalesce(site_user.site_id, 0)) as store_user_role
      from store_user
      left join site_user on store_user.store_user_id = site_user.store_user_id
      left join site on site_user.site_id = site.site_id
      left join order_notification on order_notification.store_user_id = store_user.store_user_id
      where store_user.deleted = FALSE"""
      + (employeeSiteId.map {siteId => " and user_name like '" + siteId + "-%'"}.getOrElse(""))
      + s""" order by $orderBy
      limit {pageSize} offset {offset}
      """
    ).on(
      'pageSize -> pageSize,
      'offset -> page * pageSize
    ).as(
      withSiteUser *
    )

    val count = SQL("select count(*) from store_user where deleted = FALSE").as(SqlParser.scalar[Long].single)

    PagedRecords(page, pageSize, (count + pageSize - 1) / pageSize, orderBy, list)
  }

  def delete(userId: Long)(implicit conn: Connection) {
    SQL(
      """
      update store_user set deleted = TRUE where store_user_id = {id}
      """
    ).on(
      'id -> userId
    ).executeUpdate()
  }

  def update(
    userId: Long,
    userName: String, firstName: String, middleName: Option[String], lastName: String,
    email: String, passwordHash: Long, salt: Long, companyName: Option[String]
  )(implicit conn: Connection): Int =
    SQL(
      """
      update store_user set
        user_name = {userName},
        first_name = {firstName},
        middle_name = {middleName},
        last_name = {lastName},
        email = {email},
        password_hash = {passwordHash},
        salt = {salt},
        company_name = {companyName}
      where store_user_id = {userId}
      """
    ).on(
      'userName -> userName,
      'firstName -> firstName,
      'middleName -> middleName,
      'lastName -> lastName,
      'email -> email,
      'passwordHash -> passwordHash,
      'salt -> salt,
      'companyName -> companyName,
      'userId -> userId
    ).executeUpdate()

  def maintainByCsv(
    z: Iterator[Try[CsvRecord]],
    csvRecordFilter: CsvRecord => Boolean = _ => true,
    deleteSqlSupplemental: Option[String] = None,
    employeeCsvRegistration: Boolean = false
  ): (Int, Int) = {
    DB.withConnection { implicit conn =>
      try {
        SQL(
          """
          create temp table user_csv (
            company_id bigint,
            user_name varchar(64) not null unique,
            salt bigint not null,
            password_hash bigint not null
          )
          """
        ).executeUpdate()

        insertCsvIntoTempTable(z, csvRecordFilter)

        conn.setAutoCommit(false)
        try {
          val insCount = insertByCsv(employeeCsvRegistration)

          val delCount = SQL(
            """
            update store_user
            set deleted = TRUE
            where user_role = """ + UserRole.NORMAL.ordinal +
            """
            and deleted = FALSE
            and user_name not in (
              select user_name from user_csv
            )
            """ + deleteSqlSupplemental.map { "and " + _ }.getOrElse("")
          ).executeUpdate()
          conn.commit()

          (insCount, delCount)
        }
        catch {
          case t: Throwable => {
            conn.rollback()
            throw t
          }
        }
        finally {
          conn.setAutoCommit(true)
        }
      }
      catch {
        case e: SQLException => {
          logSqlException(e)
          throw e
        }
        case t: Throwable => throw t
      }
      finally {
        SQL("drop table user_csv").executeUpdate()
      }
    }
  }

  def logSqlException(e: SQLException) {
    logger.error("DB access error.", e)
    val n = e.getNextException
    if (n != null) {
      logger.error("--- next exception ---")
      logSqlException(n)
    }
  }

  def insertCsvIntoTempTable(
    z: Iterator[Try[CsvRecord]], csvRecordFilter: CsvRecord => Boolean
  )(implicit conn: Connection) {
    var userNames = mutable.HashSet[String]()

    val recs: Iterator[Seq[ParameterValue]] = z.collect {
      case Failure(e) => throw e
      case Success(cols) if csvRecordFilter(cols) =>
        val companyIdStr = cols('CompanyId)
        val userName = cols('EmployeeNo)
        validateNormalUserName(userName)
        if (! userNames.add(userName)) {
          throw new DuplicatedUserNameException(userName)
        }
        val compoundUserName = (if (companyIdStr.isEmpty) "" else companyIdStr + "-") + userName
        val companyId = if (companyIdStr.isEmpty) None else Some(companyIdStr.toLong)
        val password = cols('Password)
        val salt = tokenGenerator.next
        val hash = PasswordHash.generate(password, salt)
        Seq(companyId, compoundUserName, salt, hash)
    }

    if (recs.hasNext) {
      val firstRec: Seq[NamedParameter] = Seq(
        "companyId", "userName", "salt", "passwordHash"
      ).zip(recs.next()).map { t => NamedParameter(t._1, t._2)}

      val sql: BatchSql = BatchSql(
        """
        insert into user_csv (
          company_id, user_name, salt, password_hash
        ) values (
          {companyId}, {userName}, {salt}, {passwordHash}
        )
        """, firstRec
      )

      recs.foldLeft(sql) { (sql, e) => sql.addBatchParams(e: _*) }.execute()
    }
  }

  def insertByCsv(employeeCsvRegistration: Boolean)(implicit conn: Connection): Int = {
    val csvRecs: Seq[(String, Long, Long, Option[String])] = SQL(
      """
      select s.site_name, user_csv.user_name, user_csv.salt, user_csv.password_hash from user_csv
      left join store_user
      on user_csv.user_name = store_user.user_name
      left join site s
      on user_csv.company_id = s.site_id
      where store_user.user_name is null
      """
    ).as(
      SqlParser.get[Option[String]]("site_name") ~
      SqlParser.str("user_name") ~
      SqlParser.long("password_hash") ~
      SqlParser.long("salt") map(SqlParser.flatten) *
    ).map { rec =>
      val companyName: Option[String] =
        if (employeeCsvRegistration) {
          rec._1
        }
        else None
      (rec._2, rec._3, rec._4, companyName)
    }

    if (csvRecs.isEmpty) {
      0
    }
    else {
      val firstRec = csvRecs.head
      val insUserSql: BatchSql = BatchSql(
        """
        insert into store_user (
          store_user_id, user_name, first_name, middle_name, last_name,
          email, password_hash, salt, deleted, user_role, company_name
        ) values (
          (select nextval('store_user_seq')), {userName}, '', null, '',
          '', {passwordHash}, {salt}, FALSE, """ +
        UserRole.NORMAL.ordinal +
        """
          , {companyName}
        )
        """,
        Seq(
          NamedParameter("userName", firstRec._1),
          NamedParameter("passwordHash", firstRec._2),
          NamedParameter("salt", firstRec._3),
          NamedParameter("companyName", firstRec._4)
        )
      )

      val cnt = csvRecs.tail.foldLeft(insUserSql) { (sql, t) =>
        sql.addBatchParams(t._1, t._2, t._3, t._4)
      }.execute().length
      insertEmployeeByCsv

      cnt
    }
  }

  def insertEmployeeByCsv(implicit conn: Connection) {
    val csvRecs: Seq[(Long, Long)] = SQL(
      """
      select s.site_id, store_user.store_user_id from user_csv
      inner join store_user
      on user_csv.user_name = store_user.user_name
      inner join site s
      on user_csv.company_id = s.site_id
      left join employee e
      on e.store_user_id = store_user.store_user_id
      where store_user.deleted = false and e.employee_id is null
      """
    ).as(
      SqlParser.long("site_id") ~
      SqlParser.long("store_user_id") map(SqlParser.flatten) *
    )

    if (! csvRecs.isEmpty) {
      val firstRec = csvRecs.head

      val insEmployeeSql: BatchSql = BatchSql(
        """
        insert into employee (
          employee_id, site_id, store_user_id
        ) values (
          (select nextval('employee_seq')), {siteId}, {userId}
        )
        """,
        Seq(
          NamedParameter("siteId", firstRec._1),
          NamedParameter("userId", firstRec._2)
        )
      )

      csvRecs.tail.foldLeft(insEmployeeSql) { (sql, t) =>
        sql.addBatchParams(t._1, t._2)
      }.execute()
    }
  }

  def changePassword(userId: Long, passwordHash: Long, salt: Long)(implicit conn: Connection): Int = SQL(
    """
    update store_user set
      password_hash = {passwordHash},
      salt = {salt}
    where store_user_id = {userId}
    """
  ).on(
    'passwordHash -> passwordHash,
    'salt -> salt,
    'userId -> userId
  ).executeUpdate()

  def validateNormalUserName(userName: String) {
    val errors: Seq[ValidationError] = FormConstraints.normalUserNameConstraint().map(_(userName)).collect {
      case Invalid(errors: Seq[ValidationError]) => errors
    }.flatten

    if (! errors.isEmpty)
      throw new InvalidUserNameException(userName, errors)
  }

  def registeredEmployeeCount(implicit conn: Connection): immutable.SortedMap[Long, RegisteredEmployeeCount] = {
    class Sum(var registered: Long = 0, var all: Long = 0) {
      def incRegistered() {
        registered += 1
      }

      def incAll() {
        all += 1
      }
    }

    val sum: mutable.Map[Long, Sum] = mutable.LongMap[Sum]()

    SQL(
      """
      select user_name, first_name from store_user u
      where u.deleted=false and u.user_role=
      """
      + UserRole.NORMAL.ordinal +
      """
      and not exists (select * from site_user su where su.store_user_id = u.store_user_id)
      """
    ).as(
      SqlParser.str("user_name") ~ SqlParser.str("first_name") map(SqlParser.flatten) *
    ).foreach { t =>
      t._1 match {
        case EmployeeUserNamePattern(siteIdStr, employeeCode) =>
          val siteId = siteIdStr.toLong
          val sumBySiteId = sum.get(siteId) match {
            case Some(rec) => rec
            case None =>
              val rec = new Sum()
              sum += (siteId -> rec)
              rec
          }
          sumBySiteId.incAll()
          if (! t._2.isEmpty) sumBySiteId.incRegistered()
        case _ =>
      }
    }

    sum.foldLeft(immutable.TreeMap[Long, RegisteredEmployeeCount]()) { (sum, e) =>
      sum + (e._1 -> RegisteredEmployeeCount(e._2.registered, e._2.all))
    }
  }

  def removeObsoleteAnonymousUser(
    durationToPreserveAnonymousUser: FiniteDuration
  )(implicit conn: Connection): Int = SQL(
    """
    delete from store_user
    where user_role = {userRole}
    and created_time < {createdTime}
    """
  ).on(
    'userRole -> UserRole.ANONYMOUS.ordinal,
    'createdTime -> java.time.Instant.ofEpochMilli(
      System.currentTimeMillis - durationToPreserveAnonymousUser.toMillis
    )
  ).executeUpdate()
}

object SiteUser {
  val simple = {
    SqlParser.get[Option[Long]]("site_user.site_user_id") ~
    SqlParser.get[Long]("site_user.site_id") ~
    SqlParser.get[Long]("site_user.store_user_id") map {
      case id~siteId~storeUserId => SiteUser(id, siteId, storeUserId)
    }
  }

  def createNew(storeUserId: Long, siteId: Long)(implicit conn: Connection): SiteUser = {
    SQL(
      """
      insert into site_user (
        site_user_id, site_id, store_user_id
      ) values (
        (select nextval('site_user_seq')),
        {siteId}, {storeUserId}
      )
      """
    ).on(
      'siteId -> siteId,
      'storeUserId -> storeUserId
    ).executeUpdate()

    val id = SQL("select currval('site_user_seq')").as(SqlParser.scalar[Long].single)
    SiteUser(Some(id), siteId, storeUserId)
  }

  def getByStoreUserId(storeUserId: Long)(implicit conn: Connection): Option[SiteUser] =
    SQL(
      """
      select * from site_user where store_user_id = {storeUserId}
      """
    ).on(
      'storeUserId -> storeUserId
    ).as(
      SiteUser.simple.singleOpt
    )
}

object SupplementalUserEmail {
  val simple = {
    SqlParser.get[Option[Long]]("supplemental_user_email.supplemental_user_email_id") ~
    SqlParser.get[String]("supplemental_user_email.email") ~
    SqlParser.get[Long]("supplemental_user_email.store_user_id") map {
      case id~email~storeUserId => SupplementalUserEmail(
        id.map{SupplementalUserEmailId.apply},
        email, storeUserId
      )
    }
  }

  def save(emailTable: Set[String], storeUserId: Long)(implicit conn: Connection) {
    SQL(
      "delete from supplemental_user_email where store_user_id = {id}"
    ).on(
      'id -> storeUserId
    ).executeUpdate()

    if (! emailTable.isEmpty) {
      val sql = BatchSql(
        """
        insert into supplemental_user_email (
          supplemental_user_email_id,
          email,
          store_user_id
        ) values (
          (select nextval('supplemental_user_email_seq')),
          {email}, {storeUserId}
        )
        """,
        Seq[NamedParameter](
          'email -> emailTable.head, 'storeUserId -> storeUserId
        )
      )

      emailTable.tail.foldLeft(sql) { (sql, e) =>
        sql.addBatchParams(e, storeUserId)
      }.execute()
    }
  }

  def load(storeUserId: Long)(implicit conn: Connection): immutable.Set[SupplementalUserEmail] = SQL(
    """
    select * from supplemental_user_email
    where store_user_id = {storeUserId}
    """
  ).on(
    'storeUserId -> storeUserId
  ).as(
    simple *
  ).toSet
}
