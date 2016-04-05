package models

sealed trait TransactionType {
  def typeCode: TransactionTypeCode
}

trait CreditTransactionType extends TransactionType

case object AccountingBillTransactionType extends TransactionType {
  val typeCode = TransactionTypeCode.ACCOUNTING_BILL
}

trait PaypalTransactionType extends CreditTransactionType {
  def status: PaypalStatus
}

case class PaypalExpressCheckoutTransactionType(
  status: PaypalStatus
) extends PaypalTransactionType {
  val typeCode = TransactionTypeCode.PAYPAL_EXPRESS_CHECKOUT
}

case class PaypalWebPaymentPlusTransactionType(
  status: PaypalStatus
) extends PaypalTransactionType {
  val typeCode = TransactionTypeCode.PAYPAL_WEB_PAYMENT_PLUS
}

