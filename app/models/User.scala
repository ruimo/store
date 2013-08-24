package models

import anorm._
import anorm.{NotAssigned, Pk}
import anorm.SqlParser
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps

case class StoreUser(
  id: Pk[Long] = NotAssigned,
  userName: String,
  firstName: String,
  lastName: String,
  email: String,
  paswordHash: Long,
  salt: Long,
  deleted: Boolean,
  userRole: UserRole
)

case class SiteUser(siteId: Long, storeUserId: Long, userRole: UserRole)

object StoreUser {
  val simple = {
    SqlParser.get[Pk[Long]]("store_user.store_user_id") ~
    SqlParser.get[String]("store_user.user_name") ~
    SqlParser.get[String]("store_user.first_name") ~
    SqlParser.get[String]("store_user.last_name") ~
    SqlParser.get[String]("store_user.email") ~
    SqlParser.get[Long]("store_user.password_hash") ~
    SqlParser.get[Long]("store_user.salt") ~
    SqlParser.get[Boolean]("store_user.deleted") ~
    SqlParser.get[Short]("store_user.user_role") map {
      case id~userName~firstName~lastName~email~passwordHash~salt~deleted~userRole => StoreUser(
        id, userName, firstName, lastName, email, passwordHash, salt, deleted, UserRole.byIndex(userRole)
      )
    }
  }

  def count = DB.withConnection { implicit conn =>
    SQL("select count(*) from store_user").as(SqlParser.scalar[Long].single)
  }

  def find(id: Long): StoreUser = DB.withConnection { implicit conn => {
    SQL(
      "select * from store_user where store_user_id = {id}"
    ).on(
      'id -> id
    ).as(StoreUser.simple.single)
  }}

  def create(
    userName: String, firstName: String, lastName: String,
    email: String, passwordHash: Long, salt: Long, userRole: UserRole
  ): StoreUser = DB.withConnection { implicit conn => {
    SQL(
      """
      insert into store_user (
        store_user_id, user_name, first_name, last_name, email, password_hash, salt, deleted, user_role
      ) values (
        (select next value for store_user_seq),
        {user_name}, {first_name}, {last_name}, {email}, {password_hash}, {salt}, false, {user_role}
      )
      """
    ).on(
      'user_name -> userName,
      'first_name -> firstName,
      'last_name -> lastName,
      'email -> email,
      'password_hash -> passwordHash,
      'salt -> salt,
      'user_role -> userRole.ordinal
    ).executeUpdate()

    val storeUserId = SQL("select currval('store_user_seq')").as(SqlParser.scalar[Long].single)
    StoreUser(Id(storeUserId), userName, firstName, lastName, email, passwordHash, salt,  false, userRole)
  }}
}

object SiteUser {
  val simple = {
    SqlParser.get[Long]("site_user.site_id") ~
    SqlParser.get[Long]("site_user.store_user_id") ~
    SqlParser.get[Int]("site_user.user_role") map {
      case siteId~storeUserId~userRole => SiteUser(siteId, storeUserId, UserRole.byIndex(userRole))
    }
  }
}
