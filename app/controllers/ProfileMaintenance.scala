package controllers

import models.LoginSession
import java.util.Locale
import scala.collection.immutable
import play.api.i18n.{Lang, Messages}
import play.api.Play.current
import play.api.data.Forms._
import controllers.I18n.I18nAware
import play.api.data.Form
import play.api.db.DB
import play.api.mvc.{Controller, RequestHeader}
import java.sql.Connection
import constraints.FormConstraints._
import models.ModifyUserProfile

object ProfileMaintenance extends Controller with NeedLogin with HasLogger with I18nAware {
  val changeProfileForm = Form(
    mapping(
      "firstName" -> text.verifying(firstNameConstraint: _*),
      "middleName" -> optional(text),
      "lastName" -> text.verifying(lastNameConstraint: _*),
      "email" -> email.verifying(emailConstraint: _*),
      "password" -> text.verifying(passwordConstraint: _*)
    )(ModifyUserProfile.apply)(ModifyUserProfile.unapply)
  )

  def index() = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    Ok(views.html.profileMaintenance())
  }

  def changeProfile() = NeedAuthenticated { implicit request =>
    implicit val login: LoginSession = request.user
    val form = changeProfileForm.fill(ModifyUserProfile(login))
    
    request2lang.toLocale match {
      case Locale.JAPANESE | Locale.JAPAN =>
        Ok(views.html.changeUserProfileJa(form))
      case _ =>
        Ok(views.html.changeUserProfileJa(form))
    }
  }

  def doChangeProfile() = NeedAuthenticated { implicit request =>
    implicit val login: LoginSession = request.user

    changeProfileForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.changeUserProfileJa(formWithErrors))
      },
      updated => {
        if (login.storeUser.passwordMatch(updated.password)) {
          DB.withConnection { implicit conn =>
            updated.save(login)
          }
          Redirect(routes.Application.index).flashing("message" -> Messages("userProfileUpdated"))
        }
        else {
          BadRequest(
            views.html.changeUserProfileJa(
              changeProfileForm.fill(updated).withError(
                "password", "currentPasswordNotMatch"
              )
            )
          )
        }
      }
    )
  }
}
