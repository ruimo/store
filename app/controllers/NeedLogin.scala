package controllers

import play.api.mvc.Security.Authenticated
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models.{StoreUser, FirstSetup, LoginUser}
import play.api.i18n.Messages

trait NeedLogin extends Controller {
  val LoginUserKey = "loginUser"

  case class LoginSession(userId: Long, expireTime: Long) {
    def withExpireTime(newExpireTime: Long) = LoginSession(userId, newExpireTime)
    def toSessionString = userId + ";" + expireTime
  }

  object LoginSession {
    def apply(sessionString: String): LoginSession = {
      val args = sessionString.split(';').map(_.toLong)
      LoginSession(args(0), args(1))
    }
  }

  val loginForm = Form(
    mapping(
      "userName" -> text.verifying(nonEmpty),
      "password" -> text.verifying(nonEmpty)
    )(LoginUser.apply)(LoginUser.unapply)
  )

  def loginSession(request: RequestHeader): Option[LoginSession] =
    request.session.get(LoginUserKey).map { sessionString: String => LoginSession(sessionString) }

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

  def startLogin = Action { implicit request =>
    Ok(views.html.admin.login(loginForm))
  }

  def login = Action { implicit request => {
    loginForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(views.html.admin.login(formWithErrors)),
      user => {
        Redirect(routes.Admin.index).flashing(
          "message" -> "Welcome"
        )
      }
    )
  }}
}
