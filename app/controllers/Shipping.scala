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
import controllers.I18n.I18nAware
import java.sql.Connection
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object Shipping extends Controller with NeedLogin with HasLogger with I18nAware {
  val Zip1Pattern = Pattern.compile("\\d{3}")
  val Zip2Pattern = Pattern.compile("\\d{4}")
  val TelPattern = Pattern.compile("\\d+{1,32}")
  val TelOptionPattern = Pattern.compile("\\d{0,32}")
  val ShippingDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")

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
      "tel3" -> text.verifying(Messages("error.number"), z => TelOptionPattern.matcher(z).matches),
      "shippingDate" -> jodaDate("yyyy-MM-dd")
    )(CreateAddress.apply4Japan)(CreateAddress.unapply4Japan)
  )

  def startEnterShippingAddress = isAuthenticated { implicit login => implicit request =>
    DB.withConnection { implicit conn =>
      val addr: Option[Address] = ShippingAddressHistory.list(login.userId).headOption.map {
        h => Address.byId(h.addressId)
      }
      val shippingDate = new DateTime().plusDays(3)
      val form = addr match {
        case Some(a) =>
          jaForm.fill(CreateAddress.fromAddress(a, shippingDate))
        case None => jaForm.bind(Map("shippingDate" -> ShippingDateFormat.print(shippingDate))).discardingErrors
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

  def enterShippingAddressJa = isAuthenticated { implicit login => implicit request =>
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

  def confirmShippingAddressJa = isAuthenticated { implicit login => implicit request =>
    DB.withConnection { implicit conn =>
      val cart = ShoppingCartItem.listItemsForUser(LocaleInfo.getDefault, login.userId)
      val his = ShippingAddressHistory.list(login.userId).head
      val addr = Address.byId(his.addressId)
      try {
        Ok(
          views.html.confirmShippingAddressJa(
            Transaction(login.userId, CurrencyInfo.Jpy, cart, addr, shippingFee(addr, cart))
          )
        )
      }
      catch {
        case e: CannotShippingException => {
          Ok(views.html.cannotShipJa(cannotShip(cart, e, addr), addr, e.itemClass))
        }
      }
    }
  }


  def cannotShip(
    cart: ShoppingCartTotal, e: CannotShippingException, addr: Address
  ): Seq[ShoppingCartTotalEntry] = {
    cart.table.filter { item =>
      item.siteItemNumericMetadata.get(SiteItemNumericMetadataType.SHIPPING_SIZE) match {
        case None => true
        case Some(itemClass) =>
          e.isCannotShip(
            item.site,
            addr.prefecture.code,
            itemClass.metadata
          )
      }
    }
  }

  def shippingFee(
    addr: Address, cart: ShoppingCartTotal
  )(implicit conn: Connection): ShippingTotal = {
    ShippingFeeHistory.feeBySiteAndItemClass(
      CountryCode.JPN, addr.prefecture.code,
      cart.table.foldLeft(ShippingFeeEntries()) {
        (sum, e) => sum.add(
          e.site,
          e.siteItemNumericMetadata.get(SiteItemNumericMetadataType.SHIPPING_SIZE).map(_.metadata).getOrElse(1L),
          e.shoppingCartItem.quantity
        )
      }
    )
  }

  def finalizeTransactionJa = isAuthenticated { implicit login => implicit request =>
    finalizeTransaction(CurrencyInfo.Jpy)
  }

  def finalizeTransaction(
    currency: CurrencyInfo
  )(implicit request: Request[AnyContent], login: LoginSession): Result = {
    DB.withTransaction { implicit conn =>
      val cart = ShoppingCartItem.listItemsForUser(LocaleInfo.getDefault, login.userId)
      if (cart.isEmpty) {
        logger.error("Shipping.finalizeTransaction(): shopping cart is empty.")
        throw new Error("Shipping.finalizeTransaction(): shopping cart is empty.")
      }
      else {
        val his = ShippingAddressHistory.list(login.userId).head
        val addr = Address.byId(his.addressId)
        try {
          val persister = new TransactionPersister()
          val tranId = persister.persist(
            Transaction(login.userId, currency, cart, addr, shippingFee(addr, cart))
          )
          ShoppingCartItem.removeForUser(login.userId)
          val tran = persister.load(tranId, LocaleInfo.getDefault)
          
          Ok(
            views.html.showTransactionJa(
              tran, Address.byId(tran.shippingTable.head._2.head.addressId)
            )

          )
        }
        catch {
          case e: CannotShippingException => {
            Ok(views.html.cannotShipJa(cannotShip(cart, e, addr), addr, e.itemClass))
          }
        }
      }
    }
  }
}
