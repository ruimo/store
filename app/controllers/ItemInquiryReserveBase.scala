package controllers

import play.api._
import db.DB
import libs.json.{JsObject, Json}
import play.api.mvc._

import models.{ItemInquiry}
import play.api.Play.current
import controllers.I18n.I18nAware
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{Lang, Messages}
import play.api.data.validation.Constraints._
import constraints.FormConstraints._
import models.{CreateItemInquiry, CreateItemReservation, StoreUser, CreateItemInquiryReservation, ItemInquiryType, Site, ItemName, SiteItem, ItemId, LocaleInfo}

class ItemInquiryReserveBase extends Controller with I18nAware with NeedLogin with HasLogger {
  def itemInquiryForm(implicit lang: Lang) = Form(
    mapping(
      "siteId" -> longNumber,
      "itemId" -> longNumber,
      "name" -> text.verifying(nonEmpty, maxLength(128)),
      "email" -> text.verifying(emailConstraint: _*),
      "inquiryBody" -> text.verifying(nonEmpty, maxLength(8192))
    )(CreateItemInquiry.apply)(CreateItemInquiry.unapply)
  )

  def itemReservationForm(implicit lang: Lang) = Form(
    mapping(
      "siteId" -> longNumber,
      "itemId" -> longNumber,
      "name" -> text.verifying(nonEmpty, maxLength(128)),
      "email" -> text.verifying(emailConstraint: _*),
      "comment" -> text.verifying(minLength(0), maxLength(8192))
    )(CreateItemReservation.apply)(CreateItemReservation.unapply)
  )

  def fillForm[T <: CreateItemInquiryReservation](form: Form[T], model: T): Form[T] = form.fill(model)

  def startItemInquiry(
    siteId: Long, itemId: Long
  ) = isAuthenticated { implicit login => implicit request =>
    val user: StoreUser = login.storeUser
    val form = fillForm(
      itemInquiryForm, 
      CreateItemInquiry(
        siteId, itemId,
        user.firstName + user.middleName.map(n => " " + n).getOrElse("") + " " + user.lastName,
        user.email, ""
      )
    )

    Ok(
      views.html.itemInquiry(itemInfo(siteId, itemId), form)
    )
  }

  def startItemReservation(
    siteId: Long, itemId: Long
  ) = isAuthenticated { implicit login => implicit request =>
    val user: StoreUser = login.storeUser
    val form = fillForm(
      itemReservationForm, 
      CreateItemReservation(
        siteId, itemId,
        user.firstName + user.middleName.map(n => " " + n).getOrElse("") + " " + user.lastName,
        user.email, ""
      )
    )

    Ok(
      views.html.itemReservation(itemInfo(siteId, itemId), form)
    )
  }

  def itemInfo(siteId: Long, itemId: Long): (Site, ItemName) = DB.withConnection { implicit conn =>
    SiteItem.getWithSiteAndItem(siteId, ItemId(itemId), LocaleInfo.getDefault)
  }.get

  def submitItemInquiry(
    siteId: Long, itemId: Long
  ) = isAuthenticated { implicit login => implicit request =>
    itemInquiryForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemInquiryReserveBase.submitItemInquiry." + formWithErrors + ".")
        BadRequest(views.html.itemInquiry(itemInfo(siteId, itemId), formWithErrors))
      },
      info => DB.withConnection { implicit conn =>
        info.save(login.storeUser)
        Redirect(routes.Application.index).flashing("message" -> Messages("itemInquirySubmit"))
      }
    )
  }

  def submitItemReservation(
    siteId: Long, itemId: Long
  ) = isAuthenticated { implicit login => implicit request =>
    itemReservationForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in ItemInquiryReserveBase.submitItemReservation." + formWithErrors + ".")
        BadRequest(views.html.itemReservation(itemInfo(siteId, itemId), formWithErrors))
      },
      info => DB.withConnection { implicit conn =>
        info.save(login.storeUser)
        Redirect(routes.Application.index).flashing("message" -> Messages("itemReservationSubmit"))
      }
    )
  }
}
