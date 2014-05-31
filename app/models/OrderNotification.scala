package models

import anorm._
import anorm.{NotAssigned, Pk}
import anorm.SqlParser

import scala.language.postfixOps
import play.api.Play.current
import java.sql.Connection

case class OrderNotification(
  id: Pk[Long] = NotAssigned,
  storeUserId: Long
) extends NotNull

object OrderNotification {
  val simple = {
    SqlParser.get[Pk[Long]]("order_notification.order_notification_id") ~
    SqlParser.get[Long]("order_notification.store_user_id") map {
      case id~storeUserId => OrderNotification(id, storeUserId)
    }
  }

  def getByUserId(storeUserId: Long)(implicit conn: Connection): Option[OrderNotification] =
    SQL(
      """
      select * from order_notification where store_user_id = {id}
      """
    ).on(
      'id -> storeUserId
    ).as(
      simple.singleOpt
    )

  def createNew(storeUserId: Long)(implicit conn: Connection): OrderNotification = {
    SQL(
      """
      insert into order_notification values (
        (select nextval('order_notification_seq')), {storeUserId}
      )
      """
    ).on(
      'storeUserId -> storeUserId
    ).executeUpdate()

    val id = SQL("select currval('order_notification_seq')").as(SqlParser.scalar[Long].single)

    OrderNotification(Id(id), storeUserId)
  }

  def delete(storeUserId: Long)(implicit conn: Connection): Int =
    SQL(
      """
      delete from order_notification where store_user_id = {id}
      """
    ).on(
      'id -> storeUserId
    ).executeUpdate()

  def list(page: Int = 0, pageSize: Int = 50)(implicit conn: Connection): Seq[OrderNotification] =
    SQL(
      """
      select * from order_notification
      order by order_notification_id
      limit {pageSize} offset {offset}
      """
    ).on(
      'pageSize -> pageSize,
      'offset -> page * pageSize
    ).as(
      simple *
    )

  def listBySite(siteId: Long)(implicit conn: Connection): Seq[StoreUser] =
    SQL(
      """
      select * from store_user
      inner join site_user on store_user.store_user_id = site_user.store_user_id
      inner join order_notification on order_notification.store_user_id = store_user.store_user_id
      where site_user.site_id = {siteId}
      and deleted = FALSE
      """
    ).on(
      'siteId -> siteId

    
    ).as(
      StoreUser.simple *
    )

  def listAdmin(implicit conn: Connection): Seq[StoreUser] =
    SQL(
      """
      select * from store_user su
      inner join order_notification on order_notification.store_user_id = su.store_user_id
      where user_role = {userRole}
      and not exists(select 'X' from site_user where store_user_id = su.store_user_id)
      and deleted = FALSE
      """
    ).on(
      'userRole -> UserRole.ADMIN.ordinal
    ).as(
      StoreUser.simple *
    )
}
