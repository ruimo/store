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
  siteId: Long,
  userId: Long,
  transactionTime: Long,
  currencyId: Long,
  totalAmount: BigDecimal,
  taxAamount: BigDecimal,
  transactionType: TransactionType
)

case class TransactionShipping(
  id: Pk[Long] = NotAssigned,
  transactionId: Long,
  amount: BigDecimal,
  addressId: Long
)

case class TransactionTax(
  id: Pk[Long] = NotAssigned,
  transactionId: Long,
  taxHistoryId: Long,
  targetAmount: BigDecimal,
  amount: BigDecimal
)

case class TransactionItem(
  id: Pk[Long] = NotAssigned,
  transactionId: Long,
  itemPriceHistoryId: Long,
  transactionShippingId: Long,
  quantity: Long,
  amount: BigDecimal
)

object TransactionHeader {
  def createNew(
    siteId: Long, userId: Long, currencyId: Long,
    totalAmount: BigDecimal, taxAmount: BigDecimal,
    transactionType: TransactionType,
    now: Long = System.currentTimeMillis
  )(implicit conn: Connection): TransactionHeader = {
    SQL(
      """
      insert into transaction_header (
        transaction_id, site_id, store_user_id, transaction_time, 
        currency_id, total_amount, tax_amount, transactionType
      ) values (
        (select nextval('transaction_header_seq')),
        {siteId}, {userId}, {transactionTime}, {currencyId}}, 
        {totalAmount}, {taxAmount}, {transactionType}
      )
      """
    ).on(
      'siteId -> siteId,
      'userId -> userId,
      'transactionTime -> new java.sql.Date(now),
      'currencyId -> currencyId,
      'totalAmount -> totalAmount.bigDecimal,
      'taxAmount -> taxAmount.bigDecimal,
      'transactionType -> transactionType.ordinal
    ).executeUpdate()

    val id = SQL("select currval('transaction_header_seq')").as(SqlParser.scalar[Long].single)

    TransactionHeader(Id(id), siteId, userId, now, currencyId, totalAmount, taxAmount, transactionType)
  }
}

object TransactionShipping {
  def createNew(
    transactionId: Long, amount: BigDecimal, addressId: Long
  )(implicit conn: Connection): TransactionShipping = {
    SQL(
      """
      insert into transaction_shipping (
        transaction_shipping_id, transaction_id, amount, address_id
      ) values (
        (select nextval('transaction_shipping_seq')),
        {transactionId}, {amount}, {addressId}
      )
      """
    ).on(
      'transactionId -> transactionId,
      'amount -> amount,
      'addressId -> addressId
    ).executeUpdate()

    val id = SQL("select currval('transaction_shipping_seq')").as(SqlParser.scalar[Long].single)

    TransactionShipping(Id(id), transactionId, amount, addressId)
  }
}

object TransactionTax {
  def createNew(
    transactionId: Long, taxHistoryId: Long, targetAmount: BigDecimal, amount: BigDecimal
  )(implicit conn: Connection): TransactionTax = {
    SQL(
      """
      insert into transaction_tax (
        transaction_tax_id, transaction_id, tax_history_id, target_amount, amount
      ) values (
        (select nextval('transaction_tax_seq')),
        {transactionId}, {taxHistoryId}, {targetAmount}, {amount}
      )
      """
    ).on(
      'transactionId -> transactionId,
      'taxHistoryId -> taxHistoryId,
      'targetAmount -> targetAmount,
      'amount -> amount
    ).executeUpdate()

    val id = SQL("select currval('transaction_tax_seq')").as(SqlParser.scalar[Long].single)

    TransactionTax(Id(id), transactionId, taxHistoryId, targetAmount, amount)
  }
}

object TransactionItem {
  def createNew(
    transactionId: Long, itemPriceHistoryId: Long, transactionShippingId: Long, quantity: Long, amount: BigDecimal
  )(implicit conn: Connection): TransactionItem = {
    SQL(
      """
      insert into transaction_item (
        transaction_item_id, transaction_id, item_price_history_id, transaction_shipping_id,
        quantity, amount
      ) values (
        (select nextval('transaction_item_seq')),
        {transactionId}, {itemPriceHistoryId}, {transactionShippingId}, {quantity}, {amount}
      )
      """
    ).on(
      'transactionId -> transactionId,
      'itemPriceHistoryId -> itemPriceHistoryId,
      'transactionShippingId -> transactionShippingId,
      'quantity -> quantity,
      'amount -> amount
    ).executeUpdate()

    val id = SQL("select currval('transaction_item_seq')").as(SqlParser.scalar[Long].single)

    TransactionItem(Id(id), transactionId, itemPriceHistoryId, transactionShippingId, quantity, amount)
  }
}
