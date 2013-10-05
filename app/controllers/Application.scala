package controllers

import play.api._
import db.DB
import play.api.mvc._
import controllers.I18n.I18nAware
import play.api.Play.current

object Application extends Controller with I18nAware with NeedLogin {
  def index = Action { implicit request =>
    implicit val login = DB.withConnection { conn =>
      loginSession(request, conn)
    }
    Ok(views.html.index())
  }
}
