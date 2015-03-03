package controllers

import play.api.Play
import play.api.Mode
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
import models.UserRegistration
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
import play.api.templates.Html
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

object UserEntry extends Controller with HasLogger with I18nAware with NeedLogin {
  import NeedLogin._

  val AppVal = Play.maybeApplication.get
  def App = if (Play.maybeApplication.get.mode == Mode.Test) Play.maybeApplication.get else AppVal
  def Config = App.configuration
  def ResetPasswordTimeout: Long = Config.getMilliseconds("resetPassword.timeout").getOrElse {
    (30 minutes).toMillis
  }
  def AutoLoginAfterRegistration: Boolean = Config.getBoolean("auto.login.after.registration").getOrElse(false)

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

  val resetPasswordForm = Form(
    mapping(
      "companyId" -> optional(text),
      "userName" -> text.verifying(nonEmpty)
    )(PasswordReset.apply)(PasswordReset.unapply)
  )

  def changePasswordForm(implicit lang: Lang) = Form(
    mapping(
      "currentPassword" -> text.verifying(nonEmpty),
      "newPassword" -> tuple(
        "main" -> text.verifying(passwordConstraint: _*),
        "confirm" -> text.verifying(passwordConstraint: _*)
      ).verifying(
        Messages("confirmPasswordDoesNotMatch"), passwords => passwords._1 == passwords._2
      )
    )(ChangePassword.apply)(ChangePassword.unapply)
  )

  def resetWithNewPasswordForm(implicit lang: Lang) = Form(
    mapping(
      "userId" -> longNumber,
      "token" -> longNumber,
      "password" -> tuple(
        "main" -> text.verifying(passwordConstraint: _*),
        "confirm" -> text.verifying(passwordConstraint: _*)
      ).verifying(
        Messages("confirmPasswordDoesNotMatch"), passwords => passwords._1 == passwords._2
      )
    )(ResetWithNewPassword.apply)(ResetWithNewPassword.unapply)
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

  def updateUserInfoForm(implicit lang: Lang) = Form(
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
      "email" -> email.verifying(nonEmpty).verifying(emailConstraint: _*),
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
      "tel1" -> text.verifying(nonEmpty).verifying(Messages("error.number"), z => TelPattern.matcher(z).matches)
    )(RegisterUserInfo.apply4Japan)(RegisterUserInfo.unapply4Japan)
  )

  def registerUserInformation(userId: Long) = Action { implicit request =>
    Ok(
      lang.toLocale match {
        case Locale.JAPANESE =>
          registerUserInformationView(userId, createRegistrationForm)
        case Locale.JAPAN =>
          registerUserInformationView(userId, createRegistrationForm)

        case _ =>
          registerUserInformationView(userId, createRegistrationForm)
      }
    )
  }

  def submitUserInfo(userId: Long) = Action { implicit request =>
    createRegistrationForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(registerUserInformationView(userId, formWithErrors)),
      newInfo =>
        DB.withConnection { implicit conn =>
          if (! newInfo.currentPasswordNotMatch(userId)) {
            BadRequest(
              registerUserInformationView(
                userId,
                createRegistrationForm.fill(newInfo).withError("currentPassword", Messages("currentPasswordNotMatch"))
              )
            )
          }
          else if (newInfo.isNaivePassword) {
            BadRequest(
              registerUserInformationView(
                userId,
                createRegistrationForm.fill(newInfo).withError("password.main", Messages("naivePassword"))
              )
            )
          }
          else {
            val u = StoreUser(userId)
            StoreUser.update(
              userId,
              u.userName,
              newInfo.firstName,
              newInfo.middleName,
              newInfo.lastName,
              newInfo.email,
              PasswordHash.generate(newInfo.passwords._1, u.salt),
              u.salt,
              u.companyName
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

            UserAddress.createNew(userId, address.id.get)
            val result = Redirect(
              routes.Application.index
            ).flashing(
              "message" -> Messages("userInfoIsUpdated")
            )

            if (AutoLoginAfterRegistration) {
              result.withSession {
                (LoginUserKey, LoginSession.serialize(userId, System.currentTimeMillis + SessionTimeout))
              }
            }
            else result
          }
        }
    )
  }

  def registerUserInformationView(
    userId: Long,
    form: Form[RegisterUserInfo]
  )(
    implicit lang: Lang,
    flash: play.api.mvc.Flash,
    request: RequestHeader
  ): Html = lang.toLocale match {
    case Locale.JAPANESE =>
      views.html.registerUserInformationJa(userId, form, Address.JapanPrefectures)
    case Locale.JAPAN =>
      views.html.registerUserInformationJa(userId, form, Address.JapanPrefectures)

    case _ =>
      views.html.registerUserInformationJa(userId, form, Address.JapanPrefectures)
  }

  def updateUserInfoStart = isAuthenticated { implicit login => implicit request =>
    DB.withConnection { implicit conn =>
      val currentInfo: Option[ChangeUserInfo] = UserAddress.getByUserId(login.storeUser.id.get) map { ua =>
        val user = login.storeUser
        val addr = Address.byId(ua.addressId)
        ChangeUserInfo(
          firstName = user.firstName,
          middleName = user.middleName,
          lastName = user.lastName,
          firstNameKana = addr.firstNameKana,
          lastNameKana = addr.lastNameKana,
          email = user.email, 
          currentPassword = "",
          countryIndex = addr.countryCode.ordinal,
          zip1 = addr.zip1,
          zip2 = addr.zip2,
          prefectureIndex = addr.prefecture.code,
          address1 = addr.address1,
          address2 = addr.address2,
          address3 = addr.address3,
          tel1 = addr.tel1
        )
      }

      val form = currentInfo match {
        case Some(info) => updateUserInfoForm.fill(info)
        case None => updateUserInfoForm
      }

      Ok(
        lang.toLocale match {
          case Locale.JAPANESE =>
            views.html.updateUserInfoJa(form, Address.JapanPrefectures)
          case Locale.JAPAN =>
            views.html.updateUserInfoJa(form, Address.JapanPrefectures)

          case _ =>
            views.html.updateUserInfoJa(form, Address.JapanPrefectures)
        }
      )
    }
  }

  def updateUserInfo = isAuthenticated { implicit login => implicit request =>
    updateUserInfoForm.bindFromRequest.fold(
      formWithErrors => BadRequest(
        lang.toLocale match {
          case Locale.JAPANESE =>
            views.html.updateUserInfoJa(formWithErrors, Address.JapanPrefectures)
          case Locale.JAPAN =>
            views.html.updateUserInfoJa(formWithErrors, Address.JapanPrefectures)

          case _ =>
            views.html.updateUserInfoJa(formWithErrors, Address.JapanPrefectures)
        }
      ),
      newInfo => {
        val prefTable = lang.toLocale match {
          case Locale.JAPANESE =>
            i: Int => JapanPrefecture.byIndex(i)
          case Locale.JAPAN =>
            i: Int => JapanPrefecture.byIndex(i)

          case _ =>
            i: Int => JapanPrefecture.byIndex(i)
        }

        DB.withConnection { implicit conn =>
          if (login.storeUser.passwordMatch(newInfo.currentPassword)) {
            updateUser(newInfo, login.storeUser)

            UserAddress.getByUserId(login.storeUser.id.get) match {
              case Some(ua: UserAddress) =>
                updateAddress(Address.byId(ua.addressId), newInfo, prefTable)
              case None =>
                val address = createAddress(newInfo, prefTable)
                UserAddress.createNew(login.storeUser.id.get, address.id.get)
            }

            Redirect(routes.Application.index).flashing("message" -> Messages("userInfoIsUpdated"))
          }
          else {
            val form = updateUserInfoForm.fill(newInfo).withError("currentPassword", "confirmPasswordDoesNotMatch")
            BadRequest(
              lang.toLocale match {
                case Locale.JAPANESE =>
                  views.html.updateUserInfoJa(form, Address.JapanPrefectures)
                case Locale.JAPAN =>
                  views.html.updateUserInfoJa(form, Address.JapanPrefectures)

                case _ =>
                  views.html.updateUserInfoJa(form, Address.JapanPrefectures)
              }
            )
          }
        }
      }
    )
  }

  def updateUser(userInfo: ChangeUserInfo, user: StoreUser)(implicit conn: Connection) {
    StoreUser.update(
      user.id.get,
      user.userName, 
      userInfo.firstName, userInfo.middleName, userInfo.lastName,
      userInfo.email, user.passwordHash, user.salt, user.companyName
    )
  }

  def updateAddress(
    address: Address, userInfo: ChangeUserInfo, prefectureTable: Int => Prefecture
  )(implicit conn: Connection) {
    Address.update(
      address.copy(
        countryCode = CountryCode.byIndex(userInfo.countryIndex),
        firstName = userInfo.firstName,
        middleName = userInfo.middleName.getOrElse(""),
        lastName = userInfo.lastName,
        firstNameKana = userInfo.firstNameKana,
        lastNameKana = userInfo.lastNameKana,
        zip1 = userInfo.zip1,
        zip2 = userInfo.zip2,
        prefecture = prefectureTable(userInfo.prefectureIndex),
        address1 = userInfo.address1,
        address2 = userInfo.address2,
        address3 = userInfo.address3,
        tel1 = userInfo.tel1
      )
    )
  }

  def createAddress(
    userInfo: ChangeUserInfo, prefectureTable: Int => Prefecture
  )(implicit conn: Connection): Address = {
    Address.createNew(
      countryCode = CountryCode.byIndex(userInfo.countryIndex),
      firstName = userInfo.firstName,
      middleName = userInfo.middleName.getOrElse(""),
      lastName = userInfo.lastName,
      firstNameKana = userInfo.firstNameKana,
      lastNameKana = userInfo.lastNameKana,
      zip1 = userInfo.zip1,
      zip2 = userInfo.zip2,
      prefecture = prefectureTable(userInfo.prefectureIndex),
      address1 = userInfo.address1,
      address2 = userInfo.address2,
      address3 = userInfo.address3,
      tel1 = userInfo.tel1
    )
  }

  def resetPasswordStart = Action { implicit request =>
    Ok(views.html.resetPassword(resetPasswordForm))
  }

  def resetPassword = Action { implicit request =>
    resetPasswordForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(views.html.resetPassword(formWithErrors)),
      newInfo => {
        DB.withConnection { implicit conn =>
          StoreUser.findByUserName(newInfo.compoundUserName) match {
            case Some(user) => {
              ResetPassword.removeByStoreUserId(user.id.get)
              val rec = ResetPassword.createNew(user.id.get)
              NotificationMail.sendResetPasswordConfirmation(user, rec)
              Ok(views.html.resetPasswordMailSent(rec))
            }

            case None =>
              BadRequest(
                views.html.resetPassword(
                  resetPasswordForm.fill(newInfo).withError("userName", "error.value")
                )
              )
          }
        }
      }
    )
  }

  def resetPasswordConfirm(userId: Long, token: Long) = Action { implicit request =>
    DB.withConnection { implicit conn =>
      logger.info("Password reset confirmation ok = " + userId + ", token = " + token)
      if (ResetPassword.isValid(userId, token, System.currentTimeMillis - ResetPasswordTimeout)) {
        Ok(
          views.html.resetWithNewPassword(
            resetWithNewPasswordForm.bind(
              Map(
                "userId" -> userId.toString,
                "token" -> token.toString
              )
            ).discardingErrors
          )
        )
      }
      else {
        logger.error("Invalid password reset confirmation userId = " + userId + ", token = " + token)
        Redirect(routes.Application.index).flashing("message" -> Messages("general.error"))
      }
    }
  }

  def resetWithNewPassword = Action { implicit request =>
    resetWithNewPasswordForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.resetWithNewPassword(formWithErrors))
      },
      newInfo => {
        DB.withConnection { implicit conn =>
          if (
            ResetPassword.changePassword(
              newInfo.userId,
              newInfo.token, 
              System.currentTimeMillis - ResetPasswordTimeout,
              newInfo.passwords._1
            )
          ) {
            Redirect(routes.UserEntry.resetPasswordCompleted)
          }
          else {
            logger.error(
              "Cannot change password: userId = " + newInfo.userId + ", token = " + newInfo.token
            )
            Redirect(routes.Application.index)
              .flashing("message" -> Messages("general.error"))
          }
        }
      }
    )
  }

  def resetPasswordCompleted = Action { implicit request =>
    Ok(views.html.resetPasswordCompleted())
  }

  def changePasswordStart = isAuthenticated { implicit login => implicit request =>
    Ok(views.html.changePassword(changePasswordForm))
  }

  def changePassword = isAuthenticated { implicit login => implicit request =>
    changePasswordForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in changing password. '" + login.storeUser.userName + "'")
        BadRequest(views.html.changePassword(formWithErrors))
      },
      newInfo => {
        if (! login.storeUser.passwordMatch(newInfo.currentPassword)) {
          logger.error("Current password does not match. '" + login.storeUser.userName + "'")
          BadRequest(
            views.html.changePassword(
              changePasswordForm.withError("currentPassword", Messages("currentPasswordNotMatch"))
            )
          )
        }
        else {
          DB.withConnection { implicit conn =>
            if (newInfo.changePassword(login.storeUser.id.get)) {
              Redirect(routes.Application.index)
                .flashing("message" -> Messages("passwordIsUpdated"))
            }
            else {
              BadRequest(
                views.html.changePassword(
                  changePasswordForm.withGlobalError("general.error")
                )
              )
            }
          }
        }
      }
    )
  }
}
