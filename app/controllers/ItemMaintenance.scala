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
import org.joda.time.DateTime
import helpers.QueryString

class ChangeItem(
  val id: Long,
  val siteMap: Map[Long, Site],
  val langTable: Seq[(String, String)],
  val itemNameTableForm: Form[ChangeItemNameTable],
  val newItemNameForm: Form[ChangeItemName],
  val siteNameTable: Seq[(String, String)],
  val siteItemTable: Seq[(Site, SiteItem)],
  val newSiteItemForm: Form[ChangeSiteItem],
  val updateCategoryForm: Form[ChangeItemCategory],
  val categoryTable: Seq[(String, String)],
  val itemDescriptionTableForm: Form[ChangeItemDescriptionTable],
  val newItemDescriptionForm: Form[ChangeItemDescription],
  val itemPriceTableForm: Form[ChangeItemPriceTable],
  val newItemPriceForm: Form[ChangeItemPrice],
  val taxTable: Seq[(String, String)],
  val currencyTable: Seq[(String, String)],
  val itemInSiteTable: Seq[(String, String)],
  val itemMetadataTableForm: Form[ChangeItemMetadataTable],
  val newItemMetadataForm: Form[ChangeItemMetadata],
  val siteItemMetadataTableForm: Form[ChangeSiteItemMetadataTable],
  val newSiteItemMetadataForm: Form[ChangeSiteItemMetadata],
  val siteItemTextMetadataTableForm: Form[ChangeSiteItemTextMetadataTable],
  val newSiteItemTextMetadataForm: Form[ChangeSiteItemTextMetadata],
  val itemTextMetadataTableForm: Form[ChangeItemTextMetadataTable],
  val newItemTextMetadataForm: Form[ChangeItemTextMetadata],
  val attachmentNames: Map[Int, String],
  val couponForm: Form[ChangeCoupon]
)

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
      "listPrice" -> optional(bigDecimal.verifying(min(BigDecimal(0)))),
      "costPrice" -> bigDecimal.verifying(min(BigDecimal(0))),
      "description" -> text.verifying(maxLength(2048)),
      "isCoupon" ->boolean
    ) (CreateItem.apply)(CreateItem.unapply)
  )

  def index = isAuthenticated { implicit login => forAdmin { implicit request =>
    Ok(views.html.admin.itemMaintenance())
  }}

  def startCreateNewItem = isAuthenticated { implicit login => forAdmin { implicit request =>
    DB.withConnection { implicit conn =>
      Ok(
        views.html.admin.createNewItem(
          createItemForm, LocaleInfo.localeTable, Category.tableForDropDown,
          if (login.isSuperUser) Site.tableForDropDown
          else {
            val site = Site(login.siteUser.get.siteId)
            List((site.id.get.toString, site.name))
          },
          Tax.tableForDropDown, CurrencyInfo.tableForDropDown
        )
      )
    }
  }}

  def createNewItem = isAuthenticated { implicit login => forAdmin { implicit request =>
    createItemForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.createNewItem." + formWithErrors + ".")
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
        DB.withConnection { implicit conn =>
          newItem.save()
        }
        Redirect(
          routes.ItemMaintenance.startCreateNewItem
        ).flashing("message" -> Messages("itemIsCreated"))
      }
    )
  }}

  def editItem(
    qs: List[String], pgStart: Int, pgSize: Int, orderBySpec: String
  ) = isAuthenticated { implicit login => forAdmin { implicit request =>
    val queryStr = if (qs.size == 1) QueryString(qs.head) else QueryString(qs.filter {! _.isEmpty})
    DB.withConnection { implicit conn => {
      login.role match {
        case Buyer => throw new Error("Logic error.")

        case SuperUser =>
          val list = Item.listForMaintenance(
            siteUser = None, locale = LocaleInfo.byLang(lang), queryString = queryStr, page = pgStart,
            pageSize = pgSize, orderBy = OrderBy(orderBySpec)
          )

          Ok(views.html.admin.editItem(queryStr, list))

        case SiteOwner(siteOwner) =>
          val list = Item.listForMaintenance(
            siteUser = Some(siteOwner), locale = LocaleInfo.byLang(lang), queryString = queryStr, page = pgStart,
            pageSize = pgSize, orderBy = OrderBy(orderBySpec)
          )
          Ok(views.html.admin.editItem(queryStr, list))
      }
    }}
  }}

  def siteListAsMap: Map[Long, Site] = {
    DB.withConnection { implicit conn => {
      Site.listAsMap
    }}
  }

  def taxTable(implicit lang: Lang): Seq[(String, String)] = DB.withConnection { implicit conn =>
    Tax.tableForDropDown
  }

  def currencyTable: Seq[(String, String)] = DB.withConnection { implicit conn =>
    CurrencyInfo.tableForDropDown
  }

  def itemMetadataTable(implicit lang: Lang): Seq[(String, String)] = ItemNumericMetadataType.all.map {
    e => (e.ordinal.toString, Messages("itemNumericMetadata" + e.toString))
  }

  def itemTextMetadataTable(implicit lang: Lang): Seq[(String, String)] = ItemTextMetadataType.all.map {
    e => (e.ordinal.toString, Messages("itemTextMetadata" + e.toString))
  }

  def siteItemMetadataTable(implicit lang: Lang): Seq[(String, String)] = SiteItemNumericMetadataType.all.map {
    e => (e.ordinal.toString, Messages("siteItemMetadata" + e.toString))
  }

  def siteItemTextMetadataTable(implicit lang: Lang): Seq[(String, String)] = SiteItemTextMetadataType.all.map {
    e => (e.ordinal.toString, Messages("siteItemTextMetadata" + e.toString))
  }

  def startChangeItem(id: Long) = isAuthenticated { implicit login => forAdmin { implicit request =>
    Ok(views.html.admin.changeItem(
      new ChangeItem(
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
        addItemDescriptionForm,
        createItemPriceTable(id),
        addItemPriceForm,
        taxTable,
        currencyTable,
        createSiteTable(id),
        createItemMetadataTable(id),
        addItemMetadataForm,
        createSiteItemMetadataTable(id),
        addSiteItemMetadataForm,
        createSiteItemTextMetadataTable(id),
        addSiteItemTextMetadataForm,
        createItemTextMetadataTable(id),
        addItemTextMetadataForm,
        ItemPictures.retrieveAttachmentNames(id),
        createCouponForm(ItemId(id))
      )
    ))
  }}

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

  val changeItemMetadataForm = Form(
    mapping(
      "itemMetadatas" -> seq(
        mapping(
          "metadataType" -> number,
          "metadata" -> longNumber
        ) (ChangeItemMetadata.apply)(ChangeItemMetadata.unapply)
      )
    ) (ChangeItemMetadataTable.apply)(ChangeItemMetadataTable.unapply)
  )

  val changeItemTextMetadataForm = Form(
    mapping(
      "itemMetadatas" -> seq(
        mapping(
          "metadataType" -> number,
          "metadata" -> text
        ) (ChangeItemTextMetadata.apply)(ChangeItemTextMetadata.unapply)
      )
    ) (ChangeItemTextMetadataTable.apply)(ChangeItemTextMetadataTable.unapply)
  )

  val changeSiteItemMetadataForm = Form(
    mapping(
      "siteItemMetadatas" -> seq(
        mapping(
          "siteId" -> longNumber,
          "metadataType" -> number,
          "metadata" -> longNumber
        ) (ChangeSiteItemMetadata.apply)(ChangeSiteItemMetadata.unapply)
      )
    ) (ChangeSiteItemMetadataTable.apply)(ChangeSiteItemMetadataTable.unapply)
  )

  val changeSiteItemTextMetadataForm = Form(
    mapping(
      "siteItemTextMetadatas" -> seq(
        mapping(
          "siteId" -> longNumber,
          "metadataType" -> number,
          "metadata" -> text
        ) (ChangeSiteItemTextMetadata.apply)(ChangeSiteItemTextMetadata.unapply)
      )
    ) (ChangeSiteItemTextMetadataTable.apply)(ChangeSiteItemTextMetadataTable.unapply)
  )

  val addItemNameForm = Form(
    mapping(
      "localeId" -> longNumber,
      "itemName" -> text.verifying(nonEmpty, maxLength(255))
    ) (ChangeItemName.apply)(ChangeItemName.unapply)
  )

  val addItemMetadataForm = Form(
    mapping(
      "metadataType" -> number,
      "metadata" -> longNumber
    ) (ChangeItemMetadata.apply)(ChangeItemMetadata.unapply)
  )

  val addItemTextMetadataForm = Form(
    mapping(
      "metadataType" -> number,
      "metadata" -> text
    ) (ChangeItemTextMetadata.apply)(ChangeItemTextMetadata.unapply)
  )

  val addSiteItemMetadataForm = Form(
    mapping(
      "siteId" -> longNumber,
      "metadataType" -> number,
      "metadata" -> longNumber
    ) (ChangeSiteItemMetadata.apply)(ChangeSiteItemMetadata.unapply)
  )

  val addSiteItemTextMetadataForm = Form(
    mapping(
      "siteId" -> longNumber,
      "metadataType" -> number,
      "metadata" -> text
    ) (ChangeSiteItemTextMetadata.apply)(ChangeSiteItemTextMetadata.unapply)
  )

  val couponForm = Form(
    mapping(
      "isCoupon" ->boolean
    ) (ChangeCoupon.apply)(ChangeCoupon.unapply)
  )

  def createItemNameTable(id: Long): Form[ChangeItemNameTable] = {
    DB.withConnection { implicit conn => {
      val itemNames = ItemName.list(ItemId(id)).values.map {
        n => ChangeItemName(n.localeId, n.name)
      }.toSeq

      changeItemNameForm.fill(ChangeItemNameTable(itemNames))
    }}
  }

  def createItemMetadataTable(id: Long): Form[ChangeItemMetadataTable] = {
    DB.withConnection { implicit conn => {
      val itemMetadatas = ItemNumericMetadata.allById(ItemId(id)).values.map {
        n => ChangeItemMetadata(n.metadataType.ordinal, n.metadata)
      }.toSeq

      changeItemMetadataForm.fill(ChangeItemMetadataTable(itemMetadatas))
    }}
  }

  def createItemTextMetadataTable(id: Long): Form[ChangeItemTextMetadataTable] = {
    DB.withConnection { implicit conn => {
      val itemMetadatas = ItemTextMetadata.allById(ItemId(id)).values.map {
        n => ChangeItemTextMetadata(n.metadataType.ordinal, n.metadata)
      }.toSeq

      changeItemTextMetadataForm.fill(ChangeItemTextMetadataTable(itemMetadatas))
    }}
  }

  def createSiteItemMetadataTable(id: Long): Form[ChangeSiteItemMetadataTable] = {
    DB.withConnection { implicit conn => {
      val itemMetadata = SiteItemNumericMetadata.allById(ItemId(id)).values.map {
        n => ChangeSiteItemMetadata(n.siteId, n.metadataType.ordinal, n.metadata)
      }.toSeq

      changeSiteItemMetadataForm.fill(ChangeSiteItemMetadataTable(itemMetadata))
    }}
  }

  def createSiteItemTextMetadataTable(id: Long): Form[ChangeSiteItemTextMetadataTable] = {
    DB.withConnection { implicit conn => {
      val itemMetadata = SiteItemTextMetadata.allById(ItemId(id)).values.map {
        n => ChangeSiteItemTextMetadata(n.siteId, n.metadataType.ordinal, n.metadata)
      }.toSeq

      changeSiteItemTextMetadataForm.fill(ChangeSiteItemTextMetadataTable(itemMetadata))
    }}
  }

  def changeItemName(id: Long) = isAuthenticated { implicit login => forAdmin { implicit request =>
    changeItemNameForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.changeItemName." + formWithErrors + ".")
        BadRequest(
          views.html.admin.changeItem(
            new ChangeItem(
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
              addItemDescriptionForm,
              createItemPriceTable(id),
              addItemPriceForm,
              taxTable,
              currencyTable,
              createSiteTable(id),
              createItemMetadataTable(id),
              addItemMetadataForm,
              createSiteItemMetadataTable(id),
              addSiteItemMetadataForm,
              createSiteItemTextMetadataTable(id),
              addSiteItemTextMetadataForm,
              createItemTextMetadataTable(id),
              addItemTextMetadataForm,
              ItemPictures.retrieveAttachmentNames(id),
              createCouponForm(ItemId(id))
            )
          )
        )
      },
      newItem => {
        DB.withConnection { implicit conn =>
          newItem.update(id)
        }
        Redirect(
          routes.ItemMaintenance.startChangeItem(id)
        ).flashing("message" -> Messages("itemIsUpdated"))
      }
    )
  }}

  def addItemName(id: Long) = isAuthenticated { implicit login => forAdmin { implicit request =>
    addItemNameForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.addItemName." + formWithErrors + ".")
        BadRequest(
          views.html.admin.changeItem(
            new ChangeItem(
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
              addItemDescriptionForm,
              createItemPriceTable(id),
              addItemPriceForm,
              taxTable,
              currencyTable,
              createSiteTable(id),
              createItemMetadataTable(id),
              addItemMetadataForm,
              createSiteItemMetadataTable(id),
              addSiteItemMetadataForm,
              createSiteItemTextMetadataTable(id),
              addSiteItemTextMetadataForm,
              createItemTextMetadataTable(id),
              addItemTextMetadataForm,
              ItemPictures.retrieveAttachmentNames(id),
              createCouponForm(ItemId(id))
            )
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
                new ChangeItem(
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
                  addItemDescriptionForm,
                  createItemPriceTable(id),
                  addItemPriceForm,
                  taxTable,
                  currencyTable,
                  createSiteTable(id),
                  createItemMetadataTable(id),
                  addItemMetadataForm,
                  createSiteItemMetadataTable(id),
                  addSiteItemMetadataForm,
                  createSiteItemTextMetadataTable(id),
                  addSiteItemTextMetadataForm,
                  createItemTextMetadataTable(id),
                  addItemTextMetadataForm,
                  ItemPictures.retrieveAttachmentNames(id),
                  createCouponForm(ItemId(id))
                )
              )
            )
          }
        }
      }
    )
  }}

  def removeItemName(itemId: Long, localeId: Long) = isAuthenticated { implicit login => forAdmin { implicit request =>
    DB.withConnection { implicit conn =>
      ItemName.remove(ItemId(itemId), localeId)
    }

    Redirect(
      routes.ItemMaintenance.startChangeItem(itemId)
    )
  }}

  def removeItemMetadata(itemId: Long, metadataType: Int) = isAuthenticated { implicit login => forAdmin { implicit request =>
    DB.withConnection { implicit conn =>
      ItemNumericMetadata.remove(ItemId(itemId), metadataType)
    }

    Redirect(
      routes.ItemMaintenance.startChangeItem(itemId)
    )
  }}

  def removeItemTextMetadata(itemId: Long, metadataType: Int) = isAuthenticated { implicit login => forAdmin { implicit request =>
    DB.withConnection { implicit conn =>
      ItemTextMetadata.remove(ItemId(itemId), metadataType)
    }

    Redirect(
      routes.ItemMaintenance.startChangeItem(itemId)
    )
  }}

  def removeSiteItemMetadata(
    itemId: Long, siteId: Long, metadataType: Int
  ) = isAuthenticated { implicit login => forAdmin { implicit request =>
    DB.withConnection { implicit conn =>
      SiteItemNumericMetadata.remove(ItemId(itemId), siteId, metadataType)
    }

    Redirect(
      routes.ItemMaintenance.startChangeItem(itemId)
    )
  }}

  def removeSiteItemTextMetadata(
    itemId: Long, siteId: Long, metadataType: Int
  ) = isAuthenticated { implicit login => forAdmin { implicit request =>
    DB.withConnection { implicit conn =>
      SiteItemTextMetadata.remove(ItemId(itemId), siteId, metadataType)
    }

    Redirect(
      routes.ItemMaintenance.startChangeItem(itemId)
    )
  }}

  val addSiteItemForm = Form(
    mapping(
      "siteId" -> longNumber
    ) (ChangeSiteItem.apply)(ChangeSiteItem.unapply)
  )

  def createSiteTable(implicit login: LoginSession): Seq[(String, String)] = {
    DB.withConnection { implicit conn => {
      Site.tableForDropDown
    }}
  }

  def createSiteTable(id: Long)(implicit login: LoginSession): Seq[(String, String)] = {
    DB.withConnection { implicit conn => {
      Site.tableForDropDown(id)
    }}
  }    

  def createSiteItemTable(itemId: Long): Seq[(Site, SiteItem)] = {
    DB.withConnection { implicit conn => {
      SiteItem.list(ItemId(itemId))
    }}
  }

  def addSiteItem(id: Long) = isAuthenticated { implicit login => forAdmin { implicit request =>
    addSiteItemForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.addSiteItem." + formWithErrors + ".")
        BadRequest(
          views.html.admin.changeItem(
            new ChangeItem(
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
              addItemDescriptionForm,
              createItemPriceTable(id),
              addItemPriceForm,
              taxTable,
              currencyTable,
              createSiteTable(id),
              createItemMetadataTable(id),
              addItemMetadataForm,
              createSiteItemMetadataTable(id),
              addSiteItemMetadataForm,
              createSiteItemTextMetadataTable(id),
              addSiteItemTextMetadataForm,
              createItemTextMetadataTable(id),
              addItemTextMetadataForm,
              ItemPictures.retrieveAttachmentNames(id),
              createCouponForm(ItemId(id))
            )
          )
        )
      },
      newSiteItem => {
        try {
          DB.withConnection { implicit conn =>
            newSiteItem.add(id)
          }

          Redirect(
            routes.ItemMaintenance.startChangeItem(id)
          ).flashing("message" -> Messages("itemIsUpdated"))
        }
        catch {
          case e: UniqueConstraintException => {
            BadRequest(
              views.html.admin.changeItem(
                new ChangeItem(
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
                  addItemDescriptionForm,
                  createItemPriceTable(id),
                  addItemPriceForm,
                  taxTable,
                  currencyTable,
                  createSiteTable(id),
                  createItemMetadataTable(id),
                  addItemMetadataForm,
                  createSiteItemMetadataTable(id),
                  addSiteItemMetadataForm,
                  createSiteItemTextMetadataTable(id),
                  addSiteItemTextMetadataForm,
                  createItemTextMetadataTable(id),
                  addItemTextMetadataForm,
                  ItemPictures.retrieveAttachmentNames(id),
                  createCouponForm(ItemId(id))
                )
              )
            )
          }
        }
      }
    )
  }}

  def removeSiteItem(itemId: Long, siteId: Long) = isAuthenticated { implicit login => forAdmin { implicit request =>
    DB.withConnection { implicit conn =>
      SiteItem.remove(ItemId(itemId), siteId)
      ItemPrice.remove(ItemId(itemId), siteId)
    }

    Redirect(
      routes.ItemMaintenance.startChangeItem(itemId)
    )
  }}

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

  def createCouponForm(id: ItemId): Form[ChangeCoupon] = {
    DB.withConnection { implicit conn => {
      couponForm.fill(ChangeCoupon(Coupon.isCoupon(id)))
    }}
  }

  def createCategoryTable(implicit lang: Lang): Seq[(String, String)] = {
    DB.withConnection { implicit conn => {
      Category.tableForDropDown
    }}
  }

  def updateItemAsCoupon(id: Long) = isAuthenticated { implicit login => forAdmin { implicit request =>
    couponForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.updateItemAsItem." + formWithErrors + ".")
        BadRequest(
          views.html.admin.changeItem(
            new ChangeItem(
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
              addItemDescriptionForm,
              createItemPriceTable(id),
              addItemPriceForm,
              taxTable,
              currencyTable,
              createSiteTable(id),
              createItemMetadataTable(id),
              addItemMetadataForm,
              createSiteItemMetadataTable(id),
              addSiteItemMetadataForm,
              createSiteItemTextMetadataTable(id),
              addSiteItemTextMetadataForm,
              createItemTextMetadataTable(id),
              addItemTextMetadataForm,
              ItemPictures.retrieveAttachmentNames(id),
              formWithErrors
            )
          )
        )
      },
      newIsCoupon => {
        DB.withConnection { implicit conn =>
          newIsCoupon.update(ItemId(id))
        }
        Redirect(
          routes.ItemMaintenance.startChangeItem(id)
        ).flashing("message" -> Messages("itemIsUpdated"))
      }
    )
  }}

  def updateItemCategory(id: Long) = isAuthenticated { implicit login => forAdmin { implicit request =>
    updateCategoryForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.updateItemCategory." + formWithErrors + ".")
        BadRequest(
          views.html.admin.changeItem(
            new ChangeItem(
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
              addItemDescriptionForm,
              createItemPriceTable(id),
              addItemPriceForm,
              taxTable,
              currencyTable,
              createSiteTable(id),
              createItemMetadataTable(id),
              addItemMetadataForm,
              createSiteItemMetadataTable(id),
              addSiteItemMetadataForm,
              createSiteItemTextMetadataTable(id),
              addSiteItemTextMetadataForm,
              createItemTextMetadataTable(id),
              addItemTextMetadataForm,
              ItemPictures.retrieveAttachmentNames(id),
              createCouponForm(ItemId(id))
            )
          )
        )
      },
      newItemCategory => {
        DB.withConnection { implicit conn =>
          newItemCategory.update(id)
        }
        Redirect(
          routes.ItemMaintenance.startChangeItem(id)
        ).flashing("message" -> Messages("itemIsUpdated"))
      }
    )
  }}

  val changeItemDescriptionForm = Form(
    mapping(
      "itemDescriptions" -> seq(
        mapping(
          "siteId" -> longNumber,
          "localeId" -> longNumber,
          "itemDescription" -> text.verifying(nonEmpty, maxLength(2048))
        ) (ChangeItemDescription.apply)(ChangeItemDescription.unapply)
      )
    ) (ChangeItemDescriptionTable.apply)(ChangeItemDescriptionTable.unapply)
  )

  val addItemDescriptionForm = Form(
    mapping(
      "siteId" -> longNumber,
      "localeId" -> longNumber,
      "itemDescription" -> text.verifying(nonEmpty, maxLength(2048))
    ) (ChangeItemDescription.apply)(ChangeItemDescription.unapply)
  )

  def createItemDescriptionTable(id: Long): Form[ChangeItemDescriptionTable] = {
    DB.withConnection { implicit conn => {
      val itemDescriptions = ItemDescription.list(ItemId(id)).map {
        n => ChangeItemDescription(n._1, n._2.id, n._3.description)
      }.toSeq

      changeItemDescriptionForm.fill(ChangeItemDescriptionTable(itemDescriptions))
    }}
  }

  def changeItemDescription(id: Long) = isAuthenticated { implicit login => forAdmin { implicit request =>
    changeItemDescriptionForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.changeItem." + formWithErrors + ".")
        BadRequest(
          views.html.admin.changeItem(
            new ChangeItem(
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
              addItemDescriptionForm,
              createItemPriceTable(id),
              addItemPriceForm,
              taxTable,
              currencyTable,
              createSiteTable(id),
              createItemMetadataTable(id),
              addItemMetadataForm,
              createSiteItemMetadataTable(id),
              addSiteItemMetadataForm,
              createSiteItemTextMetadataTable(id),
              addSiteItemTextMetadataForm,
              createItemTextMetadataTable(id),
              addItemTextMetadataForm,
              ItemPictures.retrieveAttachmentNames(id),
              createCouponForm(ItemId(id))
            )
          )
        )
      },
      newItem => {
        DB.withTransaction { implicit conn =>
          newItem.update(id)
        }
        Redirect(
          routes.ItemMaintenance.startChangeItem(id)
        ).flashing("message" -> Messages("itemIsUpdated"))
      }
    )
  }}

  def addItemDescription(id: Long) = isAuthenticated { implicit login => forAdmin { implicit request =>
    addItemDescriptionForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.changeItem." + formWithErrors + ".")
        BadRequest(
          views.html.admin.changeItem(
            new ChangeItem(
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
              formWithErrors,
              createItemPriceTable(id),
              addItemPriceForm,
              taxTable,
              currencyTable,
              createSiteTable(id),
              createItemMetadataTable(id),
              addItemMetadataForm,
              createSiteItemMetadataTable(id),
              addSiteItemMetadataForm,
              createSiteItemTextMetadataTable(id),
              addSiteItemTextMetadataForm,
              createItemTextMetadataTable(id),
              addItemTextMetadataForm,
              ItemPictures.retrieveAttachmentNames(id),
              createCouponForm(ItemId(id))
            )
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
                new ChangeItem(
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
                    .withError("siteId", "unique.constraint.violation"),
                  createItemPriceTable(id),
                  addItemPriceForm,
                  taxTable,
                  currencyTable,
                  createSiteTable(id),
                  createItemMetadataTable(id),
                  addItemMetadataForm,
                  createSiteItemMetadataTable(id),
                  addSiteItemMetadataForm,
                  createSiteItemTextMetadataTable(id),
                  addSiteItemTextMetadataForm,
                  createItemTextMetadataTable(id),
                  addItemTextMetadataForm,
                  ItemPictures.retrieveAttachmentNames(id),
                  createCouponForm(ItemId(id))
                )
              )
            )
          }
        }
      }
    )
  }}

  def removeItemDescription(
    siteId: Long, itemId: Long, localeId: Long
  ) = isAuthenticated { implicit login => forAdmin { implicit request =>
    DB.withConnection { implicit conn =>
      ItemDescription.remove(siteId, ItemId(itemId), localeId)
    }

    Redirect(
      routes.ItemMaintenance.startChangeItem(itemId)
    )
  }}

  val changeItemPriceForm = Form(
    mapping(
      "itemPrices" -> seq(
        mapping(
          "siteId" -> longNumber,
          "itemPriceId" -> longNumber,
          "itemPriceHistoryId" -> longNumber,
          "taxId" -> longNumber,
          "currencyId" -> longNumber,
          "itemPrice" -> bigDecimal.verifying(min(BigDecimal(0))),
          "listPrice" -> optional(bigDecimal.verifying(min(BigDecimal(0)))),
          "costPrice" -> bigDecimal.verifying(min(BigDecimal(0))),
          "validUntil" -> jodaDate("yyyy-MM-dd HH:mm:ss")
        ) (ChangeItemPrice.apply)(ChangeItemPrice.unapply)
      )
    ) (ChangeItemPriceTable.apply)(ChangeItemPriceTable.unapply)
  )

  val addItemPriceForm = Form(
    mapping(
      "siteId" -> longNumber,
      "itemPriceId" -> ignored(0L),
      "itemPriceHistoryId" -> ignored(0L),
      "taxId" -> longNumber,
      "currencyId" -> longNumber,
      "itemPrice" -> bigDecimal.verifying(min(BigDecimal(0))),
      "listPrice" -> optional(bigDecimal.verifying(min(BigDecimal(0)))),
      "costPrice" -> bigDecimal.verifying(min(BigDecimal(0))),
      "validUntil" -> jodaDate("yyyy-MM-dd HH:mm:ss")
    ) (ChangeItemPrice.apply)(ChangeItemPrice.unapply)
  )

  def createItemPriceTable(itemId: Long): Form[ChangeItemPriceTable] = {
    DB.withConnection { implicit conn => {
      val histories = ItemPriceHistory.listByItemId(ItemId(itemId)).map {
        e => ChangeItemPrice(
          e._1.siteId, e._2.itemPriceId, e._2.id.get, e._2.taxId, 
          e._2.currency.id, e._2.unitPrice, e._2.listPrice, e._2.costPrice, new DateTime(e._2.validUntil)
        )
      }.toSeq

      changeItemPriceForm.fill(ChangeItemPriceTable(histories))
    }}
  }

  def changeItemPrice(id: Long) = isAuthenticated { implicit login => forAdmin { implicit request =>
    changeItemPriceForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.changeItemPrice." + formWithErrors + ".")
        BadRequest(
          views.html.admin.changeItem(
            new ChangeItem(
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
              addItemDescriptionForm,
              formWithErrors,
              addItemPriceForm,
              taxTable,
              currencyTable,
              createSiteTable(id),
              createItemMetadataTable(id),
              addItemMetadataForm,
              createSiteItemMetadataTable(id),
              addSiteItemMetadataForm,
              createSiteItemTextMetadataTable(id),
              addSiteItemTextMetadataForm,
              createItemTextMetadataTable(id),
              addItemTextMetadataForm,
              ItemPictures.retrieveAttachmentNames(id),
              createCouponForm(ItemId(id))
            )
          )
        )
      },
      newPrice => {
        DB.withConnection { implicit conn =>
          newPrice.update()
        }
        Redirect(
          routes.ItemMaintenance.startChangeItem(id)
        ).flashing("message" -> Messages("itemIsUpdated"))
      }
    )
  }}

  def addItemPrice(id: Long) = isAuthenticated { implicit login => forAdmin { implicit request =>
    addItemPriceForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.addItemPrice " + formWithErrors + ".")
        BadRequest(
          views.html.admin.changeItem(
            new ChangeItem(
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
              addItemDescriptionForm,
              createItemPriceTable(id),
              formWithErrors,
              taxTable,
              currencyTable,
              createSiteTable(id),
              createItemMetadataTable(id),
              addItemMetadataForm,
              createSiteItemMetadataTable(id),
              addSiteItemMetadataForm,
              createSiteItemTextMetadataTable(id),
              addSiteItemTextMetadataForm,
              createItemTextMetadataTable(id),
              addItemTextMetadataForm,
              ItemPictures.retrieveAttachmentNames(id),
              createCouponForm(ItemId(id))
            )
          )
        )
      },
      newHistory => {
        try {
          DB.withConnection { implicit conn =>
            newHistory.add(id)
          }
          Redirect(
            routes.ItemMaintenance.startChangeItem(id)
          ).flashing("message" -> Messages("itemIsUpdated"))
        }
        catch {
          case e: UniqueConstraintException => {
            BadRequest(
              views.html.admin.changeItem(
                new ChangeItem(
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
                  addItemDescriptionForm,
                  createItemPriceTable(id),
                  addItemPriceForm
                    .fill(newHistory)
                    .withError("siteId", "unique.constraint.violation")
                    .withError("validUntil", "unique.constraint.violation"),
                  taxTable,
                  currencyTable,
                  createSiteTable(id),
                  createItemMetadataTable(id),
                  addItemMetadataForm,
                  createSiteItemMetadataTable(id),
                  addSiteItemMetadataForm,
                  createSiteItemTextMetadataTable(id),
                  addSiteItemTextMetadataForm,
                  createItemTextMetadataTable(id),
                  addItemTextMetadataForm,
                  ItemPictures.retrieveAttachmentNames(id),
                  createCouponForm(ItemId(id))
                )
              )
            )
          }
        }
      }
    )
  }}

  def removeItemPrice(
    itemId: Long, siteId: Long, itemPriceHistoryId: Long
  ) = isAuthenticated { implicit login => forAdmin { implicit request =>
    DB.withConnection { implicit conn =>
      ItemPriceHistory.remove(ItemId(itemId), siteId, itemPriceHistoryId)
    }

    Redirect(
      routes.ItemMaintenance.startChangeItem(itemId)
    )
  }}

  def changeItemMetadata(itemId: Long) = isAuthenticated { implicit login => forAdmin { implicit request =>
    changeItemMetadataForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.changeItemMetadata." + formWithErrors + ".")
        BadRequest(
          views.html.admin.changeItem(
            new ChangeItem(
              itemId,
              siteListAsMap,
              LocaleInfo.localeTable,
              createItemNameTable(itemId),
              addItemNameForm,
              createSiteTable,
              createSiteItemTable(itemId),
              addSiteItemForm,
              createItemCategoryForm(itemId),
              createCategoryTable,
              createItemDescriptionTable(itemId),
              addItemDescriptionForm,
              createItemPriceTable(itemId),
              addItemPriceForm,
              taxTable,
              currencyTable,
              createSiteTable(itemId),
              formWithErrors,
              addItemMetadataForm,
              createSiteItemMetadataTable(itemId),
              addSiteItemMetadataForm,
              createSiteItemTextMetadataTable(itemId),
              addSiteItemTextMetadataForm,
              createItemTextMetadataTable(itemId),
              addItemTextMetadataForm,
              ItemPictures.retrieveAttachmentNames(itemId),
              createCouponForm(ItemId(itemId))
            )
          )
        )
      },
      newMetadata => {
        DB.withTransaction { implicit conn =>
          newMetadata.update(itemId)
        }
        Redirect(
          routes.ItemMaintenance.startChangeItem(itemId)
        ).flashing("message" -> Messages("itemIsUpdated"))
      }
    )
  }}

  def changeItemTextMetadata(itemId: Long) = isAuthenticated { implicit login => forAdmin { implicit request =>
    changeItemTextMetadataForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.changeItemTextMetadata." + formWithErrors + ".")
        BadRequest(
          views.html.admin.changeItem(
            new ChangeItem(
              itemId,
              siteListAsMap,
              LocaleInfo.localeTable,
              createItemNameTable(itemId),
              addItemNameForm,
              createSiteTable,
              createSiteItemTable(itemId),
              addSiteItemForm,
              createItemCategoryForm(itemId),
              createCategoryTable,
              createItemDescriptionTable(itemId),
              addItemDescriptionForm,
              createItemPriceTable(itemId),
              addItemPriceForm,
              taxTable,
              currencyTable,
              createSiteTable(itemId),
              createItemMetadataTable(itemId),
              addItemMetadataForm,
              createSiteItemMetadataTable(itemId),
              addSiteItemMetadataForm,
              createSiteItemTextMetadataTable(itemId),
              addSiteItemTextMetadataForm,
              formWithErrors,
              addItemTextMetadataForm,
              ItemPictures.retrieveAttachmentNames(itemId),
              createCouponForm(ItemId(itemId))
            )
          )
        )
      },
      newMetadata => {
        DB.withTransaction { implicit conn =>
          newMetadata.update(itemId)
        }
        Redirect(
          routes.ItemMaintenance.startChangeItem(itemId)
        ).flashing("message" -> Messages("itemIsUpdated"))
      }
    )
  }}

  def changeSiteItemMetadata(itemId: Long) = isAuthenticated { implicit login => forAdmin { implicit request =>
    changeSiteItemMetadataForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.changeSiteItemMetadata." + formWithErrors + ".")
        BadRequest(
          views.html.admin.changeItem(
            new ChangeItem(
              itemId,
              siteListAsMap,
              LocaleInfo.localeTable,
              createItemNameTable(itemId),
              addItemNameForm,
              createSiteTable,
              createSiteItemTable(itemId),
              addSiteItemForm,
              createItemCategoryForm(itemId),
              createCategoryTable,
              createItemDescriptionTable(itemId),
              addItemDescriptionForm,
              createItemPriceTable(itemId),
              addItemPriceForm,
              taxTable,
              currencyTable,
              createSiteTable(itemId),
              createItemMetadataTable(itemId),
              addItemMetadataForm,
              formWithErrors,
              addSiteItemMetadataForm,
              createSiteItemTextMetadataTable(itemId),
              addSiteItemTextMetadataForm,
              createItemTextMetadataTable(itemId),
              addItemTextMetadataForm,
              ItemPictures.retrieveAttachmentNames(itemId),
              createCouponForm(ItemId(itemId))
            )
          )
        )
      },
      newMetadata => {
        DB.withConnection { implicit conn =>
          newMetadata.update(itemId)
        }
        Redirect(
          routes.ItemMaintenance.startChangeItem(itemId)
        ).flashing("message" -> Messages("itemIsUpdated"))
      }
    )
  }}

  def changeSiteItemTextMetadata(itemId: Long) = isAuthenticated { implicit login => forAdmin { implicit request =>
    changeSiteItemTextMetadataForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.changeSiteItemTextMetadata." + formWithErrors + ".")
        BadRequest(
          views.html.admin.changeItem(
            new ChangeItem(
              itemId,
              siteListAsMap,
              LocaleInfo.localeTable,
              createItemNameTable(itemId),
              addItemNameForm,
              createSiteTable,
              createSiteItemTable(itemId),
              addSiteItemForm,
              createItemCategoryForm(itemId),
              createCategoryTable,
              createItemDescriptionTable(itemId),
              addItemDescriptionForm,
              createItemPriceTable(itemId),
              addItemPriceForm,
              taxTable,
              currencyTable,
              createSiteTable(itemId),
              createItemMetadataTable(itemId),
              addItemMetadataForm,
              createSiteItemMetadataTable(itemId),
              addSiteItemMetadataForm,
              formWithErrors,
              addSiteItemTextMetadataForm,
              createItemTextMetadataTable(itemId),
              addItemTextMetadataForm,
              ItemPictures.retrieveAttachmentNames(itemId),
              createCouponForm(ItemId(itemId))
            )
          )
        )
      },
      newMetadata => {
        DB.withConnection { implicit conn =>
          newMetadata.update(itemId)
        }
        Redirect(
          routes.ItemMaintenance.startChangeItem(itemId)
        ).flashing("message" -> Messages("itemIsUpdated"))
      }
    )
  }}

  def addItemMetadata(id: Long) = isAuthenticated { implicit login => forAdmin { implicit request =>
    addItemMetadataForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.addItemMetadata." + formWithErrors + ".")
        BadRequest(
          views.html.admin.changeItem(
            new ChangeItem(
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
              addItemDescriptionForm,
              createItemPriceTable(id),
              addItemPriceForm,
              taxTable,
              currencyTable,
              createSiteTable(id),
              createItemMetadataTable(id),
              formWithErrors,
              createSiteItemMetadataTable(id),
              addSiteItemMetadataForm,
              createSiteItemTextMetadataTable(id),
              addSiteItemTextMetadataForm,
              createItemTextMetadataTable(id),
              addItemTextMetadataForm,
              ItemPictures.retrieveAttachmentNames(id),
              createCouponForm(ItemId(id))
            )
          )
        )
      },
      newMetadata => {
        try {
          newMetadata.add(id)

          Redirect(
            routes.ItemMaintenance.startChangeItem(id)
          ).flashing("message" -> Messages("itemIsUpdated"))
        }
        catch {
          case e: UniqueConstraintException => {
            BadRequest(
              views.html.admin.changeItem(
                new ChangeItem(
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
                  addItemDescriptionForm,
                  createItemPriceTable(id),
                  addItemPriceForm,
                  taxTable,
                  currencyTable,
                  createSiteTable(id),
                  createItemMetadataTable(id),
                  addItemMetadataForm
                    .fill(newMetadata)
                    .withError("metadataType", "unique.constraint.violation"),
                  createSiteItemMetadataTable(id),
                  addSiteItemMetadataForm,
                  createSiteItemTextMetadataTable(id),
                  addSiteItemTextMetadataForm,
                  createItemTextMetadataTable(id),
                  addItemTextMetadataForm,
                  ItemPictures.retrieveAttachmentNames(id),
                  createCouponForm(ItemId(id))
                )
              )
            )
          }
        }
      }
    )
  }}

  def addItemTextMetadata(id: Long) = isAuthenticated { implicit login => forAdmin { implicit request =>
    addItemTextMetadataForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.addItemTextMetadata." + formWithErrors + ".")
        BadRequest(
          views.html.admin.changeItem(
            new ChangeItem(
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
              addItemDescriptionForm,
              createItemPriceTable(id),
              addItemPriceForm,
              taxTable,
              currencyTable,
              createSiteTable(id),
              createItemMetadataTable(id),
              addItemMetadataForm,
              createSiteItemMetadataTable(id),
              addSiteItemMetadataForm,
              createSiteItemTextMetadataTable(id),
              addSiteItemTextMetadataForm,
              createItemTextMetadataTable(id),
              formWithErrors,
              ItemPictures.retrieveAttachmentNames(id),
              createCouponForm(ItemId(id))
            )
          )
        )
      },
      newMetadata => {
        try {
          newMetadata.add(id)

          Redirect(
            routes.ItemMaintenance.startChangeItem(id)
          ).flashing("message" -> Messages("itemIsUpdated"))
        }
        catch {
          case e: UniqueConstraintException => {
            BadRequest(
              views.html.admin.changeItem(
                new ChangeItem(
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
                  addItemDescriptionForm,
                  createItemPriceTable(id),
                  addItemPriceForm,
                  taxTable,
                  currencyTable,
                  createSiteTable(id),
                  createItemMetadataTable(id),
                  addItemMetadataForm,
                  createSiteItemMetadataTable(id),
                  addSiteItemMetadataForm,
                  createSiteItemTextMetadataTable(id),
                  addSiteItemTextMetadataForm,
                  createItemTextMetadataTable(id),
                  addItemTextMetadataForm
                    .fill(newMetadata)
                    .withError("metadataType", "unique.constraint.violation"),
                  ItemPictures.retrieveAttachmentNames(id),
                  createCouponForm(ItemId(id))
                )
              )
            )
          }
        }
      }
    )
  }}

  def addSiteItemMetadata(id: Long) = isAuthenticated { implicit login => forAdmin { implicit request =>
    addSiteItemMetadataForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.addSiteItemMetadata." + formWithErrors + ".")
        BadRequest(
          views.html.admin.changeItem(
            new ChangeItem(
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
              addItemDescriptionForm,
              createItemPriceTable(id),
              addItemPriceForm,
              taxTable,
              currencyTable,
              createSiteTable(id),
              createItemMetadataTable(id),
              addItemMetadataForm,
              createSiteItemMetadataTable(id),
              formWithErrors,
              createSiteItemTextMetadataTable(id),
              addSiteItemTextMetadataForm,
              createItemTextMetadataTable(id),
              addItemTextMetadataForm,
              ItemPictures.retrieveAttachmentNames(id),
              createCouponForm(ItemId(id))
            )
          )
        )
      },
      newMetadata => {
        try {
          DB.withConnection { implicit conn =>
            newMetadata.add(id)
          }

          Redirect(
            routes.ItemMaintenance.startChangeItem(id)
          ).flashing("message" -> Messages("itemIsUpdated"))
        }
        catch {
          case e: UniqueConstraintException => {
            BadRequest(
              views.html.admin.changeItem(
                new ChangeItem(
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
                  addItemDescriptionForm,
                  createItemPriceTable(id),
                  addItemPriceForm,
                  taxTable,
                  currencyTable,
                  createSiteTable(id),
                  createItemMetadataTable(id),
                  addItemMetadataForm,
                  createSiteItemMetadataTable(id),
                  addSiteItemMetadataForm
                    .fill(newMetadata)
                    .withError("metadataType", "unique.constraint.violation"),
                  createSiteItemTextMetadataTable(id),
                  addSiteItemTextMetadataForm,
                  createItemTextMetadataTable(id),
                  addItemTextMetadataForm,
                  ItemPictures.retrieveAttachmentNames(id),
                  createCouponForm(ItemId(id))
                )
              )
            )
          }
        }
      }
    )
  }}

  def addSiteItemTextMetadata(id: Long) = isAuthenticated { implicit login => forAdmin { implicit request =>
    addSiteItemTextMetadataForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemMaintenance.addSiteItemTextMetadata." + formWithErrors + ".")
        BadRequest(
          views.html.admin.changeItem(
            new ChangeItem(
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
              addItemDescriptionForm,
              createItemPriceTable(id),
              addItemPriceForm,
              taxTable,
              currencyTable,
              createSiteTable(id),
              createItemMetadataTable(id),
              addItemMetadataForm,
              createSiteItemMetadataTable(id),
              addSiteItemMetadataForm,
              createSiteItemTextMetadataTable(id),
              formWithErrors,
              createItemTextMetadataTable(id),
              addItemTextMetadataForm,
              ItemPictures.retrieveAttachmentNames(id),
              createCouponForm(ItemId(id))
            )
          )
        )
      },
      newMetadata => {
        try {
          DB.withConnection { implicit conn =>
            newMetadata.add(id)
          }

          Redirect(
            routes.ItemMaintenance.startChangeItem(id)
          ).flashing("message" -> Messages("itemIsUpdated"))
        }
        catch {
          case e: UniqueConstraintException => {
            BadRequest(
              views.html.admin.changeItem(
                new ChangeItem(
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
                  addItemDescriptionForm,
                  createItemPriceTable(id),
                  addItemPriceForm,
                  taxTable,
                  currencyTable,
                  createSiteTable(id),
                  createItemMetadataTable(id),
                  addItemMetadataForm,
                  createSiteItemMetadataTable(id),
                  addSiteItemMetadataForm,
                  createSiteItemTextMetadataTable(id),
                  addSiteItemTextMetadataForm
                    .fill(newMetadata)
                    .withError("metadataType", "unique.constraint.violation"),
                  createItemTextMetadataTable(id),
                  addItemTextMetadataForm,
                  ItemPictures.retrieveAttachmentNames(id),
                  createCouponForm(ItemId(id))
                )
              )
            )
          }
        }
      }
    )
  }}
}
