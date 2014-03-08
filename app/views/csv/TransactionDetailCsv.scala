package views.csv

import models.LoginSession
import play.api.i18n.{Lang, Messages}
import helpers.Csv

object TransactionDetailCsv {
  def instance(
    implicit lang: Lang,
    loginSession: LoginSession
  ) = new Csv(
    Messages("csv.tran.detail.id"),
    Messages("csv.tran.detail.date"),
    Messages("csv.tran.detail.shippingDate"),
    Messages("csv.tran.detail.type"),
    Messages("csv.tran.detail.itemName"),
    Messages("csv.tran.detail.quantity"),
    Messages("csv.tran.detail.amount"),
    Messages("csv.tran.detail.costPrice")
  )
}
