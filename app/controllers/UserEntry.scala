package controllers

import java.util.Locale
import play.api.i18n.{Lang, Messages}
import play.api.Play.current
import controllers.I18n.I18nAware
import play.api.mvc.{Action, Controller, RequestHeader}
import play.api.db.DB
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models.UserRegistration
import models.Address
import helpers.UserEntryMail
import controllers.Shipping.firstNameKanaConstraint
import controllers.Shipping.lastNameKanaConstraint
import controllers.Shipping.Zip1Pattern
import controllers.Shipping.Zip2Pattern
import controllers.Shipping.TelPattern
import models.RegisterUserInfo
import play.api.templates.Html
import models.LoginSession
import models.StoreUser
import models.UserAddress
import models.ChangeUserInfo

object UserEntry extends Controller with HasLogger with I18nAware with NeedLogin {
  import NeedLogin._

  def jaForm(implicit lang: Lang) = Form(
    mapping(
      "companyName" -> text.verifying(nonEmpty, maxLength(64)),
      "zip1" -> text.verifying(z => Shipping.Zip1Pattern.matcher(z).matches),
      "zip2" -> text.verifying(z => Shipping.Zip2Pattern.matcher(z).matches),
      "prefecture" -> number,
      "address1" -> text.verifying(nonEmpty, maxLength(256)),
      "address2" -> text.verifying(nonEmpty, maxLength(256)),
      "address3" -> text.verifying(maxLength(256)),
      "tel" -> text.verifying(Messages("error.number"), z => Shipping.TelPattern.matcher(z).matches),
      "fax" -> text.verifying(Messages("error.number"), z => Shipping.TelOptionPattern.matcher(z).matches),
      "title" -> text.verifying(maxLength(256)),
      "firstName" -> text.verifying(firstNameConstraint: _*),
      "lastName" -> text.verifying(lastNameConstraint: _*),
      "email" -> text.verifying(nonEmpty, maxLength(128))
    )(UserRegistration.apply4Japan)(UserRegistration.unapply4Japan)
  )

  def index() = Action { implicit request => DB.withConnection { implicit conn => {
    implicit val login = loginSession(request, conn)
    lang.toLocale match {
      case Locale.JAPANESE =>
        Ok(views.html.userEntryJa(jaForm, Address.JapanPrefectures))
      case Locale.JAPAN =>
        Ok(views.html.userEntryJa(jaForm, Address.JapanPrefectures))
        
      case _ =>
        Ok(views.html.userEntryJa(jaForm, Address.JapanPrefectures))
    }
  }}}

  def submitUserJa() = Action { implicit request => DB.withConnection { implicit conn => {
    implicit val login = loginSession(request, conn)
    jaForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.userEntryJa(formWithErrors, Address.JapanPrefectures))
      },
      newUser => {
        UserEntryMail.sendUserRegistration(newUser)
        Ok(views.html.userEntryCompleted())
      }
    )
  }}}

  val changeUserInfoForm = Form(
    mapping(
      "firstName" -> text.verifying(firstNameConstraint: _*),
      "middleName" -> optional(text),
      "lastName" -> text.verifying(lastNameConstraint: _*),
      "firstNameKana" -> text.verifying(firstNameKanaConstraint: _*),
      "lastNameKana" -> text.verifying(lastNameKanaConstraint: _*),
      "email" -> email.verifying(emailConstraint: _*),
      "currentPassword" -> text.verifying(nonEmpty, maxLength(24)),
      "country" -> number,
      "zip1" -> text.verifying(z => Zip1Pattern.matcher(z).matches),
      "zip2" -> text.verifying(z => Zip2Pattern.matcher(z).matches),
      "prefecture" -> number,
      "address1" -> text.verifying(nonEmpty, maxLength(256)),
      "address2" -> text.verifying(nonEmpty, maxLength(256)),
      "address3" -> text.verifying(maxLength(256)),
      "tel1" -> text.verifying(Messages("error.number"), z => TelPattern.matcher(z).matches)
    )(ChangeUserInfo.apply)(ChangeUserInfo.unapply)
  )
    

  def createRegistrationForm(implicit lang: Lang) = Form(
    mapping(
      "firstName" -> text.verifying(firstNameConstraint: _*),
      "middleName" -> optional(text),
      "lastName" -> text.verifying(lastNameConstraint: _*),
      "firstNameKana" -> text.verifying(firstNameKanaConstraint: _*),
      "lastNameKana" -> text.verifying(lastNameKanaConstraint: _*),
      "email" -> email.verifying(emailConstraint: _*),
      "currentPassword" -> text.verifying(nonEmpty, maxLength(24)),
      "password" -> tuple(
        "main" -> text.verifying(passwordConstraint: _*),
        "confirm" -> text
      ).verifying(
        Messages("confirmPasswordDoesNotMatch"), passwords => passwords._1 == passwords._2
      ),
      "country" -> number,
      "zip1" -> text.verifying(z => Zip1Pattern.matcher(z).matches),
      "zip2" -> text.verifying(z => Zip2Pattern.matcher(z).matches),
      "prefecture" -> number,
      "address1" -> text.verifying(nonEmpty, maxLength(256)),
      "address2" -> text.verifying(nonEmpty, maxLength(256)),
      "address3" -> text.verifying(maxLength(256)),
      "tel1" -> text.verifying(Messages("error.number"), z => TelPattern.matcher(z).matches)
    )(RegisterUserInfo.apply4Japan)(RegisterUserInfo.unapply4Japan)
  )

  def registerUserInformation() = isAuthenticated { implicit login => implicit request =>
    Ok(
      lang.toLocale match {
        case Locale.JAPANESE =>
          registerUserInformationView(createRegistrationForm, Address.JapanPrefectures)
        case Locale.JAPAN =>
          registerUserInformationView(createRegistrationForm, Address.JapanPrefectures)

        case _ =>
          registerUserInformationView(createRegistrationForm, Address.JapanPrefectures)
      }
    )
  }

  def submitUserInfo = isAuthenticated { implicit login => implicit request =>
    createRegistrationForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(registerUserInformationView(formWithErrors, Address.JapanPrefectures)),
      newInfo =>
        DB.withConnection { implicit conn =>
          if (! newInfo.currentPasswordNotMatch(login.storeUser)) {
            BadRequest(
              registerUserInformationView(
                createRegistrationForm.fill(newInfo).withError("currentPassword", Messages("currentPasswordNotMatch")),
                Address.JapanPrefectures
              )
            )
          }
          else if (newInfo.isNaivePassword) {
            BadRequest(
              registerUserInformationView(
                createRegistrationForm.fill(newInfo).withError("password.main", Messages("naivePassword")),
                Address.JapanPrefectures
              )
            )
          }
          else {
            StoreUser.update(
              login.storeUser.id.get,
              login.storeUser.userName,
              newInfo.firstName,
              newInfo.middleName,
              newInfo.lastName,
              newInfo.email,
              login.storeUser.passwordHash,
              login.storeUser.salt,
              login.storeUser.companyName
            )

            val address = Address.createNew(
              countryCode = newInfo.countryCode,
              firstName = newInfo.firstName,
              middleName = newInfo.middleName.getOrElse(""),
              lastName = newInfo.lastName,
              firstNameKana = newInfo.firstNameKana,
              lastNameKana = newInfo.lastNameKana,
              zip1 = newInfo.zip1,
              zip2 = newInfo.zip2,
              prefecture = newInfo.prefecture,
              address1 = newInfo.address1,
              address2 = newInfo.address2,
              address3 = newInfo.address3,
              tel1 = newInfo.tel1
            )

            UserAddress.createNew(login.storeUser.id.get, address.id.get)
            Redirect(routes.Application.index)
          }
        }
    )
  }

  def registerUserInformationView(
    form: Form[RegisterUserInfo], prefectureTable: Seq[(String, String)]
  )(
    implicit lang: Lang,
    flash: play.api.mvc.Flash,
    request: RequestHeader,
    loginSession: LoginSession
  ): Html = lang.toLocale match {
    case Locale.JAPANESE =>
      views.html.registerUserInformationJa(form, prefectureTable)
    case Locale.JAPAN =>
      views.html.registerUserInformationJa(form, prefectureTable)

    case _ =>
      views.html.registerUserInformationJa(form, prefectureTable)
  }
}
