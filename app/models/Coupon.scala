package models

import anorm._
import anorm.SqlParser
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import play.api.i18n.Lang
import java.sql.Connection

case class CouponId(id: Long) extends AnyVal

case class Coupon(id: Option[CouponId] = None)

object Coupon {
  val simple = {
    SqlParser.get[Option[Long]]("coupon.coupon_id") map {
      case id => Coupon(id.map { id => CouponId(id) })
    }
  }

  def createNew()(implicit conn: Connection): Coupon = {
    SQL(
      """
      insert into coupon (coupon_id) values ((select nextval('coupon_seq')))
      """
    ).executeUpdate()
    
    Coupon(CouponId(SQL("select currval('coupon_seq')").as(SqlParser.scalar[Long].single)))
  }

  def apply(id: CouponId)(implicit conn: Connection): Coupon = SQL(
    """
    select * from coupon where coupon_id = {id}
    """
  ).on(
    'id -> id.id
  ).as(simple.single)
}
