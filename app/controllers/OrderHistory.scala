package controllers

import play.api._
import db.DB
import mvc.Controller
import play.api.Play.current
import controllers.I18n.I18nAware
import models._
import collection.immutable
import play.api.data.Form
import play.api.data.Forms._
import scala.Some

object OrderHistory extends Controller with NeedLogin with HasLogger with I18nAware {
  val orderHistoryForm = Form(
    mapping(
      "year" -> number(min = YearMonth.MinYear, max = YearMonth.MaxYear),
      "month" -> number(min = 1, max = 12)
    )(YearMonth.apply)(YearMonth.unapply)
  )

  def index() = isAuthenticated { implicit login => implicit request =>
    Ok(
      views.html.orderHistory(orderHistoryForm)
    )
  }

  def showOrderHistory(
    page: Int, pageSize: Int, orderBySpec: String, tranId: Option[Long]
  ) = isAuthenticated { implicit login => implicit request =>
    DB.withConnection { implicit conn =>
      val pagedRecords = TransactionSummary.list(
        storeUserId = Some(login.storeUser.id.get), tranId = tranId,
        page = page, pageSize = pageSize, orderBy = OrderBy(orderBySpec)
      )
      val siteTranByTranId: immutable.LongMap[PersistedTransaction] =
        AccountingBill.getSiteTranByTranId(pagedRecords.records, lang)

      Ok(
        views.html.showOrderHistory(
          pagedRecords,
          AccountingBill.getDetailByTranSiteId(pagedRecords.records, lang),
          AccountingBill.getBoxBySiteAndItemSize(pagedRecords.records),
          siteTranByTranId, 
          AccountingBill.getAddressTable(siteTranByTranId),
          tranId
        )
      )
    }
  }

  def showMonthly() = isAuthenticated { implicit login => implicit request =>
    orderHistoryForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(
          views.html.orderHistory(formWithErrors)
        )
      },
      yearMonth => {
        DB.withConnection { implicit conn =>
          val summaries = TransactionSummary.listByPeriod(
            storeUserId = Some(login.storeUser.id.get), yearMonth = yearMonth
          )
          val siteTranByTranId = AccountingBill.getSiteTranByTranId(summaries, lang)
          Ok(views.html.showMonthlyOrderHistory(
            orderHistoryForm.fill(yearMonth),
            summaries,
            AccountingBill.getDetailByTranSiteId(summaries, lang),
            AccountingBill.getBoxBySiteAndItemSize(summaries),
            siteTranByTranId,
            AccountingBill.getAddressTable(siteTranByTranId)
          ))
        }
      }
    )
  }
}
