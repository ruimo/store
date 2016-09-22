package controllers

import play.api.data.validation.Constraints._
import play.api.data.Form
import controllers.I18n.I18nAware
import play.api.mvc.Controller
import play.api.db.DB
import play.api.i18n.Messages
import play.api.Play.current
import play.api.i18n.Lang

object ShowNews extends Controller with I18nAware with NeedLogin with HasLogger {
  def index = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      Ok("")
    }
  }
}
