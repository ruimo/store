package controllers

import play.api.data.Form
import controllers.I18n.I18nAware
import play.api.mvc.Controller
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models.{Category, LocaleInfo, CreateItem}
import play.api.i18n.Messages

object ItemMaintenance extends Controller with I18nAware with NeedLogin with HasLogger {
  val createItemForm = Form(
    mapping(
      "langId" -> longNumber,
      "categoryId" -> longNumber,
      "itemName" -> text.verifying(nonEmpty, maxLength(255)),
      "price" -> bigDecimal.verifying(min(BigDecimal(0))),
      "description" -> text.verifying(maxLength(500))
    ) (CreateItem.apply)(CreateItem.unapply)
  )

  def index = isAuthenticated { loginSession => implicit request =>
    Ok(views.html.admin.itemMaintenance())
  }

  def startCreateNewItem = isAuthenticated { loginSession => implicit request =>
    Ok(
      views.html.admin.createNewItem(
        createItemForm, LocaleInfo.localeTable, Category.tableForDropDown
      )
    )
  }

  def createNewItem = isAuthenticated { loginSession => implicit request =>
    createItemForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.createNewItem.")
        BadRequest(
          views.html.admin.createNewItem(
            formWithErrors, LocaleInfo.localeTable, Category.tableForDropDown
          )
        )
      },
      newItem => {
        newItem.save()
        Redirect(
          routes.ItemMaintenance.startCreateNewItem
        ).flashing("message" -> Messages("itemIsCreated"))
      }
    )
  }

  def editItem = isAuthenticated { loginSession => implicit request =>
    Ok(views.html.admin.editItem())
  }
}
