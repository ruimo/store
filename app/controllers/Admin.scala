package controllers

import play.api.data.Forms._
import play.api._
import data.Form
import db.DB
import i18n.{Lang, Messages}
import play.api.mvc._
import play.filters.csrf.CSRF.Token._
import helpers.{RandomTokenGenerator, TokenGenerator}
import play.api.data.validation.Constraints._
import models.FirstSetup
import controllers.I18n.I18nAware
import play.api.Play.current

object Admin extends Controller with I18nAware with NeedLogin with HasLogger {
  implicit val tokenGenerator: TokenGenerator = RandomTokenGenerator()

  def firstSetupForm(implicit lang: Lang) = Form(
    mapping(
      "userName" -> text.verifying(userNameConstraint: _*),
      "firstName" -> text.verifying(firstNameConstraint: _*),
      "middleName" -> optional(text),
      "lastName" -> text.verifying(lastNameConstraint: _*),
      "email" -> email.verifying(emailConstraint: _*),
      "password" -> tuple(
        "main" -> text.verifying(userNameConstraint: _*),
        "confirm" -> text
      ).verifying(
        Messages("confirmPasswordDoesNotMatch"), passwords => passwords._1 == passwords._2
      )
    )(FirstSetup.fromForm)(FirstSetup.toForm)
  )

  def startFirstSetup = Action { implicit request => {
    Ok(views.html.admin.firstSetup(firstSetupForm))
  }}

  def firstSetup = Action { implicit request => {
    firstSetupForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(views.html.admin.firstSetup(formWithErrors)),
      firstSetup => DB.withConnection { implicit conn => {
        val createdUser = firstSetup.save
        Redirect(routes.Admin.index).flashing("message" -> Messages("welcome"))
      }}
    )
  }}
  
  def index = isAuthenticated { implicit login => implicit request =>
    Ok(views.html.admin.index())
  }
}
