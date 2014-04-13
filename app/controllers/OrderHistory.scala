package controllers

import play.api._
import db.DB
import mvc.Controller
import play.api.Play.current
import controllers.I18n.I18nAware
import models.{TransactionLogShipping, TransactionDetail, LocaleInfo, TransactionSummary, OrderBy}
import collection.immutable.{HashMap, LongMap}
import play.api.data.Form
import play.api.data.Forms._
import models.YearMonth

object OrderHistory extends Controller with NeedLogin with HasLogger with I18nAware {
  val billRequestForm = Form(
    mapping(
      "year" -> number(min = YearMonth.MinYear, max = YearMonth.MaxYear),
      "month" -> number(min = 1, max = 12)
    )(YearMonth.apply)(YearMonth.unapply)
  )

  def index() = isAuthenticated { implicit login => implicit request =>
    Ok(
      views.html.orderHistory(billRequestForm)
    )
  }

  def showOrderHistory(
    page: Int, pageSize: Int, orderBySpec: String
  ) = isAuthenticated { implicit login => implicit request =>
    DB.withConnection { implicit conn =>
      val pagedRecords = TransactionSummary.list(
        storeUser = Some(login.storeUser),
        page = page, pageSize = pageSize, orderBy = OrderBy(orderBySpec)
      )
      val detailByTranSiteId = pagedRecords.records.foldLeft(LongMap[Seq[TransactionDetail]]()) {
        (sum, e) =>
          val details = TransactionDetail.show(e.transactionSiteId, LocaleInfo.byLang(lang))
          sum.updated(e.transactionSiteId, details)
      }
      val boxBySiteAndItemSize = pagedRecords.records.foldLeft(
        LongMap[LongMap[TransactionLogShipping]]()
      ) {
        (sum, e) =>
          sum ++ TransactionLogShipping.listBySite(e.transactionSiteId).foldLeft(
            LongMap[LongMap[TransactionLogShipping]]().withDefaultValue(LongMap[TransactionLogShipping]())
          ) {
            (names, e2) =>
              names.updated(
                e.transactionSiteId,
                names(e.transactionSiteId).updated(e2.itemClass, e2)
              )
          }
      }
      Ok(
        views.html.showOrderHistory(pagedRecords, detailByTranSiteId, boxBySiteAndItemSize)
      )
    }
  }

  def showBill() = isAuthenticated { implicit login => implicit request =>
    billRequestForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(
          views.html.orderHistory(formWithErrors)
        )
      },
      yearMonth => {
        Ok("Ok " + yearMonth)
      }
    )
  }
}
