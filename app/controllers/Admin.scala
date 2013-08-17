package controllers

import play.api._
import play.api.mvc._
import play.filters.csrf.CSRF.Token._

object Admin extends Controller with NeedLogin {
  def login = Action { implicit request =>
    Ok(views.html.admin.login(loginForm))
  }

  def index = isAuthenticated {username => implicit request =>
    Ok(views.html.admin.index())
  }
}

