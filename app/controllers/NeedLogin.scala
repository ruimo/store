package controllers

import play.api.mvc.Security.Authenticated
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models.{StoreUser, FirstSetup, LoginUser}

trait NeedLogin {
  val LoginUserKey = "loginUser"

  case class LoginSession(userId: Long, expireTime: Long) {
    def withExpireTime(newExpireTime: Long) = LoginSession(userId, newExpireTime)
  }

  val firstSetupForm = Form(
    mapping(
      "userName" -> text.verifying(nonEmpty),
      "firstName" -> text.verifying(nonEmpty),
      "lastName" -> text.verifying(nonEmpty),
      "email" -> email.verifying(nonEmpty),
      "password" -> text.verifying(nonEmpty),
      "passwordConfirm" -> text.verifying(nonEmpty)
    )(FirstSetup.apply)(FirstSetup.unapply)
  )

  val loginForm = Form(
    mapping(
      "userName" -> text.verifying(nonEmpty),
      "password" -> text.verifying(nonEmpty)
    )(LoginUser.apply)(LoginUser.unapply)
  )

  def loginSession(request: RequestHeader): Option[LoginSession] =
    request.session.get(LoginUserKey).map { sessionString => {
      val parsed = sessionString.split(';').map(_.toLong)
      LoginSession(parsed(0), parsed(1))
    }}

  def onUnauthorized(request: RequestHeader) = StoreUser.count match {
    case 0 => 
      Results.Redirect(routes.Admin.startFirstSetup)
    case _ =>
      Results.Redirect(routes.Admin.startLogin)
  }

  def isAuthenticated(f: => LoginSession => Request[AnyContent] => Result) = {
    Authenticated(loginSession, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }
}
