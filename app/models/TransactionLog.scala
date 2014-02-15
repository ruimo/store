package models

import anorm._
import anorm.{NotAssigned, Pk}
import anorm.SqlParser
import play.api.Play.current
import scala.language.postfixOps
import collection.immutable.{LongMap, HashMap, IntMap}
import java.sql.Connection

case class TransactionLogHeader(
  id: Pk[Long] = NotAssigned,
  userId: Long,
  transactionTime: Long,
  currencyId: Long,
  // Item total and shipping total. Excluding outer tax, including inner tax.
  totalAmount: BigDecimal,
  // Outer tax.
  taxAmount: BigDecimal,
  transactionType: TransactionType
) extends NotNull

case class TransactionLogSite(
  id: Pk[Long] = NotAssigned,
  transactionId: Long,
  siteId: Long,
  // Item total and shipping total. Excluding outer tax, including inner tax.
  totalAmount: BigDecimal,
  // Outer tax.
  taxAmount: BigDecimal
) extends NotNull

case class TransactionLogShipping(
  id: Pk[Long] = NotAssigned,
  transactionSiteId: Long,
  amount: BigDecimal,
  addressId: Long,
  itemClass: Long,
  boxSize: Int,
  taxId: Long,
  boxCount: Int,
  boxName: String,
  shippingDate: Long
) extends NotNull

case class TransactionLogTax(
  id: Pk[Long] = NotAssigned,
  transactionSiteId: Long,
  taxId: Long,
  taxType: TaxType,
  rate: BigDecimal,
  targetAmount: BigDecimal,
  amount: BigDecimal
) extends NotNull

case class TransactionLogItem(
  id: Pk[Long] = NotAssigned,
  transactionSiteId: Long,
  itemId: Long,
  itemPriceHistoryId: Long,
  quantity: Long,
  amount: BigDecimal,
  costPrice: BigDecimal
) extends NotNull

case class ShippingInfo(
  transporterId: Long,
  slipCode: String
) extends NotNull

case class TransactionShipStatus(
  id: Pk[Long] = NotAssigned,
  transactionSiteId: Long,
  status: TransactionStatus,
  lastUpdate: Long,
  shippingInfo: Option[ShippingInfo],
  mailSent: Boolean
) extends NotNull

case class ShippingDateEntry(
  siteId: Long,
  shippingDate: Long
) extends NotNull

case class ShippingDate(
  tables: Map[Long, ShippingDateEntry] // Key is siteId
) extends NotNull {
  def bySite(site: Site): ShippingDateEntry = tables(site.id.get)
}

case class Transaction(
  userId: Long,
  currency: CurrencyInfo,
  itemTotal: ShoppingCartTotal,
  shippingAddress: Address,
  shippingTotal: ShippingTotal,
  shippingDate: ShippingDate,
  now: Long = System.currentTimeMillis
) {
  lazy val total = itemTotal.total + shippingTotal.boxTotal  // Including inner tax excluding outer tax.
  lazy val taxAmount: BigDecimal = {
    val sumByTaxId = (itemTotal.sumByTaxId.toList ++ shippingTotal.sumByTaxId.toList).foldLeft(
      LongMap[BigDecimal]().withDefaultValue(BigDecimal(0))
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
      new HashMap[Site, Transaction]()
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

object TransactionLogHeader {
  val simple = {
    SqlParser.get[Pk[Long]]("transaction_header.transaction_id") ~
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
      'transactionTime -> new java.sql.Date(now),
      'currencyId -> currencyId,
      'totalAmount -> totalAmount.bigDecimal,
      'taxAmount -> taxAmount.bigDecimal,
      'transactionType -> transactionType.ordinal
    ).executeUpdate()

    val id = SQL("select currval('transaction_header_seq')").as(SqlParser.scalar[Long].single)

    TransactionLogHeader(Id(id), userId, now, currencyId, totalAmount, taxAmount, transactionType)
  }

  def list(limit: Int = 20, offset: Int = 0)(implicit conn: Connection): Seq[TransactionLogHeader] = {
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
  }

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
    SqlParser.get[Pk[Long]]("transaction_site.transaction_site_id") ~
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

    TransactionLogSite(Id(id), transactionId, siteId, totalAmount, taxAmount)
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
    SqlParser.get[Pk[Long]]("transaction_shipping.transaction_shipping_id") ~
    SqlParser.get[Long]("transaction_shipping.transaction_site_id") ~
    SqlParser.get[java.math.BigDecimal]("transaction_shipping.amount") ~
    SqlParser.get[Long]("transaction_shipping.address_id") ~
    SqlParser.get[Long]("transaction_shipping.item_class") ~
    SqlParser.get[Int]("transaction_shipping.box_size") ~
    SqlParser.get[Long]("transaction_shipping.tax_id") ~
    SqlParser.get[Int]("transaction_shipping.box_count") ~
    SqlParser.get[String]("transaction_shipping.box_name") ~
    SqlParser.get[java.util.Date]("transaction_shipping.shipping_date") map {
      case id~transactionId~amount~addressId~itemClass~boxSize~taxId~boxCount~boxName~shippingDate =>
        TransactionLogShipping(id, transactionId, amount, addressId, itemClass, boxSize, taxId, boxCount, boxName, shippingDate.getTime)
    }
  }

  def createNew(
    transactionSiteId: Long, amount: BigDecimal, addressId: Long,
    itemClass: Long, boxSize: Int, taxId: Long, boxCount: Int, boxName: String, shippingDate: Long
  )(implicit conn: Connection): TransactionLogShipping = {
    SQL(
      """
      insert into transaction_shipping (
        transaction_shipping_id, transaction_site_id, amount, address_id,
        item_class, box_size, tax_id, box_count, box_name, shipping_date
      ) values (
        (select nextval('transaction_shipping_seq')),
        {transactionSiteId}, {amount}, {addressId},
        {itemClass}, {boxSize}, {taxId}, {boxCount}, {boxName}, {shippingDate}
      )
      """
    ).on(
      'transactionSiteId -> transactionSiteId,
      'amount -> amount.bigDecimal,
      'addressId -> addressId,
      'itemClass -> itemClass,
      'boxSize -> boxSize,
      'taxId -> taxId,
      'boxCount -> boxCount,
      'boxName -> boxName,
      'shippingDate -> new java.sql.Date(shippingDate)
    ).executeUpdate()

    val id = SQL("select currval('transaction_shipping_seq')").as(SqlParser.scalar[Long].single)

    TransactionLogShipping(Id(id), transactionSiteId, amount, addressId, itemClass, boxSize, taxId, boxCount, boxName, shippingDate)
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
}

object TransactionLogTax {
  val simple = {
    SqlParser.get[Pk[Long]]("transaction_tax.transaction_tax_id") ~
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

    TransactionLogTax(Id(id), transactionSiteId, taxId, taxType, rate, targetAmount, amount)
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
    SqlParser.get[Pk[Long]]("transaction_item.transaction_item_id") ~
    SqlParser.get[Long]("transaction_item.transaction_site_id") ~
    SqlParser.get[Long]("transaction_item.item_id") ~
    SqlParser.get[Long]("transaction_item.item_price_history_id") ~
    SqlParser.get[Int]("transaction_item.quantity") ~
    SqlParser.get[java.math.BigDecimal]("transaction_item.amount") ~
    SqlParser.get[java.math.BigDecimal]("transaction_item.cost_price") map {
      case id~tranSiteId~itemId~priceHistoryId~quantity~amount~costPrice =>
        TransactionLogItem(id, tranSiteId, itemId, priceHistoryId, quantity, amount, costPrice)
    }
  }

  def createNew(
    transactionSiteId: Long, itemId: Long, itemPriceHistoryId: Long, quantity: Long, amount: BigDecimal,
    costPrice: BigDecimal
  )(implicit conn: Connection): TransactionLogItem = {
    SQL(
      """
      insert into transaction_item (
        transaction_item_id, transaction_site_id, item_id, item_price_history_id, quantity, amount, cost_price
      ) values (
        (select nextval('transaction_item_seq')),
        {transactionSiteId}, {itemId}, {itemPriceHistoryId}, {quantity}, {amount}, {costPrice}
      )
      """
    ).on(
      'transactionSiteId -> transactionSiteId,
      'itemId -> itemId,
      'itemPriceHistoryId -> itemPriceHistoryId,
      'quantity -> quantity,
      'amount -> amount.bigDecimal,
      'costPrice -> costPrice.bigDecimal
    ).executeUpdate()

    val id = SQL("select currval('transaction_item_seq')").as(SqlParser.scalar[Long].single)

    TransactionLogItem(Id(id), transactionSiteId, itemId, itemPriceHistoryId, quantity, amount, costPrice)
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
}

case class PersistedTransaction(
  header: TransactionLogHeader,
  siteTable: Seq[Site],
  shippingTable: Map[Long, Seq[TransactionLogShipping]], // First key = siteId
  taxTable: Map[Long, Seq[TransactionLogTax]], // First key = siteId
  itemTable: Map[Long, Seq[(ItemName, TransactionLogItem)]] // First key = siteId
) extends NotNull {
  lazy val sites: Map[Long, Site] = siteTable.foldLeft(
    LongMap[Site]()
  ) {
    (map, e) => map.updated(e.id.get, e)
  }
  lazy val itemTotal: Map[Long, BigDecimal] = itemTable.foldLeft(
    LongMap[BigDecimal]()
  ) {
    (map, e) => map.updated(e._1, e._2.map(_._2.amount).foldLeft(BigDecimal(0))(_ + _))
  }
  lazy val itemGrandTotal: BigDecimal = itemTotal.values.fold(BigDecimal(0))(_ + _)
  lazy val itemQuantity: Map[Long, Long] = itemTable.foldLeft(
    LongMap[Long]()
  ) {
    (map, e) => map.updated(e._1, e._2.map(_._2.quantity).foldLeft(0L)(_ + _))
  }
  lazy val itemGrandQuantity: Long = itemQuantity.values.fold(0L)(_ + _)
  lazy val boxTotal: Map[Long, BigDecimal] = shippingTable.foldLeft(
    LongMap[BigDecimal]()
  ) {
    (map, e) => map.updated(e._1, e._2.map(_.amount).foldLeft(BigDecimal(0))(_ + _))
  }
  lazy val boxGrandTotal: BigDecimal = boxTotal.values.fold(BigDecimal(0))(_ + _)
  lazy val boxQuantity: Map[Long, Int] = shippingTable.foldLeft(
    LongMap[Int]()
  ) {
    (map, e) => map.updated(e._1, e._2.map(_.boxCount).foldLeft(0)(_ + _))
  }
  lazy val boxGrandQuantity: Int = boxQuantity.values.fold(0)(_ + _)
  lazy val outerTaxTotal: Map[Long, BigDecimal] = taxTable.foldLeft(
    LongMap[BigDecimal]()
  ) {
    (map, e) => map.updated(e._1, e._2.filter(_.taxType == TaxType.OUTER_TAX).map(_.amount).foldLeft(BigDecimal(0))(_ + _))
  }
  lazy val outerTaxGrandTotal: BigDecimal = outerTaxTotal.values.fold(BigDecimal(0))(_ + _)
}

case class TransactionSummaryEntry(
  transactionId: Long,
  transactionSiteId: Long,
  transactionTime: Long,
  totalAmount: BigDecimal,
  address: Address,
  siteId: Long,
  siteName: String,
  shippingFee: BigDecimal,
  status: TransactionStatus,
  statusLastUpdate: Long,
  shipStatus: Option[ShippingInfo],
  buyer: StoreUser,
  shippingDate: Long,
  mailSent: Boolean
) extends NotNull

object TransactionShipStatus {
  val simple = {
    SqlParser.get[Pk[Long]]("transaction_status.transaction_status_id") ~
    SqlParser.get[Long]("transaction_status.transaction_site_id") ~
    SqlParser.get[Int]("transaction_status.status") ~
    SqlParser.get[Option[Long]]("transaction_status.transporter_id") ~
    SqlParser.get[Option[String]]("transaction_status.slip_code") ~
    SqlParser.get[Boolean]("transaction_status.mail_sent") ~
    SqlParser.get[java.util.Date]("transaction_status.last_update") map {
      case id~tranSiteId~status~transporterId~slipCode~mailSent~lastUpdate =>
        if (transporterId.isDefined)
          TransactionShipStatus(
            id, tranSiteId, TransactionStatus.byIndex(status),
            lastUpdate.getTime, Some(ShippingInfo(transporterId.get, slipCode.get)), mailSent
          )
        else
          TransactionShipStatus(
            id, tranSiteId, TransactionStatus.byIndex(status), lastUpdate.getTime, None, mailSent
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
      'lastUpdate -> new java.sql.Date(lastUpdate)
    ).executeUpdate()

    val id = SQL("select currval('transaction_status_seq')").as(SqlParser.scalar[Long].single)

    TransactionShipStatus(Id(id), transactionSiteId, status, lastUpdate, shippingInfo, false)
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

object TransactionSummary {
  val parser = {
    SqlParser.get[Long]("transaction_id") ~
    SqlParser.get[Long]("transaction_site_id") ~
    SqlParser.get[java.util.Date]("transaction_time") ~
    SqlParser.get[java.math.BigDecimal]("total_amount") ~
    Address.simple ~
    SqlParser.get[Long]("site_id") ~
    SqlParser.get[String]("site_name") ~
    SqlParser.get[java.math.BigDecimal]("shipping") ~
    SqlParser.get[Int]("status") ~
    StoreUser.simple ~
    SqlParser.get[java.util.Date]("shipping_date") ~
    SqlParser.get[java.util.Date]("transaction_status.last_update") ~ 
    SqlParser.get[Option[Long]]("transaction_status.transporter_id") ~
    SqlParser.get[Option[String]]("transaction_status.slip_code") ~
    SqlParser.get[Boolean]("transaction_status.mail_sent") map {
      case id~tranSiteId~time~amount~address~siteId~siteName~shippingFee~status~user~shippingDate~lastUpdate~transporterId~slipCode~mailSent =>
        TransactionSummaryEntry(
          id, tranSiteId, time.getTime, amount, address, siteId, siteName,
          shippingFee, TransactionStatus.byIndex(status), lastUpdate.getTime,
          if (transporterId.isDefined) Some(ShippingInfo(transporterId.get, slipCode.get)) else None,
          user, shippingDate.getTime, mailSent
        )
    }
  }

  def baseSql(user: Option[SiteUser], additionalWhere: String = "", limit: Option[(Int, Int)]) =
    """
    select 
      transaction_id,
      base.transaction_site_id,
      transaction_time,
      total_amount,
      address.*,
      site.site_id,
      site.site_name,
      shipping,
      coalesce(transaction_status.status, 0) status,
      transaction_status.last_update,
      transaction_status.transporter_id,
      transaction_status.slip_code,
      transaction_status.mail_sent,
      store_user.*,
      base.shipping_date
    from (
      select            
        transaction_header.transaction_id,
        transaction_header.transaction_time,
        transaction_site.total_amount,
        transaction_site.site_id,
        transaction_site.transaction_site_id,
        transaction_header.store_user_id,
        max(transaction_shipping.shipping_date) shipping_date,
        min(transaction_shipping.address_id) address_id,
        sum(transaction_shipping.amount) shipping
      from transaction_header
      inner join transaction_site on
        transaction_header.transaction_id = transaction_site.transaction_id
      inner join transaction_shipping on
        transaction_shipping.transaction_site_id = transaction_site.transaction_site_id
    """ + (user match {
      case None => ""
      case Some(siteUser) => "where transaction_site.site_id = " + siteUser.siteId
    }) +
    """
      group by
        transaction_header.transaction_id,
        transaction_site.transaction_site_id
      order by transaction_header.transaction_time desc, transaction_site.site_id
    """ + (limit match {
      case None => ""
      case Some(l) => "limit {limit} offset {offset}"
    }) +
    """
    ) base
    inner join address on address.address_id = base.address_id
    inner join site on site.site_id = base.site_id
    inner join store_user on base.store_user_id = store_user.store_user_id
    left join transaction_status
      on transaction_status.transaction_site_id = base.transaction_site_id
    """ +
    additionalWhere +
    """
    order by base.transaction_id desc, base.site_id
    """

  def list(
    user: Option[SiteUser], limit: Int = 20, offset: Int = 0
  )(implicit conn: Connection): Seq[TransactionSummaryEntry] = {
    SQL(
      baseSql(user, "", Some(limit, offset))
    ).on(
      'limit -> limit,
      'offset -> offset
    ).as(
      parser *
    )
  }

  def get(user: Option[SiteUser], tranSiteId: Long)(implicit conn: Connection): Option[TransactionSummaryEntry] = {
    SQL(
      baseSql(user, "where base.transaction_site_id = {tranSiteId}", None)
    ).on(
      'tranSiteId -> tranSiteId
    ).as(
      parser.singleOpt
    )
  }
}

case class TransactionDetail(
  itemName: String,
  unitPrice: BigDecimal,
  costPrice: BigDecimal,
  quantity: Int,
  itemNumericMetadata: Map[ItemNumericMetadataType, ItemNumericMetadata],
  siteItemNumericMetadata: Map[SiteItemNumericMetadataType, SiteItemNumericMetadata],
  itemTextMetadata: Map[ItemTextMetadataType, ItemTextMetadata]
) extends NotNull {
  lazy val price = unitPrice * quantity
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
    tranSiteId: Long, locale: LocaleInfo, user: Option[SiteUser]
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
      val metadata = ItemNumericMetadata.allById(e._5)
      val textMetadata = ItemTextMetadata.allById(e._5)
      val siteMetadata = SiteItemNumericMetadata.all(e._6, e._5)
      TransactionDetail(e._1, e._2, e._3, e._4, metadata, siteMetadata, textMetadata)
    }
  }
}

class TransactionPersister {
  def persist(tran: Transaction)(implicit conn: Connection): Long = {
    val header = TransactionLogHeader.createNew(
      tran.userId, tran.currency.id,
      tran.total, tran.taxAmount,
      TransactionType.NORMAL,
      tran.now
    )

    tran.itemTotal.bySite.keys.foreach { site =>
      saveSiteTotal(header, site, tran)
    }

    header.id.get
  }

  def saveSiteTotal(
    header: TransactionLogHeader,
    site: Site,
    tran: Transaction
  )(implicit conn: Connection) {
    val siteLog = TransactionLogSite.createNew(
      header.id.get, site.id.get,
      tran.bySite(site).total,
      tran.bySite(site).taxAmount
    )

    TransactionShipStatus.createNew(siteLog.id.get, TransactionStatus.ORDERED, System.currentTimeMillis, None)
    saveShippingTotal(siteLog, tran.bySite(site))
    saveTax(siteLog, tran.bySite(site))
    saveItem(siteLog, tran.bySite(site))
  }

  def saveShippingTotal(
    siteLog: TransactionLogSite, tran: Transaction
  )(implicit conn: Connection) {
    tran.shippingTotal.table.foreach { e =>
      TransactionLogShipping.createNew(
        siteLog.id.get, e.boxTotal, tran.shippingAddress.id.get, e.itemClass, e.shippingBox.boxSize,
        e.boxTaxInfo.taxId, e.boxQuantity, e.shippingBox.boxName, tran.shippingDate.tables(e.site.id.get).shippingDate
      )
    }
  }

  def saveTax(
    siteLog: TransactionLogSite, tran: Transaction
  )(implicit conn: Connection) {
    val taxTable = tran.shippingTotal.taxHistoryById ++ tran.itemTotal.taxHistoryById

    taxTable.foreach { e =>
      val taxId = e._1
      val taxHistory = e._2

      val targetAmount = tran.itemTotal.sumByTaxId(taxId) + tran.shippingTotal.sumByTaxId(taxId)
      val taxAmount = taxHistory.taxAmount(targetAmount)

      TransactionLogTax.createNew(
        siteLog.id.get, taxHistory.id.get, taxId, taxHistory.taxType,
        taxHistory.rate, targetAmount, taxAmount
      )
    }
  }

  def saveItem(
    siteLog: TransactionLogSite, tran: Transaction
  )(implicit conn: Connection) {
    tran.itemTotal.table.foreach { e =>
      TransactionLogItem.createNew(
        siteLog.id.get, e.shoppingCartItem.itemId, e.itemPriceHistory.id.get,
        e.quantity, e.itemPrice, e.costPrice
      )
    }
  }

  val siteWithShipping = TransactionLogSite.simple ~ TransactionLogShipping.simple map {
    case site~shipping => (site, shipping)
  }

  val siteWithTax = TransactionLogSite.simple ~ TransactionLogTax.simple map {
    case site~tax => (site, tax)
  }

  val siteWithItem = ItemName.simple ~ TransactionLogSite.simple ~ TransactionLogItem.simple map {
    case name~site~item => (name, site, item)
  }

  def load(tranId: Long, localeInfo: LocaleInfo)(implicit conn: Connection): PersistedTransaction = {
    val header = TransactionLogHeader(tranId)

    val siteLog = SQL(
      """
      select * from transaction_site
      inner join site on site.site_id = transaction_site.site_id
      where transaction_site.transaction_id = {id}
      order by site.site_name
      """
    ).on(
      'id -> tranId
    ).as(
      Site.simple *
    )

    var shippingLog = SQL(
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
      LongMap[List[TransactionLogShipping]]().withDefaultValue(List())
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
      LongMap[List[TransactionLogTax]]().withDefaultValue(List())
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
      LongMap[List[(ItemName, TransactionLogItem)]]().withDefaultValue(List())
    ) { (map, e) =>
      val siteId = e._2.siteId
      map.updated(siteId, ((e._1, e._3)) :: map(siteId))
    }.mapValues(_.reverse)

    PersistedTransaction(
      header, siteLog, shippingLog, taxLog, itemLog
    )
  }
}
