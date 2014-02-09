package controllers

import play.api._
import mvc.Controller
import play.api.Play.current
import controllers.I18n.I18nAware

object OrderHistory extends Controller with NeedLogin with HasLogger with I18nAware {
  def index = isAuthenticated { implicit login => implicit request =>
    Ok(views.html.startOrderHistory())
  }
}
