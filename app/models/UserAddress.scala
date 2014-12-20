package models

import anorm._
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import helpers.PasswordHash
import java.sql.Connection

case class UserAddress(
  id: Option[Long] = None,
  storeUserId: Long,
  addressId: Long,
  seq: Int
)

object UserAddress {
  val simple = {
    SqlParser.get[Option[Long]]("user_address.user_address_id") ~
    SqlParser.get[Long]("user_address.store_user_id") ~
    SqlParser.get[Long]("user_address.address_id") ~
    SqlParser.get[Int]("user_address.seq") map {
      case id~storeUserId~addressId~seq =>
        UserAddress(id, storeUserId, addressId, seq)
    }
  }

  def apply(id: Long)(implicit conn: Connection): UserAddress = SQL(
    """
    select * from user_address where user_address_id = {id}
    """
  ).on(
    'id -> id
  ).as(simple.single)

  def getByUserId(storeUserId: Long)(implicit conn: Connection): Option[UserAddress] = SQL(
    """
    select * from user_address where store_user_id = {id} and seq = 1
    """
  ).on(
    'id -> storeUserId
  ).as(simple.singleOpt)

  def createNew(storeUserId: Long, addressId: Long)(implicit conn: Connection): UserAddress = {
    SQL(
      """
      insert into user_address (
        user_address_id, store_user_id, address_id, seq
      ) values (
        (select nextval('user_address_seq')),
        {storeUserId}, {addressId},
        (select coalesce(max(seq) + 1, 1) from user_address where store_user_id = {storeUserId})
      )
      """
    ).on(
      'storeUserId -> storeUserId,
      'addressId -> addressId
    )executeUpdate()
    
    val id = SQL("select currval('user_address_seq')").as(SqlParser.scalar[Long].single)
    UserAddress(id)
  }
}
