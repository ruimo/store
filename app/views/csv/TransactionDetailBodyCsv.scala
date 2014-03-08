package views.csv

import helpers.CsvWriter
import models.{TransactionLogShipping, LoginSession, TransactionSummaryEntry, TransactionDetail}
import play.api.i18n.{Lang, Messages}

class TransactionDetailBodyCsv(csvWriter: CsvWriter) {
  def print(
    tranId: Long, tranSummary: TransactionSummaryEntry, detail: TransactionDetail
  ) (
    implicit lang: Lang,
    loginSession: LoginSession
  ) {
    csvWriter.print(
      tranId.toString,
      Messages("csv.tran.detail.date.format").format(tranSummary.transactionTime),
      Messages("csv.tran.detail.shippingDate.format").format(tranSummary.shippingDate),
      Messages("csv.tran.detail.type.item"),
      detail.itemName,
      detail.quantity.toString,
      detail.unitPrice.toString,
      detail.costPrice.toString
    )
  }

  def printShipping(
    tranId: Long, tranSummary: TransactionSummaryEntry, detail: TransactionLogShipping
  ) (
    implicit lang: Lang,
    loginSession: LoginSession
  ) {
    csvWriter.print(
      tranId.toString,
      Messages("csv.tran.detail.date.format").format(tranSummary.transactionTime),
      Messages("csv.tran.detail.shippingDate.format").format(tranSummary.shippingDate),
      Messages("csv.tran.detail.type.shipping"),
      detail.boxName,
      detail.boxCount.toString,
      (detail.amount / detail.boxCount).toString,
      "0"
    )
  }
}
