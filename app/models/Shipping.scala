package models

import anorm._
import anorm.{NotAssigned, Pk}
import anorm.SqlParser
import scala.language.postfixOps
import collection.immutable.{TreeMap, LongMap, HashMap, IntMap}
import java.sql.Connection
import collection.mutable

case class ShippingTotalEntry(
  shippingBox: ShippingBox,
  shippingFee: ShippingFee,
  itemQuantity: Int,
  boxQuantity: Int,
  boxUnitPrice: BigDecimal,
  boxTaxInfo: TaxHistory
) extends NotNull {
  lazy val boxTotal = boxUnitPrice * boxQuantity
  lazy val outerTax = boxTaxInfo.outerTax(boxTotal)
}

case class ShippingTotal(
  // Site, ItemClass, ShippingTotalEntry
  table: Map[Site, Map[Long, ShippingTotalEntry]],
  boxQuantity: Int,
  boxTotal: BigDecimal
) extends NotNull {
  lazy val sumByTaxId: Map[Long, BigDecimal] = {
    var sumById = LongMap[BigDecimal]().withDefaultValue(BigDecimal(0))
    table.values.foreach { map => map.values.foreach { e => 
      val taxId = e.boxTaxInfo.taxId
      sumById += taxId -> (sumById(taxId) + e.boxTotal)
    }}
    sumById
  }

  lazy val taxByType: Map[TaxType, BigDecimal] = {
    var hisById = LongMap[TaxHistory]()
    table.values.foreach { map => map.values.foreach { e =>
      val taxId = e.boxTaxInfo.taxId
      if (! hisById.contains(taxId)) {
        hisById += taxId -> e.boxTaxInfo
      }
    }}

    sumByTaxId.foldLeft(Map[TaxType, BigDecimal]().withDefaultValue(BigDecimal(0))) {
      (sum, e) => {
        val his = hisById(e._1)
        sum.updated(his.taxType, sum(his.taxType) + his.taxAmount(e._2))
      }
    }
  }

  lazy val taxHistoryById: LongMap[TaxHistory] = table.values.foldLeft(LongMap[TaxHistory]()) {
    (sum, e) => e.values.foldLeft(sum) {
      (sum2, e2) =>
        val taxHistory = e2.boxTaxInfo
        sum2.updated(taxHistory.taxId, taxHistory)
    }
  }
}

case class ShippingBox(
  id: Pk[Long] = NotAssigned, siteId: Long, itemClass: Long, boxSize: Int, boxName: String
) extends NotNull

case class ShippingFee(
  id: Pk[Long] = NotAssigned, shippingBoxId: Long, countryCode: CountryCode, locationCode: Int
) extends NotNull

case class ShippingFeeHistory(
  id: Pk[Long] = NotAssigned, shippingFeeId: Long, taxId: Long, fee: BigDecimal, validUntil: Long
) extends NotNull

case class ShippingFeeEntries(
  // siteId, itemClass, quantity
  bySiteAndItemClass: Map[Long, Map[Long, Int]] = new TreeMap[Long, Map[Long, Int]]
) {
  def add(siteId: Long, itemClass: Long, quantity: Int): ShippingFeeEntries = ShippingFeeEntries(
    bySiteAndItemClass.get(siteId) match {
      case None =>
        bySiteAndItemClass + (siteId -> (LongMap(itemClass -> quantity).withDefaultValue(0)))
      case Some(longMap) =>
        bySiteAndItemClass + (siteId -> longMap.updated(itemClass, longMap(itemClass) + quantity))
    }
  )
}

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
    SqlParser.get[Long]("shipping_fee_history.tax_id") ~
    SqlParser.get[java.math.BigDecimal]("shipping_fee_history.fee") ~
    SqlParser.get[java.util.Date]("shipping_fee_history.valid_until") map {
      case id~feeId~taxId~fee~validUntil => ShippingFeeHistory(id, feeId, taxId, fee, validUntil.getTime)
    }
  }

  def createNew(
    feeId: Long, taxId: Long, fee: BigDecimal, validUntil: Long
  )(implicit conn: Connection): ShippingFeeHistory = {
    SQL(
      """
      insert into shipping_fee_history (shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
      values (
        (select nextval('shipping_fee_history_seq')), {feeId}, {taxId}, {fee}, {validUntil}
      )
      """
    ).on(
      'feeId -> feeId,
      'taxId -> taxId,
      'fee -> fee.bigDecimal,
      'validUntil -> new java.sql.Timestamp(validUntil)
    ).executeUpdate()

    val id = SQL("select currval('shipping_fee_history_seq')").as(SqlParser.scalar[Long].single)

    ShippingFeeHistory(Id(id), feeId, taxId, fee, validUntil)
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

  def atOpt(
    feeId: Long, now: Long = System.currentTimeMillis
  )(implicit conn: Connection): Option[ShippingFeeHistory] = SQL(
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
    simple.singleOpt
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

  val withBoxFee = ShippingBox.simple ~ ShippingFee.simple map {
    case box~fee => (box, fee)
  }

  // siteId -> (ShippingBox, ShippingFee, quantity, boxCount, boxFee)
  def feeBySiteAndItemClass(
    countryCode: CountryCode, locationCode: Int, entries: ShippingFeeEntries,
    now: Long = System.currentTimeMillis
  )(
    implicit conn: Connection
  ): ShippingTotal = {
    var map = new HashMap[Site, Map[Long, ShippingTotalEntry]]
    var totalQuantity = 0
    var total = BigDecimal(0)

    entries.bySiteAndItemClass.foreach { t =>
      val siteId = t._1
      val quantityByItemClass = t._2
      var longMap: Map[Long, (ShippingBox, ShippingFee, Int)] = LongMap()

      val list = SQL(
        """
        select * from shipping_fee
        inner join shipping_box on shipping_box.shipping_box_id = shipping_fee.shipping_box_id
        where country_code = {countryCode}
        and location_code = {locationCode}
        and site_id = {siteId}
        """
      ).on(
        'countryCode -> countryCode.code,
        'locationCode -> locationCode,
        'siteId -> siteId
      ).as(
        withBoxFee *
      )
      
      if (list.isEmpty) throw new CannotShippingException(siteId, locationCode)
      val boxesByItemClass = list.map { e => (e._1.itemClass -> e) }.toMap

      quantityByItemClass.foreach { e =>
        val itemClass = e._1

        boxesByItemClass.get(itemClass) match {
          case None => throw new CannotShippingException(siteId, locationCode, itemClass)
          case Some(box) => longMap += (itemClass -> (box._1, box._2, e._2))
        }
      }

      val price = addPrice(longMap, now)
      map += (Site(siteId) -> price._1)
      totalQuantity += price._2
      total += price._3
    }

    ShippingTotal(map, totalQuantity, total)
  }

  def addPrice(
    map: Map[Long, (ShippingBox, ShippingFee, Int)], now: Long
  )(
    implicit conn: Connection
  ): (Map[Long, ShippingTotalEntry], Int, BigDecimal) = {
    var totalQuantity = 0
    var total = BigDecimal(0)
    var ret: Map[Long, ShippingTotalEntry] = LongMap()

    map.foreach { e =>
      val quantity = e._2._3
      val boxSize = e._2._1.boxSize
      val boxCount = (quantity + boxSize - 1) / boxSize
      val feeId = e._2._2.id.get

      atOpt(feeId, now) match {
        case None => throw new RuntimeException(
          "shipping_fee_history record not found(shipping_fee_id = " + feeId + ")"
        )
        case Some(boxFeeHistory) => {
          val taxHistory = TaxHistory.at(boxFeeHistory.taxId)
          ret += (e._1 -> ShippingTotalEntry(e._2._1, e._2._2, quantity, boxCount, boxFeeHistory.fee, taxHistory))
          totalQuantity += boxCount
          total += boxFeeHistory.fee * boxCount;
        }
      }
    }

    (ret, totalQuantity, total)
  }
}
