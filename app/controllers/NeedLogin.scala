package controllers

import play.api.mvc.Security.Authenticated
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models.{StoreUser, FirstSetup, LoginUser}
import play.api.i18n.Messages
import play.api.templates.Html
import helpers.PasswordHash

trait NeedLogin extends Controller with HasLogger {
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
    val form = loginForm.bindFromRequest

    form.bindFromRequest.fold(
      onValidationErrorInLogin,
      user => tryLogin(user, form)
    )
  }}

  def onValidationErrorInLogin(form: Form[LoginUser])(implicit request: Request[AnyContent]) = {
    logger.error("Validation error in NeedLogin.login.")
    BadRequest(views.html.admin.login(form))
  }

  def tryLogin(user: LoginUser, form: Form[LoginUser])(implicit request: Request[AnyContent]) = {
    StoreUser.findByUserName(user.userName) match {
      case None => onLoginUserNotFound(form)
      case Some(rec) => {
        val passwordHash = PasswordHash.generate(user.password, rec.salt)
        if (passwordHash != rec.passwordHash) {
          logger.error("Password doesnot match. input: " + passwordHash + ", record: " + rec.passwordHash)
          BadRequest(views.html.admin.login(form.withGlobalError(Messages("cannotLogin"))))
        }
        else {
          Redirect(routes.Admin.index).flashing(
            "message" -> "Welcome"
          )
        }
      }
    }
  }

  def onLoginUserNotFound(form: Form[LoginUser])(implicit request: Request[AnyContent]) = {
    logger.error("User '" + form.data("userName") + "' not found.")
    BadRequest(views.html.admin.login(form.withGlobalError(Messages("cannotLogin"))))
  }
}
