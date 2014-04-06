package controllers

import play.api._
import db.DB
import mvc.Controller
import play.api.Play.current
import controllers.I18n.I18nAware
import models.{TransactionLogShipping, TransactionDetail, LocaleInfo, TransactionSummary}
import collection.immutable.{HashMap, LongMap}

object OrderHistory extends Controller with NeedLogin with HasLogger with I18nAware {
  def index(page: Int, pageSize: Int, orderBySpec: String) = isAuthenticated { implicit login => implicit request =>
    DB.withConnection { implicit conn =>
      val pagedRecords = TransactionSummary.list(storeUser = Some(login.storeUser))
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
        views.html.startOrderHistory(pagedRecords, detailByTranSiteId, boxBySiteAndItemSize)
      )
    }
  }
}
