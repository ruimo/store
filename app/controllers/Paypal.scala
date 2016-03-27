package controllers

import scala.collection.immutable
import helpers.Cache
import play.api.i18n.{Lang, Messages}
import play.api.Play.current
import play.api.data.Forms._
import controllers.I18n.I18nAware
import models._
import play.api.data.Form
import play.api.db.DB
import collection.immutable.LongMap
import play.api.mvc.{Controller, RequestHeader}
import java.sql.Connection
import play.api.mvc._

object Paypal extends Controller with NeedLogin with HasLogger with I18nAware {
  def onSuccess(transactionId: Long, token: Long) = NeedAuthenticated { implicit request =>
    implicit val login = request.user

    DB.withConnection { implicit conn =>
      if (TransactionLogPaypalStatus.onSuccess(transactionId, token) == 0) {
        Redirect(routes.Application.index())
      }
      else {
        Ok(views.html.paypalSuccess())
      }
    }
  }

  def onCancel(transactionId: Long, token: Long) = NeedAuthenticated { implicit request =>
    implicit val login = request.user

    DB.withConnection { implicit conn =>
      if (TransactionLogPaypalStatus.onCancel(transactionId, token) == 0) {
        Redirect(routes.Application.index())
      }
      else {
        Ok(views.html.cancelPaypal())
      }
    }
  }

  def fakePaypal(cmd: String, token: String) = Action { implicit request =>
    Ok(
      views.html.fakePaypal(cmd, token)
    )
  }
}
