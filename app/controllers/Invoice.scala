package controllers

import play.api._
import db.DB
import mvc.Controller
import play.api.Play.current
import controllers.I18n.I18nAware
import models.{TransactionPersister, TransactionDetail, LocaleInfo, TransactionSummary}

object Invoice extends Controller with NeedLogin with HasLogger with I18nAware {
  def show(tranSiteId: Long) = isAuthenticated { implicit login => implicit request =>
    DB.withConnection { implicit conn =>
      val entry = TransactionSummary.get(login.siteUser.map(_.id.get), tranSiteId).get
      val persistedTran = (new TransactionPersister()).load(entry.transactionId, LocaleInfo.getDefault)
      Ok(
        views.html.showInvoice(
          entry,
          persistedTran,
          TransactionDetail.show(tranSiteId, LocaleInfo.byLang(lang), login.siteUser)
        )
      )
    }
  }
}
