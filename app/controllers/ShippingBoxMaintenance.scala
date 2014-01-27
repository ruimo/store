package controllers

import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models._
import play.api.data.Form
import controllers.I18n.I18nAware
import play.api.mvc.Controller
import play.api.db.DB
import play.api.i18n.Messages
import play.api.Play.current
import models.CreateShippingBox

object ShippingBoxMaintenance extends Controller with I18nAware with NeedLogin with HasLogger {
  val createShippingBoxForm = Form(
    mapping(
      "siteId" -> longNumber,
      "itemClass" -> longNumber,
      "boxSize" -> number,
      "boxName" -> text.verifying(nonEmpty, maxLength(32))
    ) (CreateShippingBox.apply)(CreateShippingBox.unapply)
  )

  val changeShippingBoxForm = Form(
    mapping(
      "id" -> longNumber,
      "siteId" -> longNumber,
      "itemClass" -> longNumber,
      "boxSize" -> number,
      "boxName" -> text.verifying(nonEmpty, maxLength(32))
    ) (ChangeShippingBox.apply)(ChangeShippingBox.unapply)
  )
    
  val removeBoxForm = Form(
    "boxId" -> longNumber
  )

  def index = isAuthenticated { implicit login => forSuperUser { implicit request =>
    Ok(views.html.admin.shippingBoxMaintenance())
  }}

  def startCreateShippingBox = isAuthenticated { implicit login => forSuperUser { implicit request =>
    DB.withConnection { implicit conn =>
      Ok(views.html.admin.createNewShippingBox(createShippingBoxForm, Site.tableForDropDown))
    }
  }}

  def createNewShippingBox = isAuthenticated { implicit login => forSuperUser { implicit request =>
    createShippingBoxForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ShippingBoxMaintenance.createNewShippingBox.")
        DB.withConnection { implicit conn =>
          BadRequest(views.html.admin.createNewShippingBox(formWithErrors, Site.tableForDropDown))
        }
      },
      newShippingBox => DB.withConnection { implicit conn =>
        newShippingBox.save
        Redirect(
          routes.ShippingBoxMaintenance.startCreateShippingBox
        ).flashing("message" -> Messages("shippingBoxIsCreated"))
      }
    )
  }}

  def editShippingBox(start: Int, size: Int) = isAuthenticated { implicit login => forSuperUser { implicit request =>
    DB.withConnection { implicit conn =>
      Ok(views.html.admin.editShippingBox(ShippingBox.list))
    }
  }}

  def removeShippingBox = isAuthenticated { implicit login => forSuperUser { implicit request =>
    val boxId = removeBoxForm.bindFromRequest.get
    DB.withTransaction { implicit conn =>
      ShippingBox.removeWithChildren(boxId)
    }

    Redirect(
      routes.ShippingBoxMaintenance.editShippingBox()
    ).flashing("message" -> Messages("shippingBoxIsRemoved"))
  }}

  def startChangeShippingBox(id: Long) = isAuthenticated { implicit login => forSuperUser { implicit request =>
    DB.withConnection { implicit conn =>
      val rec = ShippingBox(id)
      Ok(
        views.html.admin.changeShippingBox(
          changeShippingBoxForm.fill(
            ChangeShippingBox(
              id, rec.siteId, rec.itemClass, rec.boxSize, rec.boxName
            )
          ),
          Site.tableForDropDown
        )
      )
    }
  }}

  def changeShippingBox = isAuthenticated { implicit login => forSuperUser { implicit request =>
    changeShippingBoxForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ShippingBoxMaintenance.changeShippingBox.")
        DB.withConnection { implicit conn =>
          BadRequest(views.html.admin.changeShippingBox(formWithErrors, Site.tableForDropDown))
        }
      },
      newShippingBox => DB.withConnection { implicit conn =>
        try {
          newShippingBox.save
          Redirect(
            routes.ShippingBoxMaintenance.editShippingBox()
          ).flashing("message" -> Messages("shippingBoxIsUpdated"))
        }
        catch {
          case e: UniqueConstraintException =>
            BadRequest(
              views.html.admin.changeShippingBox(
                changeShippingBoxForm.fill(newShippingBox).withError(
                  "itemClass", Messages("unique.constraint.violation")
                ),
                Site.tableForDropDown
              )
            )
        }
      }
    )
  }}
}
