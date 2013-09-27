package models

import anorm._
import anorm.{NotAssigned, Pk}
import anorm.SqlParser
import model.Until
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import collection.immutable.{HashMap, IntMap}
import java.sql.Connection
import play.api.data.Form
import org.joda.time.DateTime

case class ShippingBox(
  id: Pk[Long] = NotAssigned, siteId: Long, itemClass: Long, boxSize: Int, boxName: String
) extends NotNull

case class ShippingFee(
  id: Pk[Long] = NotAssigned, shippingBoxId: Long, countryCode: CountryCode, locationCode: Int
) extends NotNull

case class ShippingFeeHistory(
  id: Pk[Long] = NotAssigned, shippingFeeId: Long, fee: BigDecimal, validUntil: Long
) extends NotNull

object ShippingBox {
  val simple = {
    SqlParser.get[Pk[Long]]("shipping_box.shipping_box_id") ~
    SqlParser.get[Long]("shipping_box.site_id") ~
    SqlParser.get[Long]("shipping_box.item_class") ~
    SqlParser.get[Int]("shipping_box.box_size") ~
    SqlParser.get[String]("shipping_box.box_name") map {
      case id~siteId~itemClass~boxSize~boxName =>
        ShippingBox(id, siteId, itemClass, boxSize, boxName)
    }
  }

  def createNew(
    siteId: Long, itemClass: Long, boxSize: Int, boxName: String
  )(implicit conn: Connection): ShippingBox = {
    SQL(
      """
      insert into shipping_box
      (shipping_box_id, site_id, item_class, box_size, box_name)
      values
      ((select nextval('shipping_box_seq')),
      {siteId}, {itemClass}, {boxSize}, {boxName})
      """
    ).on(
      'siteId -> siteId,
      'itemClass -> itemClass,
      'boxSize -> boxSize,
      'boxName -> boxName
    ).executeUpdate()
    
    val id = SQL("select currval('shipping_box_seq')").as(SqlParser.scalar[Long].single)
    ShippingBox(Id(id), siteId, itemClass, boxSize, boxName)
  }

  def apply(siteId: Long, itemClass: Long)(implicit conn: Connection): ShippingBox =
    SQL(
      """
      select * from shipping_box where siteId = {siteId} and itemClass = {itemClass}
      """
    ).on(
      'siteId -> siteId,
      'itemClass -> itemClass
    ).as(
      simple.single
    )

  def list(siteId: Long)(implicit conn: Connection): Seq[ShippingBox] =
    SQL(
      """
      select * fromo shipping_box where siteId = {siteId} order by itemClass
      """
    ).on(
      'siteId -> siteId
    ).as(
      simple *
    )
}

object ShippingFee {
  val simple = {
    SqlParser.get[Pk[Long]]("shipping_fee.shipping_fee_id") ~
    SqlParser.get[Long]("shipping_fee.shipping_box_id") ~
    SqlParser.get[Int]("shipping_fee.country_code") ~
    SqlParser.get[Int]("shipping_fee.location_code") map {
      case id~shippingBoxId~countryCode~locationCode =>
        ShippingFee(id, shippingBoxId, CountryCode.byIndex(countryCode), locationCode)
    }
  }

  def createNew(
    shippingBoxId: Long, countryCode: CountryCode, locationCode: Int
  )(implicit conn: Connection): ShippingFee = {
    SQL(
      """
      insert into shipping_fee
      (shipping_fee_id, shipping_box_id, country_code, location_code)
      values (
        (select nextval('shipping_fee_seq')),
        {shippingBoxId}, {countryCode}, {locationCode}
      )
      """
    ).on(
      'shippingBoxId -> shippingBoxId,
      'countryCode -> countryCode.code,
      'locationCode -> locationCode
    ).executeUpdate()

    val id = SQL("select currval('shipping_fee_seq')").as(SqlParser.scalar[Long].single)

    ShippingFee(Id(id), shippingBoxId, countryCode, locationCode)
  }

  def apply(
    shippingBoxId: Long, countryCode: CountryCode, locationCode: Int
  )(implicit conn: Connection): ShippingFee =
    SQL(
      """
      select * from shipping_fee
      where shipping_box_id = {shippingBoxId}
      and country_code = {countryCode}
      and location_code = {locationCode}
      """
    ).on(
      'shippingBoxId -> shippingBoxId,
      'countryCode -> countryCode.code,
      'locationCode -> locationCode
    ).as(
      simple.single
    )
}

object ShippingFeeHistory {
  val simple = {
    SqlParser.get[Pk[Long]]("shipping_fee_history.shipping_fee_history_id") ~
    SqlParser.get[Long]("shipping_fee_history.shipping_fee_id") ~
    SqlParser.get[java.math.BigDecimal]("shipping_fee_history.fee") ~
    SqlParser.get[java.util.Date]("shipping_fee_history.valid_until") map {
      case id~feeId~fee~validUntil => ShippingFeeHistory(id, feeId, fee, validUntil.getTime)
    }
  }

  def createNew(
    feeId: Long, fee: BigDecimal, validUntil: Long
  )(implicit conn: Connection): ShippingFeeHistory = {
    SQL(
      """
      insert into shipping_fee_history (shipping_fee_history_id, shipping_fee_id, fee, valid_until)
      values (
        (select nextval('shipping_fee_history_seq')), {feeId}, {fee}, {validUntil}
      )
      """
    ).on(
      'feeId -> feeId,
      'fee -> fee.bigDecimal,
      'validUntil -> new java.sql.Timestamp(validUntil)
    ).executeUpdate()

    val id = SQL("select currval('shipping_fee_history_seq')").as(SqlParser.scalar[Long].single)

    ShippingFeeHistory(Id(id), feeId, fee, validUntil)
  }

  def list(feeId: Long)(implicit conn: Connection): Seq[ShippingFeeHistory] = SQL(
    "select * from shipping_fee_history where shipping_fee_id = {shippingFeeId} order by valid_until"
  ).on(
    'shippingFeeId -> feeId
  ).as(
    simple *
  )

  def at(
    feeId: Long, now: Long = System.currentTimeMillis
  )(implicit conn: Connection): ShippingFeeHistory = SQL(
    """
    select * from shipping_fee_history
    where shipping_fee_id = {feeId}
    and {now} < valid_until
    order by valid_until
    limit 1
    """
  ).on(
    'feeId -> feeId,
    'now -> new java.sql.Timestamp(now)
  ).as(
    simple.single
  )

  val withFee = ShippingFee.simple ~ simple map {
    case shippingFee~shippingFeeHistory => (shippingFee, shippingFeeHistory)
  }

  def listByLocation(
    countryCode: CountryCode, locationCode: Int
  )(implicit conn: Connection): Seq[(ShippingFee, ShippingFeeHistory)] =
    SQL(
      """
      select * from shipping_fee_history
      inner join shipping_fee on shipping_fee_history.shipping_fee_id = shipping_fee.shipping_fee_id
      where country_code = {countryCode} and location_code = {locationCode}
      order by shipping_fee_history.valid_until
      """
    ).on(
      'countryCode -> countryCode.code,
      'locationCode -> locationCode
    ).as(
      withFee *
    ).toSeq
}
