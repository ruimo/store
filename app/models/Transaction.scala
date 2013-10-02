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

case class TransactionHeader(
  id: Pk[Long] = NotAssigned,
  userId: Long,
  transactionTime: Long,
  currencyId: Long,
  totalAmount: BigDecimal,
  taxAamount: BigDecimal,
  transactionType: TransactionType
) extends NotNull

case class TransactionSite(
  id: Pk[Long] = NotAssigned,
  transactionId: Long,
  siteId: Long,
  totalAmount: BigDecimal,
  taxAmount: BigDecimal
)

case class TransactionShipping(
  id: Pk[Long] = NotAssigned,
  transactionSiteId: Long,
  amount: BigDecimal,
  addressId: Long
) extends NotNull

case class TransactionTax(
  id: Pk[Long] = NotAssigned,
  transactionSiteId: Long,
  taxId: Long,
  taxType: TaxType,
  rate: BigDecimal,
  targetAmount: BigDecimal,
  amount: BigDecimal
) extends NotNull

case class TransactionItem(
  id: Pk[Long] = NotAssigned,
  transactionSiteId: Long,
  itemPriceHistoryId: Long,
  transactionShippingId: Long,
  quantity: Long,
  amount: BigDecimal
) extends NotNull

object Transaction {
  def save(
    total: ShoppingCartTotal, address: Address, feeTotal: ShippingTotal, currency: CurrencyInfo, userId: Long
  )(implicit conn: Connection) {
    total.sites.foreach { site =>
      val header = TransactionHeader.createNew(
        userId, currency.id, total.total + feeTotal.boxTotal, total.taxAmount, TransactionType.NORMAL
      )
    }
  }
}

object TransactionHeader {
  val simple = {
    SqlParser.get[Pk[Long]]("transaction_header.transaction_id") ~
    SqlParser.get[Long]("transaction_header.store_user_id") ~
    SqlParser.get[java.util.Date]("transaction_header.transaction_time") ~
    SqlParser.get[Long]("transaction_header.currency_id") ~
    SqlParser.get[java.math.BigDecimal]("transaction_header.total_amount") ~
    SqlParser.get[java.math.BigDecimal]("transaction_header.tax_amount") ~
    SqlParser.get[Int]("transaction_header.transaction_type") map {
      case transactionId~userId~transactionTime~currencyId~totalAmount~taxAmount~transactionType =>
        TransactionHeader(transactionId, userId, transactionTime.getTime, currencyId, totalAmount,
                          taxAmount, TransactionType.byIndex(transactionType))
    }
  }

  def createNew(
    userId: Long, currencyId: Long,
    totalAmount: BigDecimal, taxAmount: BigDecimal,
    transactionType: TransactionType,
    now: Long = System.currentTimeMillis
  )(implicit conn: Connection): TransactionHeader = {
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

    TransactionHeader(Id(id), userId, now, currencyId, totalAmount, taxAmount, transactionType)
  }

  def list(limit: Int = 20, offset: Int = 0)(implicit conn: Connection): Seq[TransactionHeader] = {
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
}

object TransactionSite {
  val simple = {
    SqlParser.get[Pk[Long]]("transaction_site.transaction_site_id") ~
    SqlParser.get[Long]("transaction_site.transaction_id") ~
    SqlParser.get[Long]("transaction_site.site_id") ~
    SqlParser.get[java.math.BigDecimal]("transaction_site.total_amount") ~
    SqlParser.get[java.math.BigDecimal]("transaction_site.tax_amaount") map {
      case id~transactionId~siteId~totalAmount~taxAmount =>
        TransactionSite(id, transactionId, siteId, totalAmount, taxAmount)
    }
  }

  def createNew(
    transactionId: Long, siteId: Long, totalAmount: BigDecimal, taxAmount: BigDecimal
  )(implicit conn: Connection): TransactionSite = {
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

    TransactionSite(Id(id), transactionId, siteId, totalAmount, taxAmount)
  }
}

object TransactionShipping {
  val simple = {
    SqlParser.get[Pk[Long]]("transaction_shipping.transaction_shipping_id") ~
    SqlParser.get[Long]("transaction_shipping.transaction_site_id") ~
    SqlParser.get[java.math.BigDecimal]("transaction_shipping.amount") ~
    SqlParser.get[Long]("transaction_shipping.address_id") map {
      case id~transactionId~amount~addressId =>
        TransactionShipping(id, transactionId, amount, addressId)
    }
  }

  def createNew(
    transactionSiteId: Long, amount: BigDecimal, addressId: Long
  )(implicit conn: Connection): TransactionShipping = {
    SQL(
      """
      insert into transaction_shipping (
        transaction_shipping_id, transaction_site_id, amount, address_id
      ) values (
        (select nextval('transaction_shipping_seq')),
        {transactionSiteId}, {amount}, {addressId}
      )
      """
    ).on(
      'transactionSiteId -> transactionSiteId,
      'amount -> amount.bigDecimal,
      'addressId -> addressId
    ).executeUpdate()

    val id = SQL("select currval('transaction_shipping_seq')").as(SqlParser.scalar[Long].single)

    TransactionShipping(Id(id), transactionSiteId, amount, addressId)
  }

  def list(limit: Int = 20, offset: Int = 0)(implicit conn: Connection): Seq[TransactionShipping] =
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

object TransactionTax {
  val simple = {
    SqlParser.get[Pk[Long]]("transaction_tax.transaction_tax_id") ~
    SqlParser.get[Long]("transaction_tax.transaction_site_id") ~
    SqlParser.get[Long]("transaction_tax.tax_id") ~
    SqlParser.get[Int]("transaction_tax.tax_type") ~
    SqlParser.get[java.math.BigDecimal]("transaction_tax.rate") ~
    SqlParser.get[java.math.BigDecimal]("transaction_tax.target_amount") ~
    SqlParser.get[java.math.BigDecimal]("transaction_tax.amount") map {
      case id~transactionId~taxId~taxType~rate~targetAmount~amount =>
        TransactionTax(id, transactionId, taxId, TaxType.byIndex(taxType), rate, targetAmount, amount)
    }
  }

  def createNew(
    transactionSiteId: Long, taxHistoryId: Long, taxId: Long, taxType: TaxType,
    rate: BigDecimal, targetAmount: BigDecimal, amount: BigDecimal
  )(implicit conn: Connection): TransactionTax = {
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

    TransactionTax(Id(id), transactionSiteId, taxId, taxType, rate, targetAmount, amount)
  }

  def list(limit: Int = 20, offset: Int = 0)(implicit conn: Connection): Seq[TransactionTax] =
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

object TransactionItem {
  val simple = {
    SqlParser.get[Pk[Long]]("transaction_item.transaction_item_id") ~
    SqlParser.get[Long]("transaction_item.transaction_site_id") ~
    SqlParser.get[Long]("transaction_item.item_price_history_id") ~
    SqlParser.get[Long]("transaction_item.transaction_shipping_id") ~
    SqlParser.get[Int]("transaction_item.quantity") ~
    SqlParser.get[java.math.BigDecimal]("transaction_item.amount") map {
      case id~tranId~priceHistoryId~shipId~quantity~amount =>
        TransactionItem(id, tranId, priceHistoryId, shipId, quantity, amount)
    }
  }

  def createNew(
    transactionSiteId: Long, itemPriceHistoryId: Long, transactionShippingId: Long, quantity: Long, amount: BigDecimal
  )(implicit conn: Connection): TransactionItem = {
    SQL(
      """
      insert into transaction_item (
        transaction_item_id, transaction_site_id, item_price_history_id, transaction_shipping_id,
        quantity, amount
      ) values (
        (select nextval('transaction_item_seq')),
        {transactionSiteId}, {itemPriceHistoryId}, {transactionShippingId}, {quantity}, {amount}
      )
      """
    ).on(
      'transactionSiteId -> transactionSiteId,
      'itemPriceHistoryId -> itemPriceHistoryId,
      'transactionShippingId -> transactionShippingId,
      'quantity -> quantity,
      'amount -> amount.bigDecimal
    ).executeUpdate()

    val id = SQL("select currval('transaction_item_seq')").as(SqlParser.scalar[Long].single)

    TransactionItem(Id(id), transactionSiteId, itemPriceHistoryId, transactionShippingId, quantity, amount)
  }

  def list(limit: Int = 20, offset: Int = 0)(implicit conn: Connection): Seq[TransactionItem] =
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
