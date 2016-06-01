package controllers

import play.api.i18n.Lang
import play.api.mvc.Controller
import play.api.Play.current
import collection.immutable
import controllers.I18n.I18nAware

object MyPage extends Controller with NeedLogin with HasLogger with I18nAware {
  def index() = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    if (login.isAnonymousBuyer)
      Redirect(routes.Application.index)
    else
      Ok(views.html.myPage())
  }
}


