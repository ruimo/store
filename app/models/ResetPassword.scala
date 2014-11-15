package models

import anorm._
import anorm.SqlParser
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import java.sql.Connection
import helpers.RandomTokenGenerator

case class ResetPasswordId(id: Long) extends AnyVal

case class ResetPassword(
  id: Option[ResetPasswordId] = None,
  storeUserId: Long,
  token: Long,
  resetTime: Long
)

object ResetPassword {
  val simple = {
    SqlParser.get[Option[Long]]("reset_password.reset_password_id") ~
    SqlParser.get[Long]("reset_password.store_user_id") ~
    SqlParser.get[Long]("reset_password.token") ~
    SqlParser.get[java.util.Date]("reset_password.reset_time") map {
      case id~storeUserId~token~resetTime =>
        ResetPassword(id.map {ResetPasswordId.apply}, storeUserId, token, resetTime.getTime)
    }
  }

  def createNew(storeUserId: Long)(implicit conn: Connection): ResetPassword = createNew(
    storeUserId,
    System.currentTimeMillis,
    createToken()
  )

  def createNew(storeUserId: Long, now: Long, token: Long)(implicit conn: Connection): ResetPassword = {
    SQL(
      """
      insert into reset_password (reset_password_id, store_user_id, token, reset_time)
      values (
        (select nextval('reset_password_seq')),
        {storeUserId}, {token}, {resetTime}
      )
      """
    ).on(
      'storeUserId -> storeUserId,
      'token ->token,
      'resetTime -> new java.sql.Timestamp(now)
    ).executeUpdate()

    val id = ResetPasswordId(
      SQL("select currval('reset_password_seq')").as(SqlParser.scalar[Long].single)
    )

    ResetPassword(Some(id), storeUserId, token, now)
  }

  def createToken(): Long = RandomTokenGenerator().next

  def apply(id: ResetPasswordId)(implicit conn: Connection): ResetPassword =
    SQL(
      """
      select * from reset_password where reset_password_id = {id}
      """
    ).on(
      'id -> id.id
    ).as(simple.single)

  def get(id: ResetPasswordId)(implicit conn: Connection): Option[ResetPassword] =
    SQL(
      """
      select * from reset_password where reset_password_id = {id}
      """
    ).on(
      'id -> id.id
    ).as(simple.singleOpt)

  def removeByStoreUserId(storeUserId: Long)(implicit conn: Connection): Long = SQL(
    """
    delete from reset_password where store_user_id = {id}
    """
  ).on(
    'id -> storeUserId
  ).executeUpdate()

  def isValid(storeUserId: Long, token: Long, timeout: Long)(implicit conn: Connection): Boolean = SQL(
    """
    select count(*) from reset_password
    where store_user_id = {storeUserId}
    and token = {token}
    and reset_time > {resetTime}
    """
  ).on(
    'storeUserId -> storeUserId,
    'token -> token,
    'resetTime -> new java.sql.Timestamp(timeout)
  ).as(SqlParser.scalar[Long].single) != 0
}
