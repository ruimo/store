package controllers

import play.api._
import data.Form
import db.DB
import play.api.mvc._
import models._
import play.api.Play.current

import java.util.Locale
import java.util.regex.Pattern
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n.Messages
import helpers.Enums

object Shipping extends Controller with NeedLogin with HasLogger {
  val Zip1Pattern = Pattern.compile("\\d{3}")
  val Zip2Pattern = Pattern.compile("\\d{4}")
  val TelPattern = Pattern.compile("\\d+{1,32}")
  val TelOptionPattern = Pattern.compile("\\d{0,32}")

  val jaForm = Form(
    mapping(
      "firstName" -> text.verifying(nonEmpty, maxLength(64)),
      "lastName" -> text.verifying(nonEmpty, maxLength(64)),
      "firstNameKana" -> text.verifying(nonEmpty, maxLength(64)),
      "lastNameKana" -> text.verifying(nonEmpty, maxLength(64)),
      "zip1" -> text.verifying(z => Zip1Pattern.matcher(z).matches),
      "zip2" -> text.verifying(z => Zip2Pattern.matcher(z).matches),
      "prefecture" -> number,
      "address1" -> text.verifying(nonEmpty, maxLength(256)),
      "address2" -> text.verifying(nonEmpty, maxLength(256)),
      "address3" -> text.verifying(maxLength(256)),
      "address4" -> text.verifying(maxLength(256)),
      "address5" -> text.verifying(maxLength(256)),
      "tel1" -> text.verifying(Messages("error.number"), z => TelPattern.matcher(z).matches),
      "tel2" -> text.verifying(Messages("error.number"), z => TelOptionPattern.matcher(z).matches),
      "tel3" -> text.verifying(Messages("error.number"), z => TelOptionPattern.matcher(z).matches)
    )(CreateAddress.apply4Japan)(CreateAddress.unapply4Japan)
  )

  def startEnterShippingAddress = isAuthenticated { login => implicit request =>
    DB.withConnection { implicit conn =>
      val addr: Option[Address] = ShippingAddressHistory.list(login.userId).headOption.map {
        h => Address.byId(h.addressId)
      }
      val form = addr match {
        case Some(a) =>
          jaForm.fill(CreateAddress.fromAddress(a))
        case None => jaForm
      }
      lang.toLocale match {
        case Locale.JAPANESE =>
          Ok(views.html.shippingAddressJa(form, Address.JapanPrefectures))
        case Locale.JAPAN =>
          Ok(views.html.shippingAddressJa(form, Address.JapanPrefectures))
        
        case _ =>
          Ok(views.html.shippingAddressJa(form, Address.JapanPrefectures))
      }
    }
  }

  def enterShippingAddressJa = isAuthenticated { login => implicit request =>
    jaForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in Shipping.enterShippingAddress.")
        DB.withConnection { implicit conn =>
          BadRequest(views.html.shippingAddressJa(formWithErrors, Address.JapanPrefectures))
        }
      },
      newShippingAddress => {
        DB.withTransaction { implicit conn => {
          newShippingAddress.save(login.userId)
          Redirect(routes.Shipping.confirmShippingAddressJa())
        }}
      }
    )
  }

  def confirmShippingAddressJa = isAuthenticated { login => implicit request =>
    DB.withConnection { implicit conn =>
      val cart = ShoppingCart.listItemsForUser(
        LocaleInfo.getDefault, 
        login.userId
      )
      val his = ShippingAddressHistory.list(login.userId).head
      val addr = Address.byId(his.addressId)

      Ok(views.html.confirmShippingAddressJa(cart, addr))
    }
  }

  def finalizeTransaction = isAuthenticated { login => implicit request =>
    Ok("Create Transaction")
  }
}
