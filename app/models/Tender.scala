package models;

trait Tender {
  def tenderType: TenderType
}

case class PayByAccountingBill(
  amount: BigDecimal
) extends Tender {
  def tenderType = TenderType.ACCOUNTING_BILL
}

case class Paypal(
  amount: BigDecimal
) extends Tender {
  def tenderType = TenderType.PAYPAL
}


