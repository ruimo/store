package controllers

import play.api.libs.json.{JsObject, Json}
import models.ChangeSite
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

  val changeSiteForm = Form(
    mapping(
      "siteId" -> longNumber,
      "langId" -> longNumber,
      "siteName" -> text.verifying(nonEmpty, maxLength(32))
    ) (ChangeSite.apply)(ChangeSite.unapply)
  )

  def index = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      Ok(views.html.admin.siteMaintenance())
    }
  }

  def startCreateNewSite = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      Ok(views.html.admin.createNewSite(createSiteForm, LocaleInfo.localeTable))
    }
  }

  def createNewSite = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
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
    }
  }

  def editSite = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      DB.withConnection { implicit conn =>
        Ok(views.html.admin.editSite(Site.listByName()))
      }
    }
  }

  def changeSiteStart(siteId: Long) = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      DB.withConnection { implicit conn =>
        val site = Site(siteId)
        Ok(
          views.html.admin.changeSite(
            changeSiteForm.fill(ChangeSite(site)),
            LocaleInfo.localeTable
          )
        )
      }
    }
  }

  def changeSite = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      changeSiteForm.bindFromRequest.fold(
        formWithErrors => {
          logger.error("Validation error in SiteMaintenance.changeSite.")
          BadRequest(views.html.admin.changeSite(formWithErrors, LocaleInfo.localeTable))
        },
        newSite => DB.withConnection { implicit conn =>
          newSite.update()
          Redirect(
            routes.SiteMaintenance.editSite()
          ).flashing("message" -> Messages("siteIsChanged"))
        }
      )
    }
  }

  def deleteSite(id: Long) = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      DB.withConnection { implicit conn =>
        Site.delete(id)
      }
      Redirect(routes.SiteMaintenance.editSite())
    }
  }

  def sitesAsJson = NeedAuthenticatedJson { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      DB.withConnection { implicit conn =>
        Ok(Json.obj(
          "sites" -> Site.tableForDropDown.map { t => t._2 }.toSeq
        ))
      }
    }
  }
}
