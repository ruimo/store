package controllers

import play.api._
import db.DB
import play.api.mvc._
import controllers.I18n.I18nAware
import play.api.Play.current

object Application extends Controller with I18nAware with NeedLogin {
  def index = optIsAuthenticated { implicit optLogin => implicit request =>
    Ok(views.html.index())
  }
}
