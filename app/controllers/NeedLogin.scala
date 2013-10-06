package controllers

import play.api.mvc.Security.Authenticated
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models.{LoginSession, StoreUser, FirstSetup, LoginUser}
import play.api.i18n.Messages
import play.api.templates.Html
import helpers.PasswordHash
import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

trait NeedLogin extends Controller with HasLogger {
  val userNameConstraint = List(minLength(6), maxLength(24))
  val passwordConstraint = List(minLength(6), maxLength(24))
  val firstNameConstraint = List(nonEmpty, maxLength(32))
  val lastNameConstraint = List(nonEmpty, maxLength(32))
  val emailConstraint = List(maxLength(255))

  val LoginUserKey = "loginUser"
  val SessionTimeout = 5 * 60 * 1000

  val loginForm = Form(
    mapping(
      "userName" -> text.verifying(nonEmpty),
      "password" -> text.verifying(nonEmpty),
      "uri" -> text
    )(LoginUser.apply)(LoginUser.unapply)
  )

  def loginSession(implicit request: RequestHeader, conn: Connection):
    Option[LoginSession] = loginSessionWithTime(request, System.currentTimeMillis)

  def retrieveLoginSession(request: RequestHeader) = DB.withConnection{conn =>
    loginSession(request, conn)
  }

  def loginSessionWithTime(
    request: RequestHeader, now: Long
  )(implicit conn: Connection): Option[LoginSession] =
    request.session.get(LoginUserKey).flatMap {
      sessionString =>
        val s = LoginSession(sessionString)
        if (s.expireTime < now) None else Some(s)
    }

  def onUnauthorized(request: RequestHeader) = DB.withConnection { implicit conn => {
    StoreUser.count match {
      case 0 =>  {
        logger.info("User table empty. Go to first setup page.")
        Redirect(routes.Admin.startFirstSetup)
      }
      case _ => {
        logger.info("User table is not empty. Go to login page.")
        Redirect(routes.Admin.startLogin(request.uri))
      }
    }
  }}

  def isAuthenticated(f: => LoginSession => Request[AnyContent] => Result) = {
    Authenticated(retrieveLoginSession, onUnauthorized) { user =>
      Action(request => f(user)(request).withSession(
        request.session + (LoginUserKey -> user.withExpireTime(System.currentTimeMillis + SessionTimeout).toSessionString)
      ))
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

  def logoff(uriOnLogoffSuccess: String) = Action { implicit request =>
    Redirect(uriOnLogoffSuccess).withSession(session - LoginUserKey)
  }

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
            (LoginUserKey, LoginSession.serialize(rec.id.get, System.currentTimeMillis + SessionTimeout))
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

  def forAdmin[T](
    f: Request[T] => Result
  )(implicit login: LoginSession): Request[T] => Result = { request =>
    if (login.isAdmin) f(request)
    else Redirect(routes.Application.index)
  }

  def forSuperUser(
    f: Request[AnyContent] => Result
  )(implicit login: LoginSession): Request[AnyContent] => Result = { request =>
    if (login.isSuperUser) f(request)
    else Redirect(routes.Application.index)
  }
}
