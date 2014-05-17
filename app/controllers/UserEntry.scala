package controllers

import play.api.i18n.{Lang, Messages}
import play.api.Play.current
import controllers.I18n.I18nAware
import play.api.mvc.{Action, Controller, RequestHeader}
import play.api.db.DB

object UserEntry extends Controller with HasLogger with I18nAware with NeedLogin {
  def index() = Action { implicit request => DB.withConnection { implicit conn => {
    implicit val login = loginSession(request, conn)
    Ok(views.html.userEntry())
  }}}
}
