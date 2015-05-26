package controllers

import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.mvc.Controller
import controllers.I18n.I18nAware
import play.api.data.Form
import models.{LocaleInfo, CreateTax, Tax}
import play.api.i18n.Messages
import play.api.db.DB
import play.api.Play.current

object TaxMaintenance extends Controller with I18nAware with NeedLogin with HasLogger {
  val createTaxForm = Form(
    mapping(
      "taxType" -> number,
      "langId" -> longNumber,
      "taxName" -> text.verifying(nonEmpty, maxLength(32)),
      "rate" -> bigDecimal(5, 3)
    ) (CreateTax.apply)(CreateTax.unapply)
  )

  def index = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      Ok(views.html.admin.taxMaintenance())
    }
  }

  def startCreateNewTax = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      Ok(views.html.admin.createNewTax(createTaxForm, Tax.taxTypeTable, LocaleInfo.localeTable))
    }
  }

  def createNewTax = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      createTaxForm.bindFromRequest.fold(
        formWithErrors => {
          logger.error("Validation error in TaxMaintenance.createNewTax.")
          BadRequest(views.html.admin.createNewTax(formWithErrors, Tax.taxTypeTable, LocaleInfo.localeTable))
        },
        newTax => {
          DB.withConnection { implicit conn =>
            newTax.save()
          }
          Redirect(
            routes.TaxMaintenance.startCreateNewTax
          ).flashing("message" -> Messages("taxIsCreated"))
        }
      )
    }
  }

  def editTax = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      Ok(views.html.admin.editTax())
    }
  }
}

