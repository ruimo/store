package controllers

import play.api.mvc.Result
import models.UniqueConstraintException
import helpers.Sanitize.{forUrl => sanitize}
import play.api.Play
import helpers.PasswordHash
import constraints.FormConstraints._
import java.util.Locale
import play.api.i18n.{Lang, Messages}
import play.api.Play.current
import controllers.I18n.I18nAware
import play.api.mvc.{Action, Controller, RequestHeader}
import play.api.db.DB
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models.EntryUserRegistration
import models.ResetWithNewPassword
import models.Address
import models.ResetPassword
import models.PasswordReset
import models.ChangePassword
import models.PromoteAnonymousUser
import helpers.UserEntryMail
import controllers.Shipping.firstNameKanaConstraint
import controllers.Shipping.lastNameKanaConstraint
import controllers.Shipping.Zip1Pattern
import controllers.Shipping.Zip2Pattern
import controllers.Shipping.TelPattern
import models.RegisterUserInfo
import play.twirl.api.Html
import models.LoginSession
import models.StoreUser
import models.UserAddress
import models.ChangeUserInfo
import models.Prefecture
import models.CountryCode
import models.JapanPrefecture
import java.sql.Connection
import helpers.NotificationMail
import scala.concurrent.duration._
import scala.language.postfixOps
import helpers.Cache

object EntryUserEntry extends Controller with HasLogger with I18nAware with NeedLogin {
  import NeedLogin._

  def jaForm(implicit lang: Lang) = Form(
    mapping(
      "userName" -> text.verifying(normalUserNameConstraint(): _*),
      "password" -> tuple(
        "main" -> text.verifying(passwordConstraint: _*),
        "confirm" -> text
      ).verifying(
        Messages("confirmPasswordDoesNotMatch"), passwords => passwords._1 == passwords._2
      ),
      "zip1" -> text.verifying(z => Shipping.Zip1Pattern.matcher(z).matches),
      "zip2" -> text.verifying(z => Shipping.Zip2Pattern.matcher(z).matches),
      "prefecture" -> number,
      "address1" -> text.verifying(nonEmpty, maxLength(256)),
      "address2" -> text.verifying(nonEmpty, maxLength(256)),
      "address3" -> text.verifying(maxLength(256)),
      "tel" -> text.verifying(Messages("error.number"), z => Shipping.TelPattern.matcher(z).matches),
      "fax" -> text.verifying(Messages("error.number"), z => Shipping.TelOptionPattern.matcher(z).matches),
      "firstName" -> text.verifying(firstNameConstraint: _*),
      "lastName" -> text.verifying(lastNameConstraint: _*),
      "firstNameKana" -> text.verifying(firstNameConstraint: _*),
      "lastNameKana" -> text.verifying(lastNameConstraint: _*),
      "email" -> text.verifying(emailConstraint: _*)
    )(EntryUserRegistration.apply4Japan)(EntryUserRegistration.unapply4Japan)
  )

  def userForm(implicit lang: Lang) = Form(
    mapping(
      "userName" -> text.verifying(normalUserNameConstraint(): _*),
      "firstName" -> text.verifying(firstNameConstraint: _*),
      "middleName" -> optional(text.verifying(middleNameConstraint: _*)),
      "lastName" -> text.verifying(lastNameConstraint: _*),
      "email" -> text.verifying(emailConstraint: _*),
      "password" -> tuple(
        "main" -> text.verifying(passwordConstraint: _*),
        "confirm" -> text
      ).verifying(
        Messages("confirmPasswordDoesNotMatch"), passwords => passwords._1 == passwords._2
      )
    )(PromoteAnonymousUser.apply)(PromoteAnonymousUser.unapply)
  )

  def showForm(url: String)(
    implicit request: RequestHeader
  ): Result = {
    DB.withConnection { implicit conn =>
      request2lang.toLocale match {
        case Locale.JAPANESE =>
          Ok(views.html.entryUserEntryJa(jaForm, Address.JapanPrefectures, sanitize(url)))
        case Locale.JAPAN =>
          Ok(views.html.entryUserEntryJa(jaForm, Address.JapanPrefectures, sanitize(url)))

        case _ =>
          Ok(views.html.entryUserEntryJa(jaForm, Address.JapanPrefectures, sanitize(url)))
      }
    }
  }

  def startRegistrationAsEntryUser(url: String) = Action { implicit request =>
    if (retrieveLoginSession(request).isDefined) {
      Redirect(url)
    }
    else showForm(url)
  }

  def submitUserJa(url: String) = Action { implicit request => DB.withConnection { implicit conn => {
    if (retrieveLoginSession(request).isDefined) {
      Redirect(url)
    }
    else {
      jaForm.bindFromRequest.fold(
        formWithErrors => {
          BadRequest(views.html.entryUserEntryJa(formWithErrors, Address.JapanPrefectures, sanitize(url)))
        },
        newUser => DB.withConnection { implicit conn: Connection =>
          if (newUser.isNaivePassword) {
            BadRequest(
              views.html.entryUserEntryJa(
                jaForm.fill(newUser).withError("password.main", "naivePassword"),
                Address.JapanPrefectures, sanitize(url)
              )
            )
          }
          else {
            try {
              val user = newUser.save(CountryCode.JPN, StoreUser.PasswordHashStretchCount())
              Redirect(url).flashing(
                "message" -> Messages("welcome")
              ).withSession {
                (LoginUserKey,LoginSession.serialize(user.id.get, System.currentTimeMillis + SessionTimeout))
              }
            }
            catch {
              case e: UniqueConstraintException =>
                BadRequest(
                  views.html.entryUserEntryJa(
                    jaForm.fill(newUser).withError("userName", "userNameIsTaken"),
                    Address.JapanPrefectures, sanitize(url)
                  )
                )
              case t: Throwable => throw t
            }
          }
        }
      )
    }
  }}}

  def startRegisterCurrentUserAsEntryUser = NeedAuthenticated { implicit request =>
    retrieveLoginSession(request) match {
      case Some(login) =>
        if (login.isAnonymousBuyer) Ok(
          views.html.promoteAnonymousUser(
            userForm.fill(
              PromoteAnonymousUser(
                "",
                login.storeUser.firstName,
                login.storeUser.middleName,
                login.storeUser.lastName,
                login.storeUser.email,
                ("", "")
              )
            ).discardingErrors
          )
        )
        else Redirect(routes.Application.index.url)
      case None =>
        Redirect(routes.Application.index.url)
    }
  }

  def promoteAnonymousUser = NeedAuthenticated { implicit request =>
    retrieveLoginSession(request) match {
      case Some(login) =>
        if (login.isAnonymousBuyer) {
          userForm.bindFromRequest.fold(
            formWithErrors =>
              BadRequest(views.html.promoteAnonymousUser(formWithErrors)),
            newUser => DB.withConnection { implicit conn: Connection =>
              if (newUser.isNaivePassword) {
                BadRequest(
                  views.html.promoteAnonymousUser(
                    userForm.fill(newUser).withError("password.main", "naivePassword")
                  )
                )
              }
              else {
                try {
                  if (! newUser.update(login)) {
                    throw new Error("Cannot update user " + login)
                  }
                  Redirect(routes.Application.index).flashing(
                    "message" -> Messages("anonymousUserPromoted")
                  )
                }
                catch {
                  case e: UniqueConstraintException =>
                    BadRequest(
                      views.html.promoteAnonymousUser(
                        userForm.fill(newUser).withError("userName", "userNameIsTaken")
                      )
                    )
                  case t: Throwable => throw t
                }
              }
            }
          )
        }
        else Redirect(routes.Application.index.url)
      case None =>
        Redirect(routes.Application.index.url)
    }
  }
}
