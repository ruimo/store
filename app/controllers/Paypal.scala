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

object Paypal extends Controller with NeedLogin with HasLogger with I18nAware {
  def onSuccess(transactionId: Long, token: Long) = NeedAuthenticated { implicit request =>
    implicit val login = request.user

    Ok(
      views.html.paypalSuccess()
    )
  }
}
