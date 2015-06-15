package controllers

import play.api.mvc.Security.AuthenticatedBuilder
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
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
import play.api.libs.json.Json

trait NeedLogin extends Controller with HasLogger {
  import NeedLogin._

  val LoginUserKey = "loginUser"
  lazy val SessionTimeout = loginTimeoutInMinute * 60 * 1000

  val loginForm = Form(
    mapping(
      "companyId" -> optional(text),
      "userName" -> text.verifying(nonEmpty),
      "password" -> text.verifying(nonEmpty),
      "uri" -> text
    )(LoginUser.apply)(LoginUser.unapply)
  )

  def loginSession(implicit request: RequestHeader, conn: Connection): Option[LoginSession] =
    loginSessionWithTime(request, System.currentTimeMillis)

  def retrieveLoginSession(request: RequestHeader): Option[LoginSession] = DB.withConnection { conn =>
    loginSession(request, conn)
  }

  def loginSessionWithTime(
    request: RequestHeader, now: Long
  )(implicit conn: Connection): Option[LoginSession] = {
    request.session.get(LoginUserKey).flatMap {
      sessionString =>
        val s = LoginSession(sessionString)
        if (s.expireTime < now) None else Some(s)
    }
  }

  def onUnauthorized(request: RequestHeader): Result = DB.withConnection { implicit conn => {
    StoreUser.count match {
      case 0 =>  {
        logger.info("User table empty. Go to first setup page.")
        Redirect(routes.Admin.startFirstSetup)
      }
      case _ => {
        logger.info("User table is not empty. Go to login page.")
        Redirect(
          if (request.method.equalsIgnoreCase("get"))
            routes.Admin.startLogin(request.uri)
          else
            routes.Application.index
        )
      }
    }
  }}

  def onUnauthorizedJson(request: RequestHeader): Result = DB.withConnection { implicit conn => {
    StoreUser.count match {
      case 0 =>  {
        logger.info("Json: User table empty. Go to first setup page.")
        Unauthorized(Json.toJson(Map("status" -> "Redirect", "url" -> routes.Admin.startFirstSetup().url)))
      }
      case _ => {
        logger.info("Json: User table is not empty. Go to login page.")
        val urlAfterLogin: String = 
          request.getQueryString("urlAfterLogin").getOrElse(routes.Application.index.url)

        Unauthorized(
          Json.toJson(
            Map(
              "status" -> "Redirect",
              "url" -> routes.Admin.startLogin(urlAfterLogin).url
            )
          )
        )
      }
    }
  }}

  def optIsAuthenticated(f: => Option[LoginSession] => Request[AnyContent] => Result) = 
    if (needAuthenticationEntirely) {
      Authenticated(retrieveLoginSession, onUnauthorized) { user =>
        Action(request => f(Some(user))(request).withSession(
          request.session + (LoginUserKey -> user.withExpireTime(System.currentTimeMillis + SessionTimeout).toSessionString)
        ))
      }
    }
    else {
      Action(request => {
        val optLogin: Option[LoginSession] = DB.withConnection { conn => loginSession(request, conn) }
        val result = f(optLogin)(request)
        optLogin match {
          case Some(login) =>
            result.withSession(
              request.session + (LoginUserKey -> login.withExpireTime(System.currentTimeMillis + SessionTimeout).toSessionString)
            )
          case None => result
        }
      })
    }

  object NeedAuthenticated extends AuthenticatedBuilder(req => retrieveLoginSession(req), onUnauthorized)
  def assumeSuperUser(login: LoginSession)(result: => Result): Result = {
    if (login.isSuperUser) result
    else Redirect(routes.Application.index)
  }
  def assumeAdmin(login: LoginSession)(result: => Result): Result = {
    if (login.isAdmin) result
    else Redirect(routes.Application.index)
  }
  def assumeSiteOwner(login: LoginSession)(result: => Result): Result = {
    if (login.isSiteOwner) result
    else Redirect(routes.Application.index)
  }

  object NeedAuthenticatedJson extends AuthenticatedBuilder(req => retrieveLoginSession(req), onUnauthorizedJson)

  def startLogin(uriOnLoginSuccess: String) = Action { implicit request =>
    if (retrieveLoginSession(request).isDefined)
      Redirect(uriOnLoginSuccess)
    else
      Ok(views.html.admin.login(loginForm, sanitize(uriOnLoginSuccess)))
  }

  def sanitize(url: String): String = 
    if (url.trim.startsWith("//")) "/"
    else if (url.indexOf("://") != -1) "/"
    else if (url.indexOf("csrfToken=") != -1) "/"
    else url

  def login = Action { implicit request =>
    val form = loginForm.bindFromRequest
    form.fold(
      onValidationErrorInLogin,
      user => tryLogin(user, form)
    )
  }

  def logoff(uriOnLogoffSuccess: String) = Action { implicit request =>
    Redirect(routes.Application.index).withSession(request.session - LoginUserKey)
  }

  def onValidationErrorInLogin(form: Form[LoginUser])(implicit request: Request[AnyContent]) = {
    logger.error("Validation error in NeedLogin.login.")
    BadRequest(views.html.admin.login(form, form("uri").value.get))
  }

  def tryLogin(
    user: LoginUser, form: Form[LoginUser]
  )(implicit request: Request[AnyContent]): Result = DB.withConnection { implicit conn => {
    StoreUser.findByUserName(user.compoundUserName) match {
      case None => 
        logger.error("Cannot find user '" + user.compoundUserName + "'")
        onLoginUserNotFound(form)
      case Some(rec) =>
        if (rec.passwordMatch(user.password)) {
          logger.info("Password ok '" + user.compoundUserName + "'")
          if (rec.isRegistrationIncomplete) {
            logger.info("Need user registration '" + user.compoundUserName + "'")
            Redirect(
              routes.UserEntry.registerUserInformation(rec.id.get)
            )
          }
          else {
            logger.info("Login success '" + user.compoundUserName + "'")
            Redirect(user.uri).flashing(
              "message" -> @Messages("welcome")
            ).withSession {
              (LoginUserKey, LoginSession.serialize(rec.id.get, System.currentTimeMillis + SessionTimeout))
            }
          }
        }
        else {
          logger.error("Password doesnot match '" + user.compoundUserName + "'")
          BadRequest(views.html.admin.login(
            form.withGlobalError(Messages("cannotLogin")),
            form("uri").value.get
          ))
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

object NeedLogin {
  def cfg = play.api.Play.maybeApplication.map(_.configuration).get
  def needAuthenticationEntirely = cfg.getBoolean("need.authentication.entirely").getOrElse(false)
  lazy val loginTimeoutInMinute = cfg.getString("login.timeout.minute").map {
    _.toInt
  }.getOrElse {
    5
  }
}
