package models

import anorm._
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import helpers.PasswordHash
import java.sql.Connection

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
) extends NotNull {
  def passwordMatch(password: String): Boolean =
    PasswordHash.generate(password, salt) == passwordHash
}

case class SiteUser(id: Option[Long] = None, siteId: Long, storeUserId: Long) extends NotNull

case class User(storeUser: StoreUser, siteUser: Option[SiteUser]) extends NotNull {
  lazy val userType: UserType = storeUser.userRole match {
    case UserRole.ADMIN => SuperUser
    case UserRole.NORMAL => siteUser match {
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
) extends NotNull

object StoreUser {
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
    page: Int = 0, pageSize: Int = 50, orderBy: OrderBy = OrderBy("store_user.user_name")
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
      where store_user.deleted = FALSE
      order by $orderBy
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
    email: String, passwordHash: Long, salt: Long, companyName: String
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
