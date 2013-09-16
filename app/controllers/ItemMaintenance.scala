package controllers

import play.api.data.Form
import controllers.I18n.I18nAware
import play.api.mvc.Controller
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models._
import play.api.i18n.{Lang, Messages}
import play.api.db.DB
import play.api.Play.current
import models.CreateItem
import org.postgresql.util.PSQLException
import java.sql.SQLException

object ItemMaintenance extends Controller with I18nAware with NeedLogin with HasLogger {
  val createItemForm = Form(
    mapping(
      "langId" -> longNumber,
      "siteId" -> longNumber,
      "categoryId" -> longNumber,
      "itemName" -> text.verifying(nonEmpty, maxLength(255)),
      "taxId" -> longNumber,
      "currencyId" -> longNumber,
      "price" -> bigDecimal.verifying(min(BigDecimal(0))),
      "description" -> text.verifying(maxLength(500))
    ) (CreateItem.apply)(CreateItem.unapply)
  )

  def index = isAuthenticated { loginSession => implicit request =>
    Ok(views.html.admin.itemMaintenance())
  }

  def startCreateNewItem = isAuthenticated { loginSession => implicit request =>
    DB.withConnection { implicit conn =>
      Ok(
        views.html.admin.createNewItem(
          createItemForm, LocaleInfo.localeTable, Category.tableForDropDown,
          Site.tableForDropDown, Tax.tableForDropDown, CurrencyInfo.tableForDropDown
        )
      )
    }
  }

  def createNewItem = isAuthenticated { loginSession => implicit request =>
    createItemForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.createNewItem.")
        BadRequest(
          DB.withConnection { implicit conn =>
            views.html.admin.createNewItem(
              formWithErrors, LocaleInfo.localeTable, Category.tableForDropDown,
              Site.tableForDropDown, Tax.tableForDropDown, CurrencyInfo.tableForDropDown
            )
          }
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

  def editItem(start: Int, size: Int, q: String) = isAuthenticated { loginSession => implicit request =>
    DB.withConnection { implicit conn => {
      val list = Item.list(locale = LocaleInfo.byLang(lang), queryString = q, page = start, pageSize = size)
      Ok(views.html.admin.editItem(q, list))
    }}
  }

  def siteListAsMap: Map[Long, Site] = {
    DB.withConnection { implicit conn => {
      Site.listAsMap
    }}
  }

  def startChangeItem(id: Long) = isAuthenticated { loginSession => implicit request =>
    Ok(views.html.admin.changeItem(
      id,
      siteListAsMap,
      LocaleInfo.localeTable,
      createItemNameTable(id),
      addItemNameForm,
      createSiteTable,
      createSiteItemTable(id),
      addSiteItemForm,
      createItemCategoryForm(id),
      createCategoryTable,
      createItemDescriptionTable(id),
      addItemDescriptionForm
    ))
  }

  val changeItemNameForm = Form(
    mapping(
      "itemNames" -> seq(
        mapping(
          "localeId" -> longNumber,
          "itemName" -> text.verifying(nonEmpty, maxLength(255))
        ) (ChangeItemName.apply)(ChangeItemName.unapply)
      )
    ) (ChangeItemNameTable.apply)(ChangeItemNameTable.unapply)
  )

  val addItemNameForm = Form(
    mapping(
      "localeId" -> longNumber,
      "itemName" -> text.verifying(nonEmpty, maxLength(255))
    ) (ChangeItemName.apply)(ChangeItemName.unapply)
  )

  def createItemNameTable(id: Long): Form[ChangeItemNameTable] = {
    DB.withConnection { implicit conn => {
      val itemNames = ItemName.list(id).values.map {
        n => ChangeItemName(n.localeId, n.name)
      }.toSeq

      changeItemNameForm.fill(ChangeItemNameTable(itemNames))
    }}
  }

  def changeItemName(id: Long) = isAuthenticated { loginSession => implicit request =>
    changeItemNameForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.changeItem.")
        BadRequest(
          views.html.admin.changeItem(
            id,
            siteListAsMap,
            LocaleInfo.localeTable,
            formWithErrors,
            addItemNameForm,
            createSiteTable,
            createSiteItemTable(id),
            addSiteItemForm,
            createItemCategoryForm(id),
            createCategoryTable,
            createItemDescriptionTable(id),
            addItemDescriptionForm
          )
        )
      },
      newItem => {
        newItem.update(id)
        Redirect(
          routes.ItemMaintenance.startChangeItem(id)
        ).flashing("message" -> Messages("itemIsUpdated"))
      }
    )
  }

  def addItemName(id: Long) = isAuthenticated { loginSession => implicit request =>
    addItemNameForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.changeItem.")
        BadRequest(
          views.html.admin.changeItem(
            id,
            siteListAsMap,
            LocaleInfo.localeTable,
            createItemNameTable(id),
            formWithErrors,
            createSiteTable,
            createSiteItemTable(id),
            addSiteItemForm,
            createItemCategoryForm(id),
            createCategoryTable,
            createItemDescriptionTable(id),
            addItemDescriptionForm
          )
        )
      },
      newItem => {
        try {
          newItem.add(id)

          Redirect(
            routes.ItemMaintenance.startChangeItem(id)
          ).flashing("message" -> Messages("itemIsUpdated"))
        }
        catch {
          case e: UniqueConstraintException => {
            BadRequest(
              views.html.admin.changeItem(
                id,
                siteListAsMap,
                LocaleInfo.localeTable,
                createItemNameTable(id),
                addItemNameForm.fill(newItem).withError("localeId", "unique.constraint.violation"),
                createSiteTable,
                createSiteItemTable(id),
                addSiteItemForm,
                createItemCategoryForm(id),
                createCategoryTable,
                createItemDescriptionTable(id),
                addItemDescriptionForm
              )
            )
          }
        }
      }
    )
  }

  def removeItemName(itemId: Long, localeId: Long) = isAuthenticated { loginSession => implicit request =>
    DB.withConnection { implicit conn =>
      ItemName.remove(itemId, localeId)
    }

    Redirect(
      routes.ItemMaintenance.startChangeItem(itemId)
    )
  }

  val addSiteItemForm = Form(
    mapping(
      "siteId" -> longNumber
    ) (ChangeSiteItem.apply)(ChangeSiteItem.unapply)
  )

  def createSiteTable: Seq[(String, String)] = {
    DB.withConnection { implicit conn => {
      Site.tableForDropDown
    }}
  }

  def createSiteItemTable(itemId: Long): Seq[(Site, SiteItem)] = {
    DB.withConnection { implicit conn => {
      SiteItem.list(itemId)
    }}
  }

  def addSiteItem(id: Long) = isAuthenticated { loginSession => implicit request =>
    addSiteItemForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.addSiteItem.")
        BadRequest(
          views.html.admin.changeItem(
            id,
            siteListAsMap,
            LocaleInfo.localeTable,
            createItemNameTable(id),
            addItemNameForm,
            createSiteTable,
            createSiteItemTable(id),
            formWithErrors,
            createItemCategoryForm(id),
            createCategoryTable,
            createItemDescriptionTable(id),
            addItemDescriptionForm
          )
        )
      },
      newSiteItem => {
        try {
          newSiteItem.add(id)

          Redirect(
            routes.ItemMaintenance.startChangeItem(id)
          ).flashing("message" -> Messages("itemIsUpdated"))
        }
        catch {
          case e: UniqueConstraintException => {
            BadRequest(
              views.html.admin.changeItem(
                id,
                siteListAsMap,
                LocaleInfo.localeTable,
                createItemNameTable(id),
                addItemNameForm,
                createSiteTable,
                createSiteItemTable(id),
                addSiteItemForm.fill(newSiteItem).withError("siteId", "unique.constraint.violation"),
                createItemCategoryForm(id),
                createCategoryTable,
                createItemDescriptionTable(id),
                addItemDescriptionForm
              )
            )
          }
        }
      }
    )
  }

  def removeSiteItem(itemId: Long, siteId: Long) = isAuthenticated { loginSession => implicit request =>
    DB.withConnection { implicit conn =>
      SiteItem.remove(itemId, siteId)
    }

    Redirect(
      routes.ItemMaintenance.startChangeItem(itemId)
    )
  }

  val updateCategoryForm = Form(
    mapping(
      "categoryId" -> longNumber
    ) (ChangeItemCategory.apply)(ChangeItemCategory.unapply)
  )

  def createItemCategoryForm(id: Long): Form[ChangeItemCategory] = {
    DB.withConnection { implicit conn => {
      val item = Item(id)
      updateCategoryForm.fill(ChangeItemCategory(item.categoryId))
    }}
  }

  def createCategoryTable(implicit lang: Lang): Seq[(String, String)] = {
    DB.withConnection { implicit conn => {
      Category.tableForDropDown
    }}
  }

  def updateItemCategory(id: Long) = isAuthenticated { loginSession => implicit request =>
    updateCategoryForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.updateItemCategory.")
        BadRequest(
          views.html.admin.changeItem(
            id,
            siteListAsMap,
            LocaleInfo.localeTable,
            createItemNameTable(id),
            addItemNameForm,
            createSiteTable,
            createSiteItemTable(id),
            addSiteItemForm,
            formWithErrors,
            createCategoryTable,
            createItemDescriptionTable(id),
            addItemDescriptionForm
          )
        )
      },
      newItemCategory => {
        newItemCategory.update(id)
          Redirect(
            routes.ItemMaintenance.startChangeItem(id)
          ).flashing("message" -> Messages("itemIsUpdated"))
      }
    )
  }

  val changeItemDescriptionForm = Form(
    mapping(
      "itemDescriptions" -> seq(
        mapping(
          "siteId" -> longNumber,
          "localeId" -> longNumber,
          "itemDescription" -> text.verifying(nonEmpty, maxLength(255))
        ) (ChangeItemDescription.apply)(ChangeItemDescription.unapply)
      )
    ) (ChangeItemDescriptionTable.apply)(ChangeItemDescriptionTable.unapply)
  )

  val addItemDescriptionForm = Form(
    mapping(
      "siteId" -> longNumber,
      "localeId" -> longNumber,
      "itemDescription" -> text.verifying(nonEmpty, maxLength(255))
    ) (ChangeItemDescription.apply)(ChangeItemDescription.unapply)
  )

  def createItemDescriptionTable(id: Long): Form[ChangeItemDescriptionTable] = {
    DB.withConnection { implicit conn => {
      val itemDescriptions = ItemDescription.list(id).map {
        n => ChangeItemDescription(n._1, n._2.id, n._3.description)
      }.toSeq

      changeItemDescriptionForm.fill(ChangeItemDescriptionTable(itemDescriptions))
    }}
  }

  def changeItemDescription(id: Long) = isAuthenticated { loginSession => implicit request =>
    changeItemDescriptionForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.changeItem.")
        BadRequest(
          views.html.admin.changeItem(
            id,
            siteListAsMap,
            LocaleInfo.localeTable,
            createItemNameTable(id),
            addItemNameForm,
            createSiteTable,
            createSiteItemTable(id),
            addSiteItemForm,
            createItemCategoryForm(id),
            createCategoryTable,
            formWithErrors,
            addItemDescriptionForm
          )
        )
      },
      newItem => {
        newItem.update(id)
        Redirect(
          routes.ItemMaintenance.startChangeItem(id)
        ).flashing("message" -> Messages("itemIsUpdated"))
      }
    )
  }

  def addItemDescription(id: Long) = isAuthenticated { loginSession => implicit request =>
    addItemDescriptionForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.changeItem.")
        BadRequest(
          views.html.admin.changeItem(
            id,
            siteListAsMap,
            LocaleInfo.localeTable,
            createItemNameTable(id),
            addItemNameForm,
            createSiteTable,
            createSiteItemTable(id),
            addSiteItemForm,
            createItemCategoryForm(id),
            createCategoryTable,
            createItemDescriptionTable(id),
            formWithErrors
          )
        )
      },
      newItem => {
        try {
          newItem.add(id)

          Redirect(
            routes.ItemMaintenance.startChangeItem(id)
          ).flashing("message" -> Messages("itemIsUpdated"))
        }
        catch {
          case e: UniqueConstraintException => {
            BadRequest(
              views.html.admin.changeItem(
                id,
                siteListAsMap,
                LocaleInfo.localeTable,
                createItemNameTable(id),
                addItemNameForm,
                createSiteTable,
                createSiteItemTable(id),
                addSiteItemForm,
                createItemCategoryForm(id),
                createCategoryTable,
                createItemDescriptionTable(id),
                addItemDescriptionForm
                  .fill(newItem)
                  .withError("localeId", "unique.constraint.violation")
                  .withError("siteId", "unique.constraint.violation")
              )
            )
          }
        }
      }
    )
  }

  def removeItemDescription(
    siteId: Long, itemId: Long, localeId: Long
  ) = isAuthenticated { loginSession => implicit request =>
    DB.withConnection { implicit conn =>
      ItemDescription.remove(siteId, itemId, localeId)
    }

    Redirect(
      routes.ItemMaintenance.startChangeItem(itemId)
    )
  }
}
