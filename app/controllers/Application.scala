package controllers

import play.api._
import play.api.mvc._
import controllers.I18n.I18nAware

object Application extends Controller with I18nAware with NeedLogin {
  def index = Action { implicit request =>
    Ok(views.html.index())
  }
}
