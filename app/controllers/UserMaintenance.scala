package controllers

import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models._
import play.api.data.Form
import controllers.I18n.I18nAware
import play.api.mvc.Controller
import play.api.db.DB
import play.api.i18n.{Lang, Messages}
import play.api.Play.current
import helpers.{TokenGenerator, RandomTokenGenerator}

object UserMaintenance extends Controller with I18nAware with NeedLogin with HasLogger {
  implicit val tokenGenerator: TokenGenerator = RandomTokenGenerator()

  def newSiteOwnerForm(implicit lang: Lang) = Form(
    mapping(
      "siteId" -> longNumber,
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
      ),
      "companyName" -> text.verifying(companyNameConstraint: _*)
    )(CreateSiteOwner.fromForm)(CreateSiteOwner.toForm)
  )

  def index = isAuthenticated { implicit login => forSuperUser { implicit request =>
    if (login.isSuperUser) {
      Ok(views.html.admin.userMaintenance())
    }
    else {
      Redirect(routes.Admin.index)
    }
  }}

  def startCreateNewSuperUser = isAuthenticated { implicit login => forSuperUser { implicit request =>
    Ok(views.html.admin.createNewSuperUser(Admin.createUserForm(FirstSetup.fromForm, FirstSetup.toForm)))
  }}

  def startCreateNewSiteOwner = isAuthenticated { implicit login => forSuperUser { implicit request =>
    DB.withConnection { implicit conn =>
      Ok(views.html.admin.createNewSiteOwner(newSiteOwnerForm, Site.tableForDropDown))
    }
  }}

  def startCreateNewNormalUser = isAuthenticated { implicit login => forSuperUser { implicit request =>
    Ok(views.html.admin.createNewNormalUser(Admin.createUserForm(CreateNormalUser.fromForm, CreateNormalUser.toForm)))
  }}

  def createNewSuperUser = isAuthenticated { implicit login => forSuperUser { implicit request =>
    Admin.createUserForm(FirstSetup.fromForm, FirstSetup.toForm).bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in UserMaintenance.createNewSuperUser.")
        BadRequest(views.html.admin.createNewSuperUser(formWithErrors))
      },
      newUser => DB.withConnection { implicit conn =>
        newUser.save
        Redirect(
          routes.UserMaintenance.startCreateNewSuperUser
        ).flashing("message" -> Messages("userIsCreated"))
      }
    )
  }}

  def createNewSiteOwner = isAuthenticated { implicit login => forSuperUser { implicit request =>
    newSiteOwnerForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in UserMaintenance.createNewSiteOwner.")
        DB.withConnection { implicit conn =>
          BadRequest(views.html.admin.createNewSiteOwner(formWithErrors, Site.tableForDropDown))
        }
      },
      newUser => DB.withTransaction { implicit conn =>
        newUser.save
        Redirect(
          routes.UserMaintenance.startCreateNewSiteOwner
        ).flashing("message" -> Messages("userIsCreated"))
      }
    )
  }}

  def createNewNormalUser = isAuthenticated { implicit login => forSuperUser { implicit request =>
    Admin.createUserForm(CreateNormalUser.fromForm, CreateNormalUser.toForm).bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in UserMaintenance.createNewNormalUser.")
        BadRequest(views.html.admin.createNewNormalUser(formWithErrors))
      },
      newUser => DB.withConnection { implicit conn =>
        newUser.save
        Redirect(
          routes.UserMaintenance.startCreateNewNormalUser
        ).flashing("message" -> Messages("userIsCreated"))
      }
    )
  }}

  def editUser = isAuthenticated { implicit login => forSuperUser { implicit request =>
    DB.withConnection { implicit conn =>
      Ok(views.html.admin.editUser(StoreUser.listUsers()))
    }
  }}

  def deleteUser(id: Long) = isAuthenticated { implicit login => forSuperUser { implicit request =>
    DB.withConnection { implicit conn =>
      StoreUser.delete(id)
    }
    Redirect(routes.UserMaintenance.editUser)
  }}
}
