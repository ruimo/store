package controllers

import play.api._
import db.DB
import mvc.Controller
import play.api.Play.current
import controllers.I18n.I18nAware
import models.TransactionSummary

object Invoice extends Controller with NeedLogin with HasLogger with I18nAware {
  def show(tranSiteId: Long) = isAuthenticated { implicit login => implicit request =>
    DB.withConnection { implicit conn =>
      val entry = TransactionSummary.get(login.siteUser, tranSiteId).get
      Ok(views.html.showInvoice())
    }
  }
}
