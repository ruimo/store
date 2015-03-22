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
import models.{CreateItemInquiry, StoreUser, CreateItemInquiryReservation}

class ItemInquiryReserveBase extends Controller with I18nAware with NeedLogin {
  def itemInquiryForm(implicit lang: Lang) = Form(
    mapping(
      "siteId" -> longNumber,
      "itemId" -> longNumber,
      "name" -> text.verifying(nonEmpty, maxLength(128)),
      "email" -> text.verifying(emailConstraint: _*)
    )(CreateItemInquiry.apply)(CreateItemInquiry.unapply)
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
        user.email
      )
    )

    Ok(views.html.itemInquiry(form))
  }

  def submitItemInquiry = isAuthenticated { implicit login => implicit request =>
    itemInquiryForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(views.html.itemInquiry(formWithErrors)),
      info =>
        Redirect(routes.Application.index).flashing("message" -> Messages("itemInquirySubmit"))
    )
  }
}
