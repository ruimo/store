package controllers

import play.api.data.Forms._
import play.api._
import data.Form
import i18n.{Lang, Messages}
import play.api.mvc._
import play.filters.csrf.CSRF.Token._
import helpers.{RandomTokenGenerator, TokenGenerator}
import play.api.data.validation.Constraints._
import models.FirstSetup

object Admin extends Controller with NeedLogin {
  implicit val tokenGenerator: TokenGenerator = RandomTokenGenerator()
  private val logger = Logger(getClass)

  def firstSetupForm(implicit lang: Lang) = Form(
    mapping(
      "userName" -> text.verifying(minLength(6)),
      "firstName" -> text.verifying(nonEmpty),
      "middleName" -> optional(text),
      "lastName" -> text.verifying(nonEmpty),
      "email" -> email.verifying(nonEmpty),
      "password" -> tuple(
        "main" -> text(minLength = 8),
        "confirm" -> text
      ).verifying(
        Messages("confirmPasswordDoesNotMatch"), passwords => passwords._1 == passwords._2
      )
    )(FirstSetup.fromForm)(FirstSetup.toForm)
  )

  def startFirstSetup = Action { implicit request =>
    Ok(views.html.admin.firstSetup(firstSetupForm))
  }

  def firstSetup = Action { implicit request => {
    firstSetupForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(views.html.admin.firstSetup(formWithErrors)),
      firstSetup => {
        val createdUser = firstSetup.save
        Redirect(routes.Admin.index).flashing("message" -> "Welcome")
      }
    )
  }}
  
  def index = isAuthenticated { loginSession => implicit request =>
    Ok(views.html.admin.index())
  }
}

