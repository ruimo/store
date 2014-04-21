package controllers

import play.api._
import db.DB
import mvc.Controller
import play.api.Play.current
import controllers.I18n.I18nAware
import models._
import collection.immutable.{HashMap, LongMap}
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
            storeUser = Some(login.storeUser), yearMonth = yearMonth
          )
          val siteTranByTranId = summaries.foldLeft(LongMap[PersistedTransaction]()) {
            (sum, e) =>
            val siteTran = (new TransactionPersister).load(e.transactionId, LocaleInfo.getDefault)
            sum.updated(e.transactionId, siteTran)
          }
          val detailByTranSiteId = summaries.foldLeft(LongMap[Seq[TransactionDetail]]()) {
            (sum, e) =>
            val details = TransactionDetail.show(e.transactionSiteId, LocaleInfo.byLang(lang))
            sum.updated(e.transactionSiteId, details)
          }
          val boxBySiteAndItemSize = summaries.foldLeft(
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

          Ok(views.html.showMonthlyOrderHistory(
            orderHistoryForm.fill(yearMonth),
            summaries, detailByTranSiteId, boxBySiteAndItemSize, siteTranByTranId
          ))
        }
      }
    )
  }
}
