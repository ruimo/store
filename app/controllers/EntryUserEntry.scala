package controllers

import helpers.Cache
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

  val PasswordHashStretchCount: () => Int = Cache.config(
    _.getInt("passwordHashStretchCount").getOrElse(1000)
  )

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
      "email" -> text.verifying(emailConstraint: _*)
    )(EntryUserRegistration.apply4Japan)(EntryUserRegistration.unapply4Japan)
  )

  def startRegistrationAsEntryUser(url: String) = Action { implicit request =>
    request2lang.toLocale match {
      case Locale.JAPANESE =>
        Ok(views.html.entryUserEntryJa(jaForm, Address.JapanPrefectures, sanitize(url)))
      case Locale.JAPAN =>
        Ok(views.html.entryUserEntryJa(jaForm, Address.JapanPrefectures, sanitize(url)))
        
      case _ =>
        Ok(views.html.entryUserEntryJa(jaForm, Address.JapanPrefectures, sanitize(url)))
    }
  }

  def submitUserJa(url: String) = Action { implicit request => DB.withConnection { implicit conn => {
    jaForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.entryUserEntryJa(formWithErrors, Address.JapanPrefectures, sanitize(url)))
      },
      newUser => {
        newUser.save(PasswordHashStretchCount())
// TODO        UserEntryMail.sendUserRegistration(newUser)
        Redirect(url)
      }
    )
  }}}
}
