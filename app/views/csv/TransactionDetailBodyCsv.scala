package views.csv

import helpers.CsvWriter
import models.{TransactionSummaryEntry, TransactionDetail}
import play.api.i18n.{Lang, Messages}

class TransactionDetailBodyCsv(csvWriter: CsvWriter) {
  def print(
    tranId: Long, tranSummary: TransactionSummaryEntry, detail: TransactionDetail
  ) (
    implicit lang: Lang
  ) {
    csvWriter.print(
      tranId.toString,
      Messages("csv.tran.detail.date.format").format(tranSummary.transactionTime),
      Messages("csv.tran.detail.shippingDate.format").format(tranSummary.shippingDate),
      detail.itemName,
      detail.quantity.toString,
      detail.unitPrice.toString,
      detail.costPrice.toString
    )
  }
}
