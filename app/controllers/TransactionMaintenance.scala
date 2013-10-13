package controllers

import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models._
import play.api.data.Form
import controllers.I18n.I18nAware
import play.api.mvc.Controller
import play.api.db.DB
import play.api.i18n.Messages
import play.api.Play.current

object TransactionMaintenance extends Controller with I18nAware with NeedLogin with HasLogger {
  def index = isAuthenticated { implicit login => forAdmin { implicit request =>
    DB.withConnection { implicit conn =>
      Ok(
        views.html.admin.transactionMaintenance(
          TransactionSummary.list(
            if (login.isAdmin) None else login.siteUser
          )
        )
      )
    }
  }}
}
