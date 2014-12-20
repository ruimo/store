package models

import anorm._
import anorm.SqlParser
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import play.api.i18n.Lang
import java.sql.Connection

case class CouponId(id: Long) extends AnyVal

case class Coupon(id: Option[CouponId] = None, deleted: Boolean)

case class CouponItemId(id: Long) extends AnyVal

case class CouponItem(
  id: Option[CouponItemId] = None,
  itemId: ItemId,
  couponId: CouponId
)

object Coupon {
  val simple = {
    SqlParser.get[Option[Long]]("coupon.coupon_id") ~
    SqlParser.get[Boolean]("coupon.deleted") map {
      case id~deleted => Coupon(id.map(CouponId.apply), deleted)
    }
  }

  def createNew()(implicit conn: Connection): Coupon = {
    SQL(
      """
      insert into coupon (coupon_id, deleted)
      values ((select nextval('coupon_seq')), false)
      """
    ).executeUpdate()
    
    Coupon(
      Some(CouponId(SQL("select currval('coupon_seq')").as(SqlParser.scalar[Long].single))),
      false
    )
  }

  def apply(id: CouponId)(implicit conn: Connection): Coupon = SQL(
    """
    select * from coupon where coupon_id = {id}
    """
  ).on(
    'id -> id.id
  ).as(simple.single)

  def getByItem(itemId: ItemId)(implicit conn: Connection): Option[Coupon] = SQL(
    """
    select * from coupon c
    inner join coupon_item ci on c.coupon_id = ci.coupon_id
    where ci.item_id = {itemId}
    """
  ).on(
    'itemId -> itemId.id
  ).as(simple.singleOpt)

  def update(itemId: ItemId, isCoupon: Boolean)(implicit conn: Connection): Unit =
    if (isCoupon) updateAsCoupon(itemId) else updateAsNonCoupon(itemId)

  def updateAsCoupon(itemId: ItemId)(implicit conn: Connection): Unit = {
    val count: Int = SQL(
      """
      update coupon set deleted = false
      where coupon_id = (
        select coupon_id from coupon_item where item_id = {id}
      )
      """
    ).on(
      'id -> itemId.id
    ).executeUpdate()

    if (count == 0) {
      val coupon = createNew()
      try {
        ExceptionMapper.mapException {
          CouponItem.create(itemId, coupon.id.get)
        }
      }
      catch {
        case e: UniqueConstraintException =>
        case e: Throwable => throw e
      }
    }
  }

  def updateAsNonCoupon(itemId: ItemId)(implicit conn: Connection): Unit = SQL(
    """
    update coupon set deleted = true
    where coupon_id = (
      select coupon_id from coupon_item where item_id = {id}
    )
    """
  ).on(
    'id -> itemId.id
  ).executeUpdate()


  def isCoupon(itemId: ItemId)(implicit conn: Connection): Boolean = SQL(
    """
    select coupon.deleted = false
    from coupon
    inner join coupon_item on coupon.coupon_id = coupon_item.coupon_id
    where coupon_item.item_id = {itemId}
    """
  ).on(
    'itemId -> itemId.id
  ).as(
    SqlParser.scalar[Boolean].singleOpt
  ).getOrElse(
    false
  )
}

object CouponItem {
  val simple = {
    SqlParser.get[Option[Long]]("coupon_item.coupon_item_id") ~
    SqlParser.get[Long]("coupon_item.item_id") ~
    SqlParser.get[Long]("coupon_item.coupon_id") map {
      case id~itemId~couponId => CouponItem(
        id.map(CouponItemId.apply), ItemId(itemId), CouponId(couponId)
      )
    }
  }

  def create(
    itemId: ItemId, couponId: CouponId
  )(
    implicit conn: Connection
  ): CouponItem = {
    SQL(
      """
      insert into coupon_item (coupon_item_id, item_id, coupon_id)
      values ((select nextval('coupon_item_seq')), {itemId}, {couponId})
      """
    ).on(
      'itemId -> itemId.id,
      'couponId -> couponId.id
    ).executeUpdate()

    CouponItem(
      Some(CouponItemId(SQL("select currval('coupon_item_seq')").as(SqlParser.scalar[Long].single))),
      itemId,
      couponId
    )
  }
}
