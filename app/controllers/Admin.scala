package controllers

import play.api._
import play.api.mvc._
import play.filters.csrf.CSRF.Token._

object Admin extends Controller with NeedLogin {
  def startFirstSetup = Action { implicit request =>
    Ok(views.html.admin.firstSetup(firstSetupForm))
  }

  def firstSetup = Action { implicit request => {
    firstSetupForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(views.html.admin.firstSetup(formWithErrors)),
      firstSetup => {
        Redirect(routes.Admin.index).flashing("message" -> "Welcome")
      }
    )
  }}

  def startLogin = Action { implicit request =>
    Ok(views.html.admin.login(loginForm))
  }

  def login = Action { implicit request => {
    loginForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(views.html.admin.login(formWithErrors)),
      user => {
        Redirect(routes.Admin.index).flashing("message" -> "Welcome")
      }
    )
  }}

  def index = isAuthenticated { loginSession => implicit request =>
    Ok(views.html.admin.index())
  }
}

