package controllers

import constraints.FormConstraints._
import controllers.I18n.I18nAware
import models.CreateAddress
import play.api._
import data.Form
import db.DB
import libs.concurrent.Akka
import play.api.mvc._
import models._
import play.api.Play.current

import collection.immutable.LongMap
import java.util.Locale
import java.util.regex.Pattern
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import i18n.{Lang, Messages}
import helpers.{RecommendEngine, NotificationMail, Enums}
import java.sql.Connection
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.collection.immutable

object Shipping extends Controller with NeedLogin with HasLogger with I18nAware {
  import NeedLogin._

  val firstNameKanaConstraint = List(nonEmpty, maxLength(64))
  val lastNameKanaConstraint = List(nonEmpty, maxLength(64))

  val Zip1Pattern = Pattern.compile("\\d{3}")
  val Zip2Pattern = Pattern.compile("\\d{4}")
  val TelPattern = Pattern.compile("\\d+{1,32}")
  val TelOptionPattern = Pattern.compile("\\d{0,32}")
  def shippingDateFormat(implicit lang: Lang) = DateTimeFormat.forPattern(Messages("shipping.date.format"))

  def jaForm(implicit lang: Lang) = Form(
    mapping(
      "firstName" -> text.verifying(firstNameConstraint: _*),
      "lastName" -> text.verifying(lastNameConstraint: _*),
      "firstNameKana" -> text.verifying(firstNameKanaConstraint: _*),
      "lastNameKana" -> text.verifying(lastNameKanaConstraint: _*),
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
      "shippingDate" -> jodaDate(Messages("shipping.date.format")),
      "comment" -> text.verifying(maxLength(2048)),
      "email" -> text.verifying(emailConstraint: _*)
    )(CreateAddress.apply4Japan)(CreateAddress.unapply4Japan)
  )

  def startEnterShippingAddress = NeedAuthenticated { implicit request =>
    implicit val login: LoginSession = request.user
    DB.withConnection { implicit conn =>
      if (ShoppingCartItem.isAllCoupon(login.userId)) {
        Redirect(routes.Shipping.confirmShippingAddressJa())
      }
      else {
        val addr: Option[Address] = ShippingAddressHistory.list(login.userId).headOption.map {
          h => Address.byId(h.addressId)
        }
        val shippingDate = new DateTime().plusDays(5)
        val form = addr match {
          case Some(a) =>
            jaForm.fill(CreateAddress.fromAddress(a.fillEmailIfEmpty(login.storeUser.email), shippingDate))
          case None =>
            jaForm.bind(
              Map(
                "shippingDate" -> shippingDateFormat.print(shippingDate),
                "email" -> login.storeUser.email
              )
            ).discardingErrors
        }

        request2lang.toLocale match {
          case Locale.JAPANESE =>
            Ok(views.html.shippingAddressJa(form, Address.JapanPrefectures))
          case Locale.JAPAN =>
            Ok(views.html.shippingAddressJa(form, Address.JapanPrefectures))
        
          case _ =>
            Ok(views.html.shippingAddressJa(form, Address.JapanPrefectures))
        }
      }
    }
  }

  def enterShippingAddressJa = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    jaForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in Shipping.enterShippingAddress.")
        DB.withConnection { implicit conn =>
          BadRequest(views.html.shippingAddressJa(formWithErrors, Address.JapanPrefectures))
        }
      },
      newShippingAddress => {
        DB.withTransaction { implicit conn =>
          newShippingAddress.save(login.userId)
          Redirect(routes.Shipping.confirmShippingAddressJa())
        }
      }
    )
  }

  def confirmShippingAddressJa = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    DB.withConnection { implicit conn =>
      val (cart: ShoppingCartTotal, errors: Seq[ItemExpiredException]) =
        ShoppingCartItem.listItemsForUser(LocaleInfo.getDefault, login.userId)

      if (! errors.isEmpty) {
        Ok(views.html.itemExpired(errors))
      }
      else {
        if (ShoppingCartItem.isAllCoupon(login.userId)) {
          Ok(
            views.html.confirmShippingAddressJa(
              Transaction(
                login.userId, CurrencyInfo.Jpy, cart, None, ShippingTotal(), ShippingDate()
              )
            )
          )
        }
        else {
          val shipping = cart.sites.foldLeft(LongMap[ShippingDateEntry]()) { (sum, e) =>
            sum.updated(e.id.get, ShippingDateEntry(e.id.get, ShoppingCartShipping.find(login.userId, e.id.get)))
          }
          val his = ShippingAddressHistory.list(login.userId).head
          val addr = Address.byId(his.addressId)
          try {
            Ok(
              views.html.confirmShippingAddressJa(
                Transaction(
                  login.userId, CurrencyInfo.Jpy, cart, Some(addr), shippingFee(addr, cart), ShippingDate(shipping)
                )
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

  def finalizeTransactionJa = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    finalizeTransaction(CurrencyInfo.Jpy)
  }

  def finalizeTransaction(
    currency: CurrencyInfo
  )(implicit request: Request[AnyContent], login: LoginSession): Result = {
    DB.withTransaction { implicit conn =>
      val (cart: ShoppingCartTotal, expErrors: Seq[ItemExpiredException]) =
        ShoppingCartItem.listItemsForUser(LocaleInfo.getDefault, login.userId)
      if (! expErrors.isEmpty) {
        Ok(views.html.itemExpired(expErrors))
      }
      else {
        if (cart.isEmpty) {
          logger.error("Shipping.finalizeTransaction(): shopping cart is empty.")
          throw new Error("Shipping.finalizeTransaction(): shopping cart is empty.")
        }
        else {
          val exceedStock: immutable.Map[(ItemId, Long), (String, String, Int, Long)] =
            ShoppingCartItem.itemsExceedStock(login.userId, LocaleInfo.getDefault)

          if (! exceedStock.isEmpty) {
              logger.error("Item exceed stock. " + exceedStock)
              Ok(views.html.itemStockExhausted(exceedStock))
          }
          else if (ShoppingCartItem.isAllCoupon(login.userId)) {
            val persister = new TransactionPersister()
            val tranId = persister.persist(
              Transaction(login.userId, currency, cart, None, ShippingTotal(), ShippingDate())
            )
            ShoppingCartItem.removeForUser(login.userId)
            val tran = persister.load(tranId, LocaleInfo.getDefault)
            NotificationMail.orderCompleted(loginSession.get, tran, None)
            RecommendEngine.onSales(loginSession.get, tran, None)
            Ok(views.html.showTransactionJa(tran, None, textMetadata(tran), siteItemMetadata(tran)))
          }
          else {
            val his = ShippingAddressHistory.list(login.userId).head
            val addr = Address.byId(his.addressId)
            val shipping = cart.sites.foldLeft(LongMap[ShippingDateEntry]()) { (sum, e) =>
              sum.updated(e.id.get, ShippingDateEntry(e.id.get, ShoppingCartShipping.find(login.userId, e.id.get)))
            }
            try {
              val persister = new TransactionPersister()
              val tranId = persister.persist(
                Transaction(login.userId, currency, cart, Some(addr), shippingFee(addr, cart), ShippingDate(shipping))
              )
              ShoppingCartItem.removeForUser(login.userId)
              val tran = persister.load(tranId, LocaleInfo.getDefault)
              val address = Address.byId(tran.shippingTable.head._2.head.addressId)
              NotificationMail.orderCompleted(loginSession.get, tran, Some(address))
              RecommendEngine.onSales(loginSession.get, tran, Some(address))
              Ok(views.html.showTransactionJa(tran, Some(address), textMetadata(tran), siteItemMetadata(tran)))
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
  }

  def textMetadata(
    tran: PersistedTransaction
  )(
    implicit conn: Connection
  ): Map[Long, Map[ItemTextMetadataType, ItemTextMetadata]] = {
    val buf = scala.collection.mutable.HashMap[Long, Map[ItemTextMetadataType, ItemTextMetadata]]()
    tran.itemTable.foreach { e =>
      val items = e._2
      items.foreach { it =>
        val tranItem = it._2
        val itemId = ItemId(tranItem.itemId)
        buf.update(tranItem.itemId, ItemTextMetadata.allById(itemId))
      }
    }

    buf.toMap
  }

  def siteItemMetadata(
    tran: PersistedTransaction
  )(
    implicit conn: Connection
  ): Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]] = {
    val buf = scala.collection.mutable.HashMap[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]]()
    tran.itemTable.foreach { e =>
      val siteId = e._1
      val items = e._2
      items.foreach { it =>
        val tranItem = it._2
        val itemId = tranItem.itemId
        buf.update(siteId -> itemId, SiteItemNumericMetadata.all(siteId, ItemId(tranItem.itemId)))
      }
    }

    buf.toMap
  }
}
