package controllers

import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.mvc.Controller
import controllers.I18n.I18nAware
import play.api.data.Form
import models.{LocaleInfo, CreateCategory, Category}
import play.api.i18n.Messages
import play.api.db.DB
import play.api.Play.current

object CategoryMaintenance extends Controller with I18nAware with NeedLogin with HasLogger {
  val createCategoryForm = Form(
    mapping(
      "langId" -> longNumber,
      "categoryName" -> text.verifying(nonEmpty, maxLength(32))
    ) (CreateCategory.apply)(CreateCategory.unapply)
  )

  def index = isAuthenticated { implicit login => forSuperUser { implicit request =>
    Ok(views.html.admin.categoryMaintenance())
  }}

  def startCreateNewCategory = isAuthenticated { implicit login => forSuperUser { implicit request => {
    Ok(views.html.admin.createNewCategory(createCategoryForm, LocaleInfo.localeTable))
  }}}

  def createNewCategory = isAuthenticated { implicit login => forSuperUser { implicit request =>
    createCategoryForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in CategoryMaintenance.createNewCategory.")
        BadRequest(views.html.admin.createNewCategory(formWithErrors, LocaleInfo.localeTable))
      },
      newCategory => DB.withConnection { implicit conn =>
        newCategory.save
        Redirect(
          routes.CategoryMaintenance.startCreateNewCategory
        ).flashing("message" -> Messages("categoryIsCreated"))
      }
    )
  }}

  def editCategory(start: Int  = 0, size: Int = 10) = isAuthenticated { implicit login => forSuperUser { implicit request => 
    DB.withConnection { implicit conn => {
      val p = Category.list(page = start, pageSize = size, locale = LocaleInfo.byLang(lang))
      Ok(views.html.admin.editCategory(p))
    }}
  }}
}
