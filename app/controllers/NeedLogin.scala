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
import play.api.db.DB
import play.api.Play.current

trait NeedLogin extends Controller with HasLogger {
  val userNameConstraint = List(minLength(6), maxLength(24))
  val passwordConstraint = List(minLength(6), maxLength(24))
  val firstNameConstraint = List(nonEmpty, maxLength(32))
  val lastNameConstraint = List(nonEmpty, maxLength(32))
  val emailConstraint = List(maxLength(255))

  val LoginUserKey = "loginUser"
  val SessionTimeout = 5 * 60 * 1000

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
      "password" -> text.verifying(nonEmpty),
      "uri" -> text
    )(LoginUser.apply)(LoginUser.unapply)
  )

  def loginSession(request: RequestHeader): Option[LoginSession] = loginSessionWithTime(request, System.currentTimeMillis)

  def loginSessionWithTime(request: RequestHeader, now: Long): Option[LoginSession] =
    request.session.get(LoginUserKey).flatMap {
      sessionString =>
        val s = LoginSession(sessionString)
        if (s.expireTime < now) None else Some(s)
    }

  def onUnauthorized(request: RequestHeader) = DB.withConnection { implicit conn => {
    StoreUser.count match {
      case 0 =>  {
        logger.info("User table empty. Go to first setup page.")
        Results.Redirect(routes.Admin.startFirstSetup)
      }
      case _ => {
        logger.info("User table is not empty. Go to login page.")
        Results.Redirect(routes.Admin.startLogin(request.uri))
      }
    }
  }}

  def isAuthenticated(f: => LoginSession => Request[AnyContent] => Result) = {
    Authenticated(loginSession, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }

  def startLogin(uriOnLoginSuccess: String) = Action { implicit request =>
    Ok(views.html.admin.login(loginForm, uriOnLoginSuccess))
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
    BadRequest(views.html.admin.login(form, form("uri").value.get))
  }

  def tryLogin(
    user: LoginUser, form: Form[LoginUser]
  )(implicit request: Request[AnyContent]) = DB.withConnection { implicit conn => {
    StoreUser.findByUserName(user.userName) match {
      case None => onLoginUserNotFound(form)
      case Some(rec) => {
        if (rec.passwordMatch(user.password)) {
          Redirect(user.uri).flashing(
            "message" -> "Welcome"
          ).withSession {
            (LoginUserKey, LoginSession(rec.id.get, System.currentTimeMillis + SessionTimeout).toSessionString)
          }
        }
        else {
          logger.error("Password doesnot match.")
          BadRequest(views.html.admin.login(
            form.withGlobalError(Messages("cannotLogin")),
            form("uri").value.get
          ))
        }
      }
    }
  }}

  def onLoginUserNotFound(form: Form[LoginUser])(implicit request: Request[AnyContent]) = {
    logger.error("User '" + form.data("userName") + "' not found.")
    BadRequest(views.html.admin.login(
      form.withGlobalError(Messages("cannotLogin")),
      form("uri").value.get
    ))
  }
}
