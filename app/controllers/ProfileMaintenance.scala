package controllers

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
    implicit val login = request.user
    Ok(views.html.changeUserProfile(changeProfileForm))
  }
}
