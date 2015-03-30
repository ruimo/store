package controllers

import scala.collection.immutable
import play.api._
import db.DB
import libs.json.{JsObject, Json}
import play.api.mvc._

import java.sql.Connection
import models.{ItemInquiry}
import play.api.Play.current
import controllers.I18n.I18nAware
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{Lang, Messages}
import play.api.data.validation.Constraints._
import constraints.FormConstraints._
import models.{CreateItemInquiry, CreateItemReservation, StoreUser, CreateItemInquiryReservation, ItemInquiryType, Site, ItemName, SiteItem, ItemId, LocaleInfo, LoginSession, ItemInquiryId, ItemInquiryField, ItemInquiryStatus}

class ItemInquiryReserveBase extends Controller with I18nAware with NeedLogin with HasLogger {
  val idSubmitForm: Form[Long] = Form(
    single(
      "id" -> longNumber
    )
  )

  def itemInquiryForm(implicit lang: Lang): Form[CreateItemInquiryReservation] = Form(
    mapping(
      "siteId" -> longNumber,
      "itemId" -> longNumber,
      "name" -> text.verifying(nonEmpty, maxLength(128)),
      "email" -> text.verifying(emailConstraint: _*),
      "inquiryBody" -> text.verifying(nonEmpty, maxLength(8192))
    )(CreateItemInquiry.apply)(CreateItemInquiry.unapply)
  ).asInstanceOf[Form[CreateItemInquiryReservation]]

  def itemReservationForm(implicit lang: Lang): Form[CreateItemInquiryReservation] = Form(
    mapping(
      "siteId" -> longNumber,
      "itemId" -> longNumber,
      "name" -> text.verifying(nonEmpty, maxLength(128)),
      "email" -> text.verifying(emailConstraint: _*),
      "comment" -> text.verifying(minLength(0), maxLength(8192))
    )(CreateItemReservation.apply)(CreateItemReservation.unapply)
  ).asInstanceOf[Form[CreateItemInquiryReservation]]

  def startItemInquiry(
    siteId: Long, itemId: Long
  ) = isAuthenticated { implicit login => implicit request =>
    Ok(
      views.html.itemInquiry(itemInfo(siteId, itemId), inquiryStartForm(siteId, itemId, login.storeUser))
    )
  }

  def startItemReservation(
    siteId: Long, itemId: Long
  ) = isAuthenticated { implicit login => implicit request =>
    Ok(
      views.html.itemReservation(itemInfo(siteId, itemId), reservationStartForm(siteId, itemId, login.storeUser))
    )
  }

  def inquiryStartForm(
    siteId: Long, itemId: Long, user: StoreUser
  )(
    implicit login: LoginSession
  ): Form[_ <: CreateItemInquiryReservation] = itemInquiryForm.fill(
    CreateItemInquiry(
      siteId, itemId,
      user.fullName,
      user.email, ""
    )
  )

  def reservationStartForm(
    siteId: Long, itemId: Long, user: StoreUser
  )(
    implicit login: LoginSession
  ): Form[_ <: CreateItemInquiryReservation] = itemReservationForm.fill(
    CreateItemReservation(
      siteId, itemId,
      user.fullName,
      user.email, ""
    ).asInstanceOf[CreateItemInquiryReservation]
  )

  def itemInfo(siteId: Long, itemId: Long): (Site, ItemName) = DB.withConnection { implicit conn =>
    SiteItem.getWithSiteAndItem(siteId, ItemId(itemId), LocaleInfo.getDefault)
  }.get

  def amendReservationForm(
    rec: ItemInquiry, fields: immutable.Map[Symbol, String]
  ): Form[_ <: CreateItemInquiryReservation] = itemReservationForm.fill(
    CreateItemReservation(
      rec.siteId, rec.itemId.id,
      rec.submitUserName,
      rec.email,
      fields('Message)
    )
  )

  def amendItemReservationStart(inqId: Long) = isAuthenticated { implicit login => implicit request =>
    DB.withConnection { implicit conn =>
      val id = ItemInquiryId(inqId)
      val rec = ItemInquiry(id)
      val fields = ItemInquiryField(id)

      Ok(
        views.html.amendItemReservation(
          id,
          itemInfo(rec.siteId, rec.itemId.id),
          amendReservationForm(rec, fields)
        )
      )
    }
  }

  def amendItemReservation(inqId: Long) = isAuthenticated { implicit login => implicit request =>
    val id = ItemInquiryId(inqId)
    val (rec: ItemInquiry, fields: immutable.Map[Symbol, String]) = DB.withConnection { implicit conn =>
      (ItemInquiry(id), ItemInquiryField(id))
    }

    itemReservationForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemInquiryReserveBase.amendItemReservation." + formWithErrors + ".")
        BadRequest(views.html.amendItemReservation(id, itemInfo(rec.siteId, rec.itemId.id), formWithErrors))
      },
      info => DB.withTransaction { implicit conn =>
        info.update(id)
        Ok(
          views.html.itemReservationConfirm(
            rec, fields, itemInfo(rec.siteId, rec.itemId.id), idSubmitForm.fill(inqId)
          )
        )
      }
    )
  }

  def confirmItemInquiry(
    siteId: Long, itemId: Long
  ) = isAuthenticated { implicit login => implicit request =>
    itemInquiryForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemInquiryReserveBase.submitItemInquiry." + formWithErrors + ".")
        BadRequest(views.html.itemInquiry(itemInfo(siteId, itemId), formWithErrors))
      },
      info => DB.withConnection { implicit conn =>
        val rec: ItemInquiry = info.save(login.storeUser)
        Redirect(routes.ItemInquiryReserve.submitItemInquiryStart(rec.id.get.id))
      }
    )
  }

  def amendItemInquiryStart(inqId: Long) = isAuthenticated { implicit login => implicit request =>
    DB.withConnection { implicit conn =>
      val id = ItemInquiryId(inqId)
      val rec = ItemInquiry(id)
      val fields = ItemInquiryField(id)

      Ok(
        views.html.amendItemInquiry(
          id,
          itemInfo(rec.siteId, rec.itemId.id),
          itemInquiryForm.fill(
            CreateItemInquiry(
              rec.siteId, rec.itemId.id,
              rec.submitUserName,
              rec.email,
              fields('Message)
            )
          )
        )
      )
    }
  }

  def amendItemInquiry(inqId: Long) = isAuthenticated { implicit login => implicit request =>
    val id = ItemInquiryId(inqId)
    val (rec: ItemInquiry, fields: immutable.Map[Symbol, String]) = DB.withConnection { implicit conn =>
      (ItemInquiry(id), ItemInquiryField(id))
    }

    itemInquiryForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemInquiryReserveBase.amendItemInquiry." + formWithErrors + ".")
        BadRequest(
          views.html.amendItemInquiry(
            id,
            itemInfo(rec.siteId, rec.itemId.id),
            formWithErrors
          )
        )
      },
      info => DB.withTransaction { implicit conn =>
        info.update(id)
        Ok(
          views.html.itemReservationConfirm(
            rec, fields, itemInfo(rec.siteId, rec.itemId.id), idSubmitForm.fill(inqId)
          )
        )
      }
    )
  }

  def confirmItemReservation(
    siteId: Long, itemId: Long
  ) = isAuthenticated { implicit login => implicit request =>
    itemReservationForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemInquiryReserveBase.confirmItemReservation." + formWithErrors + ".")
        BadRequest(views.html.itemReservation(itemInfo(siteId, itemId), formWithErrors))
      },
      info => DB.withConnection { implicit conn =>
        val rec: ItemInquiry = info.save(login.storeUser)
        Redirect(routes.ItemInquiryReserve.submitItemReservationStart(rec.id.get.id))
      }
    )
  }

  def submitItemInquiryStart(inquiryId: Long) = isAuthenticated { implicit login => implicit request =>
    DB.withConnection { implicit conn =>
      val id = ItemInquiryId(inquiryId)
      val rec = ItemInquiry(id)
      val fields = ItemInquiryField(id)
      Ok(
        views.html.itemInquiryConfirm(
          rec, fields, itemInfo(rec.siteId, rec.itemId.id), idSubmitForm.fill(inquiryId)
        )
      )
    }
  }

  def submitItemInquiry(inquiryId: Long) = isAuthenticated { implicit login => implicit request =>
    idSubmitForm.bindFromRequest.fold(
      formWithErrors => DB.withConnection { implicit conn =>
        val id = ItemInquiryId(inquiryId)
        val rec = ItemInquiry(id)
        val fields = ItemInquiryField(id)
        logger.error("Validation error in ItemInquiryReserveBase.submitItemInquiry." + formWithErrors + ".")
        BadRequest(
          views.html.itemInquiryConfirm(
            rec, fields, itemInfo(rec.siteId, rec.itemId.id), idSubmitForm.fill(inquiryId)
          )
        )
      },
      id => DB.withConnection { implicit conn =>
        if (ItemInquiry.changeStatus(ItemInquiryId(id), ItemInquiryStatus.SUBMITTED) == 0) {
          throw new Error("Record update fail id = " + id)
        }
        Redirect(routes.Application.index).flashing("message" -> Messages("itemInquirySubmit"))
      }
    )
  }

  def submitItemReservationStart(inquiryId: Long) = isAuthenticated { implicit login => implicit request =>
    DB.withConnection { implicit conn =>
      val id = ItemInquiryId(inquiryId)
      val rec = ItemInquiry(id)
      val fields = ItemInquiryField(id)
      Ok(
        views.html.itemReservationConfirm(
          rec, fields, itemInfo(rec.siteId, rec.itemId.id), idSubmitForm.fill(inquiryId)
        )
      )
    }
  }

  def submitItemReservation(inquiryId: Long) = isAuthenticated { implicit login => implicit request =>
    idSubmitForm.bindFromRequest.fold(
      formWithErrors => DB.withConnection { implicit conn =>
        val id = ItemInquiryId(inquiryId)
        val rec = ItemInquiry(id)
        val fields = ItemInquiryField(id)
        logger.error("Validation error in ItemInquiryReserveBase.submitItemReservation." + formWithErrors + ".")
        BadRequest(
          views.html.itemReservationConfirm(
            rec, fields, itemInfo(rec.siteId, rec.itemId.id), idSubmitForm.fill(inquiryId)
          )
        )
      },
      id => DB.withConnection { implicit conn =>
        if (ItemInquiry.changeStatus(ItemInquiryId(id), ItemInquiryStatus.SUBMITTED) == 0) {
          throw new Error("Record update fail id = " + id)
        }
        Redirect(routes.Application.index).flashing("message" -> Messages("itemReservationSubmit"))
      }
    )
  }
}
