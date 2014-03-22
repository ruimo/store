package controllers

import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models.{StoreUser, LocaleInfo, CreateSite, Site}
import play.api.data.Form
import controllers.I18n.I18nAware
import play.api.mvc.Controller
import play.api.db.DB
import play.api.i18n.Messages
import play.api.Play.current

object SiteMaintenance extends Controller with I18nAware with NeedLogin with HasLogger {
  val createSiteForm = Form(
    mapping(
      "langId" -> longNumber,
      "siteName" -> text.verifying(nonEmpty, maxLength(32))
    ) (CreateSite.apply)(CreateSite.unapply)
  )

  def index = isAuthenticated { implicit login => forSuperUser { implicit request =>
    Ok(views.html.admin.siteMaintenance())
  }}

  def startCreateNewSite = isAuthenticated { implicit login => forSuperUser { implicit request =>
    Ok(views.html.admin.createNewSite(createSiteForm, LocaleInfo.localeTable))
  }}

  def createNewSite = isAuthenticated { implicit login => forSuperUser { implicit request =>
    createSiteForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in SiteMaintenance.createNewSite.")
        BadRequest(views.html.admin.createNewSite(formWithErrors, LocaleInfo.localeTable))
      },
      newSite => DB.withConnection { implicit conn =>
        newSite.save
        Redirect(
          routes.SiteMaintenance.startCreateNewSite()
        ).flashing("message" -> Messages("siteIsCreated"))
      }
    )
  }}

  def editSite = isAuthenticated { implicit login => forSuperUser { implicit request =>
    DB.withConnection { implicit conn =>
      Ok(views.html.admin.editSite(Site.listByName()))
    }
  }}

  def deleteSite(id: Long) = isAuthenticated { implicit login => forSuperUser { implicit request =>
    DB.withConnection { implicit conn =>
      Site.delete(id)
    }
    Redirect(routes.SiteMaintenance.editSite())
  }}

}
