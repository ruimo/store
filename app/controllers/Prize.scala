package controllers

import controllers.I18n.I18nAware
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.db.DB
import play.api.mvc._
import play.api.i18n.{Lang, Messages}
import models.{StoreUser, CreatePrize, Address, UserAddress}
import constraints.FormConstraints._

object Prize extends Controller with NeedLogin with HasLogger with I18nAware {
  def prizeForm(implicit lang: Lang) = Form(
    mapping(
      "firstName" -> text.verifying(firstNameConstraint: _*),
      "lastName" -> text.verifying(lastNameConstraint: _*),
      "zip1" -> text.verifying(z => Shipping.Zip1Pattern.matcher(z).matches),
      "zip2" -> text.verifying(z => Shipping.Zip2Pattern.matcher(z).matches),
      "prefecture" -> number,
      "address1" -> text.verifying(nonEmpty, maxLength(256)),
      "address2" -> text.verifying(nonEmpty, maxLength(256)),
      "address3" -> text.verifying(maxLength(256)),
      "address4" -> text.verifying(maxLength(256)),
      "address5" -> text.verifying(maxLength(256)),
      "tel" -> text.verifying(Messages("error.number"), z => Shipping.TelPattern.matcher(z).matches),
      "comment" -> text.verifying(maxLength(2048))
    )(CreatePrize.apply)(CreatePrize.unapply)
  )

  def entry(itemName: String) = isAuthenticated { implicit login => implicit request =>
    val user: StoreUser = login.storeUser
    val addr: Option[Address] = DB.withConnection{ implicit conn =>
      UserAddress.getByUserId(user.id.get).map {
        ua => Address.byId(ua.addressId)
      }
    }

    Ok(
      views.html.prize(
        itemName,
        user,
        Address.JapanPrefectures,
        prizeForm.fill(
          CreatePrize(
            user.firstName,
            user.lastName,
            addr.map(_.zip1).getOrElse(""),
            addr.map(_.zip2).getOrElse(""),
            addr.map(_.prefecture.code).getOrElse(0),
            addr.map(_.address1).getOrElse(""),
            addr.map(_.address2).getOrElse(""),
            addr.map(_.address3).getOrElse(""),
            addr.map(_.address4).getOrElse(""),
            addr.map(_.address5).getOrElse(""),
            addr.map(_.tel1).getOrElse(""),
            ""
          )
        )
      )
    )
  }

  def confirm(itemName: String) = isAuthenticated { implicit login => implicit request =>
    Ok("")
  }
}

