package controllers

import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models._
import play.api.data.Form
import controllers.I18n.I18nAware
import play.api.mvc.Controller
import play.api.db.DB
import play.api.i18n.{Lang, Messages}
import play.api.Play.current
import helpers.{TokenGenerator, RandomTokenGenerator}

object ShoppingCart extends Controller with I18nAware with NeedLogin with HasLogger {
  def addToCartJson = isAuthenticatedJson { implicit login => implicit request =>
    Ok("")
  }
}
