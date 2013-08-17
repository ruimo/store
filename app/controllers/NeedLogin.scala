package controllers

import play.api.mvc.Security.Authenticated
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models.LoginUser

trait NeedLogin {
  val loginForm = Form(
    mapping(
      "userName" -> text.verifying(nonEmpty),
      "password" -> text.verifying(nonEmpty)
    )(LoginUser.apply)(LoginUser.unapply)
  )

  def username(request: RequestHeader) = request.session.get("loginUser")
  def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Admin.login)
  def isAuthenticated(f: => String => Request[AnyContent] => Result) = {
    Authenticated(username, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }
}
