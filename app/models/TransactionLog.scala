package models

import anorm._
import anorm.SqlParser
import play.api.Play.current
import scala.language.postfixOps
import collection.immutable
import java.sql.Connection
import helpers.RandomTokenGenerator

case class TransactionLogHeader(
  id: Option[Long] = None,
  userId: Long,
  transactionTime: Long,
  currencyId: Long,
  // Item total and shipping total. Excluding outer tax, including inner tax.
  totalAmount: BigDecimal,
  // Outer tax + Inner tax.
  taxAmount: BigDecimal,
  transactionType: TransactionType
)

case class TransactionLogSite(
  id: Option[Long] = None,
  transactionId: Long,
  siteId: Long,
  // Item total and shipping total. Excluding outer tax, including inner tax.
  totalAmount: BigDecimal,
  // Outer + Inner tax of items and shipping.
  taxAmount: BigDecimal
)

case class TransactionLogShipping(
  id: Option[Long] = None,
  transactionSiteId: Long,
  amount: BigDecimal, // Unit price * boxCount
  costAmount: Option[BigDecimal], // Unit cost price * boxCount
  addressId: Long,
  itemClass: Long,
  boxSize: Int,
  taxId: Long,
  boxCount: Int,
  boxName: String,
  shippingDate: Long
)

case class TransactionLogTax(
  id: Option[Long] = None,
  transactionSiteId: Long,
  taxId: Long,
  taxType: TaxType,
  rate: BigDecimal,
  targetAmount: BigDecimal,
  amount: BigDecimal
)

case class TransactionLogItem(
  id: Option[Long] = None,
  transactionSiteId: Long,
  itemId: Long,
  itemPriceHistoryId: Long,
  quantity: Long,
  amount: BigDecimal,
  costPrice: BigDecimal,
  taxId: Long
)

case class TransactionLogCouponId(id: Long) extends AnyVal

case class TransactionLogCoupon(
  id: Option[TransactionLogCouponId] = None,
  transactionItemId: Long,
  couponId: CouponId
)

case class TransactionLogItemNumericMetadataId(id: Long) extends AnyVal

case class TransactionLogItemNumericMetadata(
  id: Option[TransactionLogItemNumericMetadataId] = None,
  transactionItemId: Long,
  metadataType: ItemNumericMetadataType,
  metadata: Long
)

case class TransactionLogItemTextMetadataId(id: Long) extends AnyVal

case class TransactionLogItemTextMetadata(
  id: Option[TransactionLogItemTextMetadataId] = None,
  transactionItemId: Long,
  metadataType: ItemTextMetadataType,
  metadata: String
)

case class TransactionLogSiteItemNumericMetadataId(id: Long) extends AnyVal

case class TransactionLogSiteItemNumericMetadata(
  id: Option[TransactionLogSiteItemNumericMetadataId] = None,
  transactionItemId: Long,
  metadataType: SiteItemNumericMetadataType,
  metadata: Long
)

case class TransactionLogSiteItemTextMetadataId(id: Long) extends AnyVal

case class TransactionLogSiteItemTextMetadata(
  id: Option[TransactionLogSiteItemTextMetadataId] = None,
  transactionItemId: Long,
  metadataType: SiteItemTextMetadataType,
  metadata: String
)

case class ShippingInfo(
  transporterId: Long,
  slipCode: String
)

case class TransactionLogCreditTenderId(id: Long) extends AnyVal

case class TransactionLogCreditTender(
  id: Option[TransactionLogCreditTenderId] = None,
  transactionId: Long,
  amount: BigDecimal
)

case class TransactionLogPaypalStatusId(id: Long) extends AnyVal

case class TransactionLogPaypalStatus(
  id: Option[TransactionLogPaypalStatusId] = None,
  transactionId: Long,
  status: PaypalStatus,
  token: Long
)

case class TransactionShipStatus(
  id: Option[Long] = None,
  transactionSiteId: Long,
  status: TransactionStatus,
  lastUpdate: Long,
  shippingInfo: Option[ShippingInfo],
  mailSent: Boolean,
  plannedShippingDate: Option[Long],
  plannedDeliveryDate: Option[Long]
)

case class ShippingDateEntry(
  siteId: Long,
  shippingDate: Long
)

case class ShippingDate(
  tables: Map[Long, ShippingDateEntry] = Map() // Key is siteId
) {
  def bySite(site: Site): ShippingDateEntry = bySiteId(site.id.get)
  def bySiteId(siteId: Long): ShippingDateEntry = tables(siteId)
}

case class Transaction(
  userId: Long,
  currency: CurrencyInfo,
  itemTotal: ShoppingCartTotal,
  shippingAddress: Option[Address],
  shippingTotal: ShippingTotal,
  shippingDate: ShippingDate,
  now: Long = System.currentTimeMillis
) {
  lazy val total = itemTotal.total + shippingTotal.boxTotal  // Including inner tax excluding outer tax.
  lazy val taxAmount: BigDecimal = {
    val sumByTaxId = (itemTotal.sumByTaxId.toList ++ shippingTotal.sumByTaxId.toList).foldLeft(
      immutable.LongMap[BigDecimal]().withDefaultValue(BigDecimal(0))
    ) { (sum, e) => 
      sum.updated(e._1, sum(e._1) + e._2)
    }

    val taxHistoryById = itemTotal.taxHistoryById ++ shippingTotal.taxHistoryById
    sumByTaxId.foldLeft(BigDecimal(0)) { (sum, e) =>
      sum + taxHistoryById(e._1).taxAmount(e._2)
    }
  }
  lazy val bySite: Map[Site, Transaction] = {
    itemTotal.bySite.foldLeft(
      new immutable.HashMap[Site, Transaction]()
    ) { (map, e) =>
      map.updated(
        e._1,
        Transaction(
          userId, currency, e._2, shippingAddress, shippingTotal.bySite(e._1), shippingDate, now
        )
      )
    }
  }
}

case class CouponDetail(
  tranHeaderId: Long,
  tranCouponId: TransactionLogCouponId,
  site: Site,
  time: Long,
  itemId: ItemId,
  itemName: String,
  couponId: CouponId
)

case class CouponDetailWithMetadata(
  couponDetail: CouponDetail,
  itemNumericMetadata: Map[ItemNumericMetadataType, ItemNumericMetadata],
  itemTextMetadata: Map[ItemTextMetadataType, ItemTextMetadata],
  siteItemNumericMetadata: Map[SiteItemNumericMetadataType, SiteItemNumericMetadata],
  siteItemTextMetadata: Map[SiteItemTextMetadataType, SiteItemTextMetadata]
)

object TransactionLogHeader {
  val simple = {
    SqlParser.get[Option[Long]]("transaction_header.transaction_id") ~
    SqlParser.get[Long]("transaction_header.store_user_id") ~
    SqlParser.get[java.util.Date]("transaction_header.transaction_time") ~
    SqlParser.get[Long]("transaction_header.currency_id") ~
    SqlParser.get[java.math.BigDecimal]("transaction_header.total_amount") ~
    SqlParser.get[java.math.BigDecimal]("transaction_header.tax_amount") ~
    SqlParser.get[Int]("transaction_header.transaction_type") map {
      case transactionId~userId~transactionTime~currencyId~totalAmount~taxAmount~transactionType =>
        TransactionLogHeader(transactionId, userId, transactionTime.getTime, currencyId, totalAmount,
                             taxAmount, TransactionType.byIndex(transactionType))
    }
  }

  def createNew(
    userId: Long, currencyId: Long,
    totalAmount: BigDecimal, taxAmount: BigDecimal,
    transactionType: TransactionType,
    now: Long = System.currentTimeMillis
  )(implicit conn: Connection): TransactionLogHeader = {
    SQL(
      """
      insert into transaction_header (
        transaction_id, store_user_id, transaction_time, 
        currency_id, total_amount, tax_amount, transaction_type
      ) values (
        (select nextval('transaction_header_seq')),
        {userId}, {transactionTime}, {currencyId},
        {totalAmount}, {taxAmount}, {transactionType}
      )
      """
    ).on(
      'userId -> userId,
      'transactionTime -> new java.util.Date(now),
      'currencyId -> currencyId,
      'totalAmount -> totalAmount.bigDecimal,
      'taxAmount -> taxAmount.bigDecimal,
      'transactionType -> transactionType.ordinal
    ).executeUpdate()

    val id = SQL("select currval('transaction_header_seq')").as(SqlParser.scalar[Long].single)

    TransactionLogHeader(Some(id), userId, now, currencyId, totalAmount, taxAmount, transactionType)
  }

  def list(limit: Int = 20, offset: Int = 0)(implicit conn: Connection): Seq[TransactionLogHeader] =
    SQL(
      """
      select * from transaction_header
      order by transaction_time desc
      limit {limit} offset {offset}
      """
    ).on(
      'limit -> limit,
      'offset -> offset
    ).as(
      simple *
    )

  def apply(id: Long)(implicit conn: Connection): TransactionLogHeader =
    SQL(
      """
      select * from transaction_header
      where transaction_id = {id}
      """
    ).on(
      'id -> id
    ).as(
      simple.single
    )
}

object TransactionLogSite {
  val simple = {
    SqlParser.get[Option[Long]]("transaction_site.transaction_site_id") ~
    SqlParser.get[Long]("transaction_site.transaction_id") ~
    SqlParser.get[Long]("transaction_site.site_id") ~
    SqlParser.get[java.math.BigDecimal]("transaction_site.total_amount") ~
    SqlParser.get[java.math.BigDecimal]("transaction_site.tax_amount") map {
      case id~transactionId~siteId~totalAmount~taxAmount =>
        TransactionLogSite(id, transactionId, siteId, totalAmount, taxAmount)
    }
  }

  def createNew(
    transactionId: Long, siteId: Long, totalAmount: BigDecimal, taxAmount: BigDecimal
  )(implicit conn: Connection): TransactionLogSite = {
    SQL(
      """
      insert into transaction_site (
        transaction_site_id, transaction_id, site_id, total_amount, tax_amount
      ) values (
        (select nextval('transaction_site_seq')),
        {transactionId}, {siteId}, {totalAmount}, {taxAmount}
      )
      """
    ).on(
      'transactionId -> transactionId,
      'siteId -> siteId,
      'totalAmount -> totalAmount.bigDecimal,
      'taxAmount -> taxAmount.bigDecimal
    ).executeUpdate()

    val id = SQL("select currval('transaction_site_seq')").as(SqlParser.scalar[Long].single)

    TransactionLogSite(Some(id), transactionId, siteId, totalAmount, taxAmount)
  }

  def byId(id: Long)(implicit conn: Connection): TransactionLogSite =
    SQL(
      """
      select * from transaction_site
      where transaction_site_id = {id}
      """
    ).on(
      'id -> id
    ).as(
      simple.single
    )

  def list(limit: Int = 20, offset: Int = 0)(implicit conn: Connection): Seq[TransactionLogSite] = {
    SQL(
      """
      select * from transaction_site
      order by transaction_id, site_id
      limit {limit} offset {offset}
      """
    ).on(
      'limit -> limit,
      'offset -> offset
    ).as(
      simple *
    )
  }
}

object TransactionLogShipping {
  val simple = {
    SqlParser.get[Option[Long]]("transaction_shipping.transaction_shipping_id") ~
    SqlParser.get[Long]("transaction_shipping.transaction_site_id") ~
    SqlParser.get[java.math.BigDecimal]("transaction_shipping.amount") ~
    SqlParser.get[Option[java.math.BigDecimal]]("transaction_shipping.cost_amount") ~
    SqlParser.get[Long]("transaction_shipping.address_id") ~
    SqlParser.get[Long]("transaction_shipping.item_class") ~
    SqlParser.get[Int]("transaction_shipping.box_size") ~
    SqlParser.get[Long]("transaction_shipping.tax_id") ~
    SqlParser.get[Int]("transaction_shipping.box_count") ~
    SqlParser.get[String]("transaction_shipping.box_name") ~
    SqlParser.get[java.util.Date]("transaction_shipping.shipping_date") map {
      case id~transactionId~amount~costAmount~addressId~itemClass~boxSize~taxId~boxCount~boxName~shippingDate =>
        TransactionLogShipping(id, transactionId, amount, costAmount.map {BigDecimal.apply}, addressId, itemClass, boxSize, taxId, boxCount, boxName, shippingDate.getTime)
    }
  }

  def createNew(
    transactionSiteId: Long, amount: BigDecimal, costAmount: Option[BigDecimal], addressId: Long,
    itemClass: Long, boxSize: Int, taxId: Long, boxCount: Int, boxName: String, shippingDate: Long
  )(implicit conn: Connection): TransactionLogShipping = {
    SQL(
      """
      insert into transaction_shipping (
        transaction_shipping_id, transaction_site_id, amount, cost_amount, address_id,
        item_class, box_size, tax_id, box_count, box_name, shipping_date
      ) values (
        (select nextval('transaction_shipping_seq')),
        {transactionSiteId}, {amount}, {costAmount}, {addressId},
        {itemClass}, {boxSize}, {taxId}, {boxCount}, {boxName}, {shippingDate}
      )
      """
    ).on(
      'transactionSiteId -> transactionSiteId,
      'amount -> amount.bigDecimal,
      'costAmount -> costAmount.map {_.bigDecimal},
      'addressId -> addressId,
      'itemClass -> itemClass,
      'boxSize -> boxSize,
      'taxId -> taxId,
      'boxCount -> boxCount,
      'boxName -> boxName,
      'shippingDate -> new java.util.Date(shippingDate)
    ).executeUpdate()

    val id = SQL("select currval('transaction_shipping_seq')").as(SqlParser.scalar[Long].single)

    TransactionLogShipping(Some(id), transactionSiteId, amount, costAmount, addressId, itemClass, boxSize, taxId, boxCount, boxName, shippingDate)
  }

  def list(limit: Int = 20, offset: Int = 0)(implicit conn: Connection): Seq[TransactionLogShipping] =
    SQL(
      """
      select * from transaction_shipping
      order by transaction_site_id
      limit {limit} offset {offset}
      """
    ).on(
      'limit -> limit,
      'offset -> offset
    ).as(
      simple *
    )

  def listBySite(tranSiteId: Long)(implicit conn: Connection): Seq[TransactionLogShipping] =
    SQL(
      """
      select * from transaction_shipping
      where transaction_site_id = {tranSiteId}
      order by transaction_shipping_id
      """
    ).on(
      'tranSiteId -> tranSiteId
    ).as(
      simple *
    )
}

object TransactionLogTax {
  val simple = {
    SqlParser.get[Option[Long]]("transaction_tax.transaction_tax_id") ~
    SqlParser.get[Long]("transaction_tax.transaction_site_id") ~
    SqlParser.get[Long]("transaction_tax.tax_id") ~
    SqlParser.get[Int]("transaction_tax.tax_type") ~
    SqlParser.get[java.math.BigDecimal]("transaction_tax.rate") ~
    SqlParser.get[java.math.BigDecimal]("transaction_tax.target_amount") ~
    SqlParser.get[java.math.BigDecimal]("transaction_tax.amount") map {
      case id~transactionId~taxId~taxType~rate~targetAmount~amount =>
        TransactionLogTax(id, transactionId, taxId, TaxType.byIndex(taxType), rate, targetAmount, amount)
    }
  }

  def createNew(
    transactionSiteId: Long, taxHistoryId: Long, taxId: Long, taxType: TaxType,
    rate: BigDecimal, targetAmount: BigDecimal, amount: BigDecimal
  )(implicit conn: Connection): TransactionLogTax = {
    SQL(
      """
      insert into transaction_tax (
        transaction_tax_id, transaction_site_id, tax_id, tax_type, rate, target_amount, amount
      ) values (
        (select nextval('transaction_tax_seq')),
        {transactionSiteId}, {taxId}, {taxType}, {rate}, {targetAmount}, {amount}
      )
      """
    ).on(
      'transactionSiteId -> transactionSiteId,
      'taxHistoryId -> taxHistoryId,
      'taxId -> taxId,
      'taxType -> taxType.ordinal,
      'rate -> rate.bigDecimal,
      'targetAmount -> targetAmount.bigDecimal,
      'amount -> amount.bigDecimal
    ).executeUpdate()

    val id = SQL("select currval('transaction_tax_seq')").as(SqlParser.scalar[Long].single)

    TransactionLogTax(Some(id), transactionSiteId, taxId, taxType, rate, targetAmount, amount)
  }

  def list(limit: Int = 20, offset: Int = 0)(implicit conn: Connection): Seq[TransactionLogTax] =
    SQL(
      """
      select * from transaction_tax
      order by transaction_site_id
      limit {limit} offset {offset}
      """
    ).on(
      'limit -> limit,
      'offset -> offset
    ).as(
      simple *
    )
}

object TransactionLogItem {
  val simple = {
    SqlParser.get[Option[Long]]("transaction_item.transaction_item_id") ~
    SqlParser.get[Long]("transaction_item.transaction_site_id") ~
    SqlParser.get[Long]("transaction_item.item_id") ~
    SqlParser.get[Long]("transaction_item.item_price_history_id") ~
    SqlParser.get[Int]("transaction_item.quantity") ~
    SqlParser.get[java.math.BigDecimal]("transaction_item.amount") ~
    SqlParser.get[java.math.BigDecimal]("transaction_item.cost_price") ~
    SqlParser.get[Long]("transaction_item.tax_id") map {
      case id~tranSiteId~itemId~priceHistoryId~quantity~amount~costPrice~taxId =>
        TransactionLogItem(id, tranSiteId, itemId, priceHistoryId, quantity, amount, costPrice, taxId)
    }
  }

  def createNew(
    transactionSiteId: Long, itemId: Long, itemPriceHistoryId: Long, quantity: Long, amount: BigDecimal,
    costPrice: BigDecimal, taxId: Long
  )(implicit conn: Connection): TransactionLogItem = {
    SQL(
      """
      insert into transaction_item (
        transaction_item_id, transaction_site_id, item_id, item_price_history_id, quantity,
        amount, cost_price, tax_id
      ) values (
        (select nextval('transaction_item_seq')),
        {transactionSiteId}, {itemId}, {itemPriceHistoryId}, {quantity}, {amount}, {costPrice}, {taxId}
      )
      """
    ).on(
      'transactionSiteId -> transactionSiteId,
      'itemId -> itemId,
      'itemPriceHistoryId -> itemPriceHistoryId,
      'quantity -> quantity,
      'amount -> amount.bigDecimal,
      'costPrice -> costPrice.bigDecimal,
      'taxId -> taxId
    ).executeUpdate()

    val id = SQL("select currval('transaction_item_seq')").as(SqlParser.scalar[Long].single)

    TransactionLogItem(
      Some(id), transactionSiteId, itemId, itemPriceHistoryId, quantity, amount, costPrice, taxId
    )
  }

  def list(limit: Int = 20, offset: Int = 0)(implicit conn: Connection): Seq[TransactionLogItem] =
    SQL(
      """
      select * from transaction_item
      order by transaction_site_id
      limit {limit} offset {offset}
      """
    ).on(
      'limit -> limit,
      'offset -> offset
    ).as(
      simple *
    )

  def listBySite(tranSiteId: Long)(implicit conn: Connection): Seq[TransactionLogItem] =
    SQL(
      """ 
      select * from transaction_item
      where transaction_site_id = {tranSiteId}
      order by transaction_item_id
      """
    ).on(
      'tranSiteId -> tranSiteId
    ).as(
      simple *
    )
}

object TransactionLogCreditTender {
  val simple = {
    SqlParser.get[Option[Long]]("transaction_credit_tender.transaction_credit_tender_id") ~
    SqlParser.get[Long]("transaction_credit_tender.transaction_id") ~
    SqlParser.get[java.math.BigDecimal]("transaction_credit_tender.amount") map {
      case id~transactionId~amount =>
        TransactionLogCreditTender(
          id.map(TransactionLogCreditTenderId.apply),
          transactionId,
          amount
        )
    }
  }

  def createNew(
    transactionId: Long, amount: BigDecimal
  )(implicit conn: Connection) : TransactionLogCreditTender = {
    SQL(
      """
      insert into transaction_credit_tender (
        transaction_credit_tender_id, transaction_id, amount
      ) values (
        (select nextval('transaction_credit_tender_seq')),
        {transactionId}, {amount}
      )
      """
    ).on(
      'transactionId -> transactionId,
      'amount -> amount
    ).executeUpdate()

    val id = SQL("select currval('transaction_credit_tender_seq')").as(SqlParser.scalar[Long].single)

    TransactionLogCreditTender(
      Some(TransactionLogCreditTenderId(id)), transactionId, amount
    )
  }

  def byTransactionId(tranId: Long)(implicit conn: Connection): TransactionLogCreditTender =
    SQL(
      """
      select * from transaction_credit_tender
      where transaction_id = {tranId}
      """
    ).on(
      'tranId -> tranId
    ).as(
      simple.single
    )

  def list(limit: Int = 10, offset: Int = 0)(implicit conn: Connection): Seq[TransactionLogCreditTender] =
    SQL(
      """
      select * from transaction_credit_tender
      order by transaction_credit_tender_id
      limit {limit} offset {offset}
      """
    ).on(
      'limit -> limit,
      'offset -> offset
    ).as(
      simple *
    )
}

object TransactionLogPaypalStatus {
  val simple = {
    SqlParser.get[Option[Long]]("transaction_paypal_status.transaction_payapl_status_id") ~
    SqlParser.get[Long]("transaction_paypal_status.transaction_id") ~
    SqlParser.get[Int]("transaction_paypal_status.status") ~
    SqlParser.get[Long]("transaction_paypal_status.token") map {
      case id~transactionId~status~token =>
        TransactionLogPaypalStatus(
          id.map(TransactionLogPaypalStatusId.apply),
          transactionId,
          PaypalStatus.byIndex(status),
          token
        )
    }
  }

  def createNew(
    transactionId: Long, status: PaypalStatus, token: Long
  )(implicit conn: Connection): TransactionLogPaypalStatus = {
    SQL(
      """
      insert into transaction_paypal_status (
        transaction_paypal_status_id,
        transaction_id, status, token
      ) values (
        (select nextval('transaction_paypal_status_seq')),
        {transactionId}, {status}, {token}
      )
      """
    ).on(
      'transactionId -> transactionId,
      'status -> status.ordinal,
      'token -> token
    ).executeUpdate()

    val id = SQL("select currval('transaction_paypal_status_seq')").as(SqlParser.scalar[Long].single)

    TransactionLogPaypalStatus(
      Some(TransactionLogPaypalStatusId(id)),
      transactionId, status, token
    )
  }

  def update(transactionId: Long, status: PaypalStatus)(implicit conn: Connection): Int = SQL(
    """
    update transaction_paypal_status
    set status = {status}
    where transaction_id = {transactionId}
    """
  ).on(
    'status -> status.ordinal,
    'transactionId -> transactionId
  ).executeUpdate()
}

object TransactionLogCoupon {
  val TransactionLogCouponDefaultOrderBy = OrderBy("h.transaction_id", Desc)
  val DefaultOrderByMap = immutable.Map(
    "h.transaction_id" -> OrderBy("h.transaction_id", Desc),
    "s.site_id" -> OrderBy("s.site_id", Desc),
    "c.coupon_id" -> OrderBy("c.coupon_id", Desc)
  )

  val simple = {
    SqlParser.get[Option[Long]]("transaction_coupon.transaction_coupon_id") ~
    SqlParser.get[Long]("transaction_coupon.transaction_item_id") ~
    SqlParser.get[Long]("transaction_coupon.coupon_id") map {
      case id~tranItemId~couponId =>
        TransactionLogCoupon(id.map(TransactionLogCouponId.apply), tranItemId, CouponId(couponId))
    }
  }

  def createNew(
    transactionItemId: Long, couponId: CouponId
  )(implicit conn: Connection): TransactionLogCoupon = {
    SQL(
      """
      insert into transaction_coupon (
        transaction_coupon_id, transaction_item_id, coupon_id
      ) values (
        (select nextval('transaction_coupon_seq')),
        {transactionItemId}, {couponId}
      )
      """
    ).on(
      'transactionItemId -> transactionItemId,
      'couponId -> couponId.id
    ).executeUpdate()

    val id = SQL("select currval('transaction_coupon_seq')").as(SqlParser.scalar[Long].single)

    TransactionLogCoupon(
      Some(TransactionLogCouponId(id)), transactionItemId, couponId
    )
  }

  def couponParser(implicit conn: Connection) = 
    SqlParser.get[Long]("transaction_header.transaction_id") ~
    SqlParser.get[Long]("transaction_coupon.transaction_coupon_id") ~
    SqlParser.get[Long]("transaction_site.site_id") ~
    SqlParser.get[java.util.Date]("transaction_header.transaction_time") ~
    SqlParser.get[Long]("transaction_item.item_id") ~
    SqlParser.get[String]("item_name.item_name") ~
    SqlParser.get[Long]("transaction_coupon.coupon_id") map {
      case tranId~tranCouponId~siteId~time~itemId~itemName~couponId =>
        CouponDetail(
          tranId, TransactionLogCouponId(tranCouponId), Site(siteId), time.getTime,
          ItemId(itemId), itemName, CouponId(couponId)
        )
    }

  def list(
    locale: LocaleInfo, userId: Long, 
    page: Int = 0, pageSize: Int = 10, orderBy: OrderBy = TransactionLogCouponDefaultOrderBy
  )(implicit conn: Connection): PagedRecords[CouponDetail] = {

    val orderByTable = DefaultOrderByMap + (orderBy.columnName -> orderBy)

    val baseSql = 
      """
      from transaction_header h
      inner join transaction_site s on h.transaction_id = s.transaction_id
      inner join transaction_item i on s.transaction_site_id = i.transaction_site_id
      inner join transaction_coupon c on i.transaction_item_id = c.transaction_item_id
      inner join item it on it.item_id = i.item_id
      inner join item_name iname on iname.item_id = i.item_id
      where h.store_user_id = {userId}
      and iname.locale_id = {localeId}
    """

    val count = SQL(
      "select count(*) " + baseSql
    ).on(
      'userId -> userId,
      'localeId -> locale.id
    ).as(
      SqlParser.scalar[Long].single
    )

    val list: Seq[CouponDetail] = SQL(
      """
      select
        h.transaction_id,
        c.transaction_coupon_id,
        s.site_id,
        h.transaction_time,
        i.item_id,
        iname.item_name,
        c.coupon_id
      """ +
      baseSql +
      " order by " +
      orderByTable("h.transaction_id") + ", " +
      orderByTable("s.site_id") + ", " +
      orderByTable("c.coupon_id")
    ).on(
      'userId -> userId,
      'localeId -> locale.id
    ).as(
      couponParser *
    )

    PagedRecords(page, pageSize, (count + pageSize - 1) / pageSize, orderBy, list)
  }

  def at(
    locale: LocaleInfo, userId: Long, id: TransactionLogCouponId
  )(implicit conn: Connection): CouponDetailWithMetadata = {
    val c: CouponDetail = SQL(
      """
      select * from transaction_header h
      inner join transaction_site s on h.transaction_id = s.transaction_id
      inner join transaction_item i on s.transaction_site_id = i.transaction_site_id
      inner join transaction_coupon c on i.transaction_item_id = c.transaction_item_id
      inner join item it on it.item_id = i.item_id
      inner join item_name iname on iname.item_id = i.item_id
      where c.transaction_coupon_id = {id}
      and h.store_user_id = {userId}
      and iname.locale_id = {localeId}
      """
    ).on(
      'id -> id.id,
      'userId -> userId,
      'localeId -> locale.id
    ).as(
      couponParser.single
    )

    CouponDetailWithMetadata(
      c,
      ItemNumericMetadata.allById(c.itemId),
      ItemTextMetadata.allById(c.itemId),
      SiteItemNumericMetadata.all(c.site.id.get, c.itemId),
      SiteItemTextMetadata.all(c.site.id.get, c.itemId)
    )
  }
}

case class PersistedTransaction(
  header: TransactionLogHeader,
  tranSiteLog: Map[Long, TransactionLogSite], // First key = siteId
  siteTable: Seq[Site],
  shippingTable: Map[Long, Seq[TransactionLogShipping]], // First key = siteId
  taxTable: Map[Long, Seq[TransactionLogTax]], // First key = siteId
  itemTable: Map[Long, Seq[(ItemName, TransactionLogItem, Option[TransactionLogCoupon])]], // First key = siteId
  creditTable: Option[TransactionLogCreditTender] = None
) {
  lazy val outerTaxWhenCostPrice: Map[Long, BigDecimal] = {
    var result = immutable.LongMap[BigDecimal]()

    siteTable.foreach { site =>
      var amountByTaxId = immutable.LongMap[BigDecimal]()
      val taxByTaxId = taxBySiteIdAndTaxId(site.id.get)

      itemTable(site.id.get).foreach { s =>
        if (taxByTaxId(s._2.taxId).taxType == TaxType.OUTER_TAX)
          amountByTaxId = amountByTaxId.updated(
            s._2.taxId,
            s._2.costPrice * s._2.quantity + amountByTaxId.get(s._2.taxId).getOrElse(BigDecimal(0))
          )
      }
      shippingTable(site.id.get).foreach { s =>
        if (taxByTaxId(s.taxId).taxType == TaxType.OUTER_TAX)
          amountByTaxId = amountByTaxId.updated(s.taxId, s.amount + amountByTaxId(s.taxId))
      }

      result = result.updated(
        site.id.get,
        amountByTaxId.foldLeft(BigDecimal(0)) { (sum, e) =>
          sum + Tax.outerTax(e._2, TaxType.OUTER_TAX, taxByTaxId(e._1).rate / BigDecimal(100))
        }
      )
    }

    result
  }
  // Key: siteId, taxId
  lazy val taxBySiteIdAndTaxId: Map[Long, Map[Long, TransactionLogTax]] = taxTable.mapValues {
    seq => seq.foldLeft(
      immutable.LongMap[TransactionLogTax]()
    ) {
      (map, e) => map.updated(e.taxId, e)
    }
  }
  lazy val sites: Map[Long, Site] = siteTable.foldLeft(
    immutable.LongMap[Site]()
  ) {
    (map, e) => map.updated(e.id.get, e)
  }
  lazy val itemTotal: Map[Long, BigDecimal] = itemTable.foldLeft(
    immutable.LongMap[BigDecimal]()
  ) {
    (map, e) => map.updated(e._1, e._2.map(_._2.amount).foldLeft(BigDecimal(0))(_ + _))
  }
  lazy val costPriceTotal: Map[Long, BigDecimal] = itemTable.foldLeft(
    immutable.LongMap[BigDecimal]()
  ) {
    (map, e) => map.updated(
      e._1,
      e._2.foldLeft(BigDecimal(0)) {(sum, t) =>
        sum + t._2.costPrice * t._2.quantity
      }
    )
  }
  lazy val itemGrandTotal: BigDecimal = itemTotal.values.fold(BigDecimal(0))(_ + _)
  lazy val itemQuantity: Map[Long, Long] = itemTable.foldLeft(
    immutable.LongMap[Long]()
  ) {
    (map, e) => map.updated(e._1, e._2.map(_._2.quantity).foldLeft(0L)(_ + _))
  }
  lazy val itemGrandQuantity: Long = itemQuantity.values.fold(0L)(_ + _)
  lazy val boxTotal: Map[Long, BigDecimal] = shippingTable.foldLeft(
    immutable.LongMap[BigDecimal]()
  ) {
    (map, e) => map.updated(e._1, e._2.map(_.amount).foldLeft(BigDecimal(0))(_ + _))
  }.withDefaultValue(BigDecimal(0))
  lazy val boxGrandTotal: BigDecimal = boxTotal.values.fold(BigDecimal(0))(_ + _)
  lazy val boxQuantity: Map[Long, Int] = shippingTable.foldLeft(
    immutable.LongMap[Int]()
  ) {
    (map, e) => map.updated(e._1, e._2.map(_.boxCount).foldLeft(0)(_ + _))
  }.withDefaultValue(0)
  lazy val boxGrandQuantity: Int = boxQuantity.values.fold(0)(_ + _)
  lazy val boxNameBySiteIdAndItemClass: Map[Long, Map[Long, String]] = shippingTable.foldLeft(
    immutable.LongMap[Map[Long, String]]().withDefaultValue(immutable.LongMap[String]())
  ) {
    (map, e) => map.updated(e._1, e._2.map {e => (e.itemClass -> e.boxName)}.toMap)
  }
  lazy val outerTaxTotal: Map[Long, BigDecimal] = taxTable.foldLeft(
    immutable.LongMap[BigDecimal]()
  ) {
    (map, e) => map.updated(
      e._1, e._2.filter(_.taxType == TaxType.OUTER_TAX).map(_.amount).foldLeft(BigDecimal(0))(_ + _)
    )
  }
  lazy val outerTaxGrandTotal: BigDecimal = outerTaxTotal.values.fold(BigDecimal(0))(_ + _)
}

object TransactionShipStatus {
  val simple = {
    SqlParser.get[Option[Long]]("transaction_status.transaction_status_id") ~
    SqlParser.get[Long]("transaction_status.transaction_site_id") ~
    SqlParser.get[Int]("transaction_status.status") ~
    SqlParser.get[Option[Long]]("transaction_status.transporter_id") ~
    SqlParser.get[Option[String]]("transaction_status.slip_code") ~
    SqlParser.get[Boolean]("transaction_status.mail_sent") ~
    SqlParser.get[java.util.Date]("transaction_status.last_update") ~
    SqlParser.get[Option[java.util.Date]]("transaction_status.planned_shipping_date") ~
    SqlParser.get[Option[java.util.Date]]("transaction_status.planned_delivery_date") map {
      case id~tranSiteId~status~transporterId~slipCode~mailSent~lastUpdate~plannedShippingDate~plannedDeliveryDate =>
        TransactionShipStatus(
          id, tranSiteId, TransactionStatus.byIndex(status), lastUpdate.getTime,
          transporterId.map {tid => ShippingInfo(tid, slipCode.get)}, mailSent,
          plannedShippingDate.map(_.getTime), plannedDeliveryDate.map(_.getTime)
        )
    }
  }
  
  def byId(id: Long)(implicit conn: Connection): TransactionShipStatus =
    SQL(
      """
      select * from transaction_status where transaction_status_id = {id}
      """
    ).on(
      'id -> id
    ).as(
      simple.single
    )
  
  def createNew(
    transactionSiteId: Long, status: TransactionStatus, lastUpdate: Long, shippingInfo: Option[ShippingInfo]
  )(implicit conn: Connection): TransactionShipStatus = {
    SQL(
      """
      insert into transaction_status (
        transaction_status_id, transaction_site_id, status,
        transporter_id, slip_code, last_update
      ) values (
        (select nextval('transaction_status_seq')),
        {transactionSiteId}, {status},
        {transporterId}, {slipCode}, {lastUpdate}
      )
      """
    ).on(
      'transactionSiteId -> transactionSiteId,
      'status -> status.ordinal,
      'transporterId -> shippingInfo.map(_.transporterId),
      'slipCode -> shippingInfo.map(_.slipCode),
      'lastUpdate -> new java.util.Date(lastUpdate)
    ).executeUpdate()

    val id = SQL("select currval('transaction_status_seq')").as(SqlParser.scalar[Long].single)

    TransactionShipStatus(Some(id), transactionSiteId, status, lastUpdate, shippingInfo, false, None, None)
  }

  def update(
    siteUser: Option[SiteUser], transactionSiteId: Long, status: TransactionStatus
  )(implicit conn: Connection): Int = SQL(
    """
    update transaction_status
    set status = {status},
    last_update = current_timestamp
    where transaction_site_id = {tranSiteId}
    """ + (
      siteUser match {
        case None => ""
        case Some(u) => "and " + u.siteId + " = " +
          """
          (
            select site_id from transaction_site
            where transaction_site.transaction_site_id = transaction_status.transaction_site_id
          )
          """
      }
    )
  ).on(
    'status -> status.ordinal,
    'tranSiteId -> transactionSiteId
  ).executeUpdate()

  def updateShippingInfo(
    siteUser: Option[SiteUser], transactionSiteId: Long, transporterId: Long, slipCode: String
  )(implicit conn: Connection): Int = SQL(
    """
    update transaction_status
    set transporter_id = {transporterId},
    slip_code = {slipCode},
    last_update = current_timestamp
    where transaction_site_id = {tranSiteId}
    """ + (
      // Prevent a site owner changing transaction status of other sites.
      siteUser match {
        case None => ""
        case Some(u) => "and " + u.siteId + " = " +
          """
          (
            select site_id from transaction_site
            where transaction_site.transaction_site_id = transaction_status.transaction_site_id
          )
          """
      }
    )
  ).on(
    'tranSiteId -> transactionSiteId,
    'transporterId -> transporterId,
    'slipCode -> slipCode
  ).executeUpdate()

  def updateShippingDeliveryDate(
    siteUser: Option[SiteUser], transactionSiteId: Long, shippingDate: Long, deliveryDate: Long
  )(implicit conn: Connection): Int = SQL(
    """
    update transaction_status
    set planned_shipping_date = {plannedShippingDate},
    planned_delivery_date = {plannedDeliveryDate},
    last_update = current_timestamp
    where transaction_site_id = {tranSiteId}
    """ + (
      // Prevent a site owner changing transaction status of other sites.
      siteUser match {
        case None => ""
        case Some(u) => "and " + u.siteId + " = " +
          """
          (
            select site_id from transaction_site
            where transaction_site.transaction_site_id = transaction_status.transaction_site_id
          )
          """
      }
    )
  ).on(
    'tranSiteId -> transactionSiteId,
    'plannedShippingDate -> new java.util.Date(shippingDate),
    'plannedDeliveryDate -> new java.util.Date(deliveryDate)
  ).executeUpdate()

  def getByTransactionSiteId(tranSiteId: Long)(implicit conn: Connection): Option[TransactionShipStatus] =
    SQL(
      """
      select * from transaction_status where transaction_site_id = {id}
      """
    ).on(
      'id -> tranSiteId
    ).as(
      simple.singleOpt
    )

  def byTransactionSiteId(tranSiteId: Long)(implicit conn: Connection) =
    getByTransactionSiteId(tranSiteId).get

  def mailSent(tranSiteId: Long)(implicit conn: Connection) {
    SQL(
      """
      update transaction_status
      set mail_sent = TRUE
      where transaction_site_id = {tranSiteId}
      """
    ).on(
      'tranSiteId -> tranSiteId
    ).executeUpdate()
  }
}

object TransactionLogItemNumericMetadata {
  val simple = {
    SqlParser.get[Option[Long]]("transaction_item_numeric_metadata.transaction_item_numeric_metadata_id") ~
    SqlParser.get[Long]("transaction_item_numeric_metadata.transaction_item_id") ~
    SqlParser.get[Int]("transaction_item_numeric_metadata.metadata_type") ~
    SqlParser.get[Long]("transaction_item_numeric_metadata.metadata") map {
      case id~tranItemId~metadataType~metadata =>
        TransactionLogItemNumericMetadata(
          id.map(TransactionLogItemNumericMetadataId.apply),
          tranItemId, ItemNumericMetadataType.byIndex(metadataType), metadata
        )
    }
  }

  def createNew(
    transactionItemId: Long, metadataTable: Iterable[ItemNumericMetadata]
  )(implicit conn: Connection): Iterable[TransactionLogItemNumericMetadata] = metadataTable.map { md =>
    SQL(
      """
      insert into transaction_item_numeric_metadata (
        transaction_item_numeric_metadata_id, transaction_item_id,
        metadata_type, metadata
      ) values (
        (select nextval('transaction_item_numeric_metadata_seq')),
        {transactionItemId}, {metadataType}, {metadata}
      )
      """
    ).on(
      'transactionItemId -> transactionItemId,
      'metadataType -> md.metadataType.ordinal,
      'metadata -> md.metadata
    ).executeUpdate()

    val id = SQL("select currval('transaction_item_numeric_metadata_seq')").as(SqlParser.scalar[Long].single)

    TransactionLogItemNumericMetadata(
      Some(TransactionLogItemNumericMetadataId(id)), transactionItemId, md.metadataType, md.metadata
    )
  }

  def list(transactionItemId: Long)(implicit conn: Connection): Seq[TransactionLogItemNumericMetadata] = SQL(
    """
    select * from transaction_item_numeric_metadata
    where transaction_item_id = {transactionItemId}
    order by metadata_type
    """
  ).on(
    'transactionItemId -> transactionItemId
  ).as(
    simple *
  )
}

object TransactionLogItemTextMetadata {
  val simple = {
    SqlParser.get[Option[Long]]("transaction_item_text_metadata.transaction_item_text_metadata_id") ~
    SqlParser.get[Long]("transaction_item_text_metadata.transaction_item_id") ~
    SqlParser.get[Int]("transaction_item_text_metadata.metadata_type") ~
    SqlParser.get[String]("transaction_item_text_metadata.metadata") map {
      case id~tranItemId~metadataType~metadata =>
        TransactionLogItemTextMetadata(
          id.map(TransactionLogItemTextMetadataId.apply),
          tranItemId, ItemTextMetadataType.byIndex(metadataType), metadata
        )
    }
  }

  def createNew(
    transactionItemId: Long, metadataTable: Iterable[ItemTextMetadata]
  )(implicit conn: Connection): Iterable[TransactionLogItemTextMetadata] = metadataTable.map { md =>
    SQL(
      """
      insert into transaction_item_text_metadata (
        transaction_item_text_metadata_id, transaction_item_id,
        metadata_type, metadata
      ) values (
        (select nextval('transaction_item_text_metadata_seq')),
        {transactionItemId}, {metadataType}, {metadata}
      )
      """
    ).on(
      'transactionItemId -> transactionItemId,
      'metadataType -> md.metadataType.ordinal,
      'metadata -> md.metadata
    ).executeUpdate()

    val id = SQL("select currval('transaction_item_text_metadata_seq')").as(SqlParser.scalar[Long].single)

    TransactionLogItemTextMetadata(
      Some(TransactionLogItemTextMetadataId(id)), transactionItemId, md.metadataType, md.metadata
    )
  }

  def list(transactionItemId: Long)(implicit conn: Connection): Seq[TransactionLogItemTextMetadata] = SQL(
    """
    select * from transaction_item_text_metadata
    where transaction_item_id = {transactionItemId}
    order by metadata_type
    """
  ).on(
    'transactionItemId -> transactionItemId
  ).as(
    simple *
  )
}

object TransactionLogSiteItemNumericMetadata {
  val simple = {
    SqlParser.get[Option[Long]]("transaction_site_item_numeric_metadata.transaction_site_item_numeric_metadata_id") ~
    SqlParser.get[Long]("transaction_site_item_numeric_metadata.transaction_item_id") ~
    SqlParser.get[Int]("transaction_site_item_numeric_metadata.metadata_type") ~
    SqlParser.get[Long]("transaction_site_item_numeric_metadata.metadata") map {
      case id~tranItemId~metadataType~metadata =>
        TransactionLogSiteItemNumericMetadata(
          id.map(TransactionLogSiteItemNumericMetadataId.apply),
          tranItemId, SiteItemNumericMetadataType.byIndex(metadataType), metadata
        )
    }
  }

  def createNew(
    transactionItemId: Long, metadataTable: Iterable[SiteItemNumericMetadata]
  )(implicit conn: Connection): Iterable[TransactionLogSiteItemNumericMetadata] = metadataTable.map { md =>
    SQL(
      """
      insert into transaction_site_item_numeric_metadata (
        transaction_site_item_numeric_metadata_id, transaction_item_id,
        metadata_type, metadata
      ) values (
        (select nextval('transaction_site_item_numeric_metadata_seq')),
        {transactionItemId}, {metadataType}, {metadata}
      )
      """
    ).on(
      'transactionItemId -> transactionItemId,
      'metadataType -> md.metadataType.ordinal,
      'metadata -> md.metadata
    ).executeUpdate()

    val id = SQL("select currval('transaction_site_item_numeric_metadata_seq')").as(SqlParser.scalar[Long].single)

    TransactionLogSiteItemNumericMetadata(
      Some(TransactionLogSiteItemNumericMetadataId(id)), transactionItemId, md.metadataType, md.metadata
    )
  }

  def list(transactionItemId: Long)(implicit conn: Connection): Seq[TransactionLogSiteItemNumericMetadata] = SQL(
    """
    select * from transaction_site_item_numeric_metadata
    where transaction_item_id = {transactionItemId}
    order by metadata_type
    """
  ).on(
    'transactionItemId -> transactionItemId
  ).as(
    simple *
  )
}

object TransactionLogSiteItemTextMetadata {
  val simple = {
    SqlParser.get[Option[Long]]("transaction_site_item_text_metadata.transaction_site_item_text_metadata_id") ~
    SqlParser.get[Long]("transaction_site_item_text_metadata.transaction_item_id") ~
    SqlParser.get[Int]("transaction_site_item_text_metadata.metadata_type") ~
    SqlParser.get[String]("transaction_site_item_text_metadata.metadata") map {
      case id~tranItemId~metadataType~metadata =>
        TransactionLogSiteItemTextMetadata(
          id.map(TransactionLogSiteItemTextMetadataId.apply),
          tranItemId, SiteItemTextMetadataType.byIndex(metadataType), metadata
        )
    }
  }

  def createNew(
    transactionItemId: Long, metadataTable: Iterable[SiteItemTextMetadata]
  )(implicit conn: Connection): Iterable[TransactionLogSiteItemTextMetadata] = metadataTable.map { md =>
    SQL(
      """
      insert into transaction_site_item_text_metadata (
        transaction_site_item_text_metadata_id, transaction_item_id,
        metadata_type, metadata
      ) values (
        (select nextval('transaction_site_item_text_metadata_seq')),
        {transactionItemId}, {metadataType}, {metadata}
      )
      """
    ).on(
      'transactionItemId -> transactionItemId,
      'metadataType -> md.metadataType.ordinal,
      'metadata -> md.metadata
    ).executeUpdate()

    val id = SQL("select currval('transaction_site_item_text_metadata_seq')").as(SqlParser.scalar[Long].single)

    TransactionLogSiteItemTextMetadata(
      Some(TransactionLogSiteItemTextMetadataId(id)), transactionItemId, md.metadataType, md.metadata
    )
  }

  def list(transactionItemId: Long)(implicit conn: Connection): Seq[TransactionLogSiteItemTextMetadata] = SQL(
    """
    select * from transaction_site_item_text_metadata
    where transaction_item_id = {transactionItemId}
    order by metadata_type
    """
  ).on(
    'transactionItemId -> transactionItemId
  ).as(
    simple *
  )
}

case class TransactionDetail(
  itemId: Long,
  siteId: Long,
  itemName: String,
  unitPrice: BigDecimal,
  costUnitPrice: BigDecimal,
  quantity: Int,
  itemNumericMetadata: Map[ItemNumericMetadataType, ItemNumericMetadata],
  siteItemNumericMetadata: Map[SiteItemNumericMetadataType, SiteItemNumericMetadata],
  itemTextMetadata: Map[ItemTextMetadataType, ItemTextMetadata]
) {
  lazy val price = unitPrice * quantity
  lazy val costPrice = costUnitPrice * quantity
}

object TransactionDetail {
  val parser = {
    SqlParser.get[String]("item_name.item_name") ~
    SqlParser.get[Int]("transaction_item.quantity") ~
    SqlParser.get[java.math.BigDecimal]("transaction_item.amount") ~
    SqlParser.get[java.math.BigDecimal]("transaction_item.cost_price") ~
    SqlParser.get[Long]("transaction_item.item_id") ~
    SqlParser.get[Long]("transaction_site.site_id") map {
      case name~quantity~amount~costPrice~itemId~siteId => (
        name, BigDecimal(amount) / quantity, costPrice, quantity, itemId, siteId
      )
    }
  }

  def show(
    tranSiteId: Long, locale: LocaleInfo, user: Option[SiteUser] = None
  )(implicit conn: Connection): Seq[TransactionDetail] = {
    SQL(
      """
      select * from transaction_item
      inner join item_name on item_name.item_id = transaction_item.item_id
      inner join transaction_site on transaction_site.transaction_site_id = transaction_item.transaction_site_id
      where transaction_item.transaction_site_id = {id}
      and item_name.locale_id = {locale}
      """ + (user match {
        case None => ""
        case Some(u) => "and transaction_site.site_id = " + u.siteId
      }) +
      """
      order by transaction_item_id
      """
    ).on(
      'id -> tranSiteId,
      'locale -> locale.id
    ).as(
      parser *
    ).map { e =>
      val metadata = ItemNumericMetadata.allById(ItemId(e._5))
      val textMetadata = ItemTextMetadata.allById(ItemId(e._5))
      val siteMetadata = SiteItemNumericMetadata.all(e._6, ItemId(e._5))
      TransactionDetail(e._5, e._6, e._1, e._2, e._3, e._4, metadata, siteMetadata, textMetadata)
    }
  }
}

class TransactionPersister {
  def persistPaypal(
    tran: Transaction
  )(implicit conn: Connection): (Long, immutable.Map[Site, immutable.Seq[TransactionLogTax]], Long) = {
    val (transactionId: Long, taxesBySite: immutable.Map[Site, immutable.Seq[TransactionLogTax]])
      = persist(tran, TransactionType.PAYPAL)

    val outerTax: BigDecimal = taxesBySite.values.foldLeft(BigDecimal(0)) { (sum, e) =>
      sum + e.foldLeft(BigDecimal(0)) { (sum2, e2) =>
        sum2 + (
          if (e2.taxType == TaxType.OUTER_TAX) e2.amount else BigDecimal(0)
        )
      }
    }
    val creditLog: TransactionLogCreditTender = TransactionLogCreditTender.createNew(
      transactionId, tran.total + outerTax
    )

    val paypalToken: Long = RandomTokenGenerator().next

    val paypalStatus: TransactionLogPaypalStatus = TransactionLogPaypalStatus.createNew(
      transactionId, PaypalStatus.START, paypalToken
    )

    (transactionId, taxesBySite, paypalToken)
  }

  def persist(
    tran: Transaction, transactionType: TransactionType = TransactionType.NORMAL
  )(implicit conn: Connection): (Long, immutable.Map[Site, immutable.Seq[TransactionLogTax]]) = {
    val header = TransactionLogHeader.createNew(
      tran.userId, tran.currency.id,
      tran.total, tran.taxAmount,
      transactionType,
      tran.now
    )

    val taxesBySite: immutable.Map[Site, immutable.Seq[TransactionLogTax]] =
      tran.itemTotal.bySite.keys.foldLeft(immutable.HashMap[Site, immutable.Seq[TransactionLogTax]]()) { (map, site) =>
        map.updated(
          site, saveSiteTotal(header, site, tran)
        )
      }

    (header.id.get, taxesBySite)
  }

  def saveSiteTotal(
    header: TransactionLogHeader,
    site: Site,
    tran: Transaction
  )(implicit conn: Connection): immutable.Seq[TransactionLogTax] = {
    val siteLog = TransactionLogSite.createNew(
      header.id.get, site.id.get,
      tran.bySite(site).total,
      tran.bySite(site).taxAmount
    )

    TransactionShipStatus.createNew(siteLog.id.get, TransactionStatus.ORDERED, System.currentTimeMillis, None)
    saveShippingTotal(siteLog, tran.bySite(site))
    val taxes: immutable.Seq[TransactionLogTax] = saveTax(siteLog, tran.bySite(site))
    saveItem(siteLog, tran.bySite(site))

    taxes
  }

  def saveShippingTotal(
    siteLog: TransactionLogSite, tran: Transaction
  )(implicit conn: Connection) {
    tran.shippingTotal.table.foreach { e =>
      tran.shippingAddress.foreach { addr =>
        TransactionLogShipping.createNew(
          siteLog.id.get, e.boxTotal, e.boxCostTotal, addr.id.get, e.itemClass, e.shippingBox.boxSize,
          e.boxTaxInfo.taxId, e.boxQuantity, e.shippingBox.boxName, tran.shippingDate.tables(e.site.id.get).shippingDate
        )
      }
    }
  }

  def saveTax(
    siteLog: TransactionLogSite, tran: Transaction
  )(implicit conn: Connection): immutable.Seq[TransactionLogTax] = {
    val taxTable = tran.shippingTotal.taxHistoryById ++ tran.itemTotal.taxHistoryById

    taxTable.map { e =>
      val taxId = e._1
      val taxHistory = e._2

      val targetAmount = tran.itemTotal.sumByTaxId(taxId) + tran.shippingTotal.sumByTaxId(taxId)
      val taxAmount = taxHistory.taxAmount(targetAmount)

      TransactionLogTax.createNew(
        siteLog.id.get, taxHistory.id.get, taxId, taxHistory.taxType,
        taxHistory.rate, targetAmount, taxAmount
      )
    }.toVector
  }

  def saveItem(
    siteLog: TransactionLogSite, tran: Transaction
  )(implicit conn: Connection) {
    tran.itemTotal.table.foreach { e =>
      val tranItem = TransactionLogItem.createNew(
        siteLog.id.get, e.shoppingCartItem.itemId, e.itemPriceHistory.id.get,
        e.quantity, e.itemPrice, e.costUnitPrice, e.itemPriceHistory.taxId
      )

      TransactionLogItemNumericMetadata.createNew(tranItem.id.get, e.itemNumericMetadata.values)
      TransactionLogItemTextMetadata.createNew(tranItem.id.get, e.itemTextMetadata.values)
      TransactionLogSiteItemNumericMetadata.createNew(tranItem.id.get, e.siteItemNumericMetadata.values)
      TransactionLogSiteItemTextMetadata.createNew(tranItem.id.get, e.siteItemTextMetadata.values)

      Coupon.getByItem(ItemId(e.shoppingCartItem.itemId)).foreach { coupon =>
        TransactionLogCoupon.createNew(
          tranItem.id.get, coupon.id.get
        )
      }
    }
  }

  val siteWithShipping = TransactionLogSite.simple ~ TransactionLogShipping.simple map {
    case site~shipping => (site, shipping)
  }

  val siteWithTax = TransactionLogSite.simple ~ TransactionLogTax.simple map {
    case site~tax => (site, tax)
  }

  val siteWithItem = 
    ItemName.simple ~ 
    TransactionLogSite.simple ~ 
    TransactionLogItem.simple ~
    (TransactionLogCoupon.simple ?) map {
    case name~site~item~coupon => (name, site, item, coupon)
  }

  def load(tranId: Long, localeInfo: LocaleInfo)(implicit conn: Connection): PersistedTransaction = {
    val header = TransactionLogHeader(tranId)

    val (tranSiteLog, siteLog) = SQL(
      """
      select * from transaction_site
      inner join site on site.site_id = transaction_site.site_id
      where transaction_site.transaction_id = {id}
      order by site.site_name
      """
    ).on(
      'id -> tranId
    ).as(
      (TransactionLogSite.simple ~ Site.simple) *
    ).map {
      e => (e._1 -> e._2)
    }.unzip

    val shippingLog = SQL(
      """
      select * from transaction_site
      inner join transaction_shipping
        on transaction_site.transaction_site_id = transaction_shipping.transaction_site_id
      where transaction_site.transaction_id = {id}
      order by transaction_site.transaction_site_id
      """
    ).on(
      'id -> tranId
    ).as(
      siteWithShipping *
    ).foldLeft(
      immutable.LongMap[List[TransactionLogShipping]]().withDefaultValue(List())
    ) { (map, e) =>
      val siteId = e._1.siteId
      map.updated(siteId, e._2 :: map(siteId))
    }.mapValues(_.reverse)

    val taxLog = SQL(
      """
      select * from transaction_site
      inner join transaction_tax
        on transaction_site.transaction_site_id = transaction_tax.transaction_site_id
      where transaction_site.transaction_id = {id}
      order by transaction_tax_id
      """
    ).on(
      'id -> tranId
    ).as(
      siteWithTax *
    ).foldLeft(
      immutable.LongMap[List[TransactionLogTax]]().withDefaultValue(List())
    ) { (map, e) =>
      val siteId = e._1.siteId
      map.updated(siteId, e._2 :: map(siteId))
    }.mapValues(_.reverse)

    val itemLog = SQL(
      """
      select * from transaction_site
      inner join transaction_item
        on transaction_site.transaction_site_id = transaction_item.transaction_site_id
      inner join item_name on transaction_item.item_id = item_name.item_id
      left join transaction_coupon c on c.transaction_item_id = transaction_item.transaction_item_id
      where transaction_site.transaction_id = {id}
      and item_name.locale_id = {locale}
      order by transaction_item.transaction_item_id
      """
    ).on(
      'id -> tranId,
      'locale -> localeInfo.id
    ).as(
      siteWithItem *
    ).foldLeft(
      immutable.LongMap[List[(ItemName, TransactionLogItem, Option[TransactionLogCoupon])]]().withDefaultValue(List())
    ) { (map, e) =>
      val siteId = e._2.siteId
      map.updated(siteId, ((e._1, e._3, e._4)) :: map(siteId))
    }.mapValues(_.reverse)

    val credit: Option[TransactionLogCreditTender] = 
      if (header.transactionType == TransactionType.PAYPAL)
        Some(TransactionLogCreditTender.byTransactionId(tranId))
      else
        None

    PersistedTransaction(
      header, tranSiteLog.map {e => (e.siteId -> e)}.toMap, siteLog, shippingLog, taxLog, itemLog, credit
    )
  }
}
