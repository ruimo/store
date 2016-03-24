package controllers

import scala.util.{Try, Success, Failure}
import java.net.URLDecoder
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.ws.{WSResponse, WS}
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import helpers.Cache
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
import java.util.regex.Pattern

object Shipping extends Controller with NeedLogin with HasLogger with I18nAware {
  import NeedLogin._
  val NameValuePattern = Pattern.compile("=")

  val tenderTypeForm = Form(
    single(
      "tenderType" -> text
    )
  )

  val UrlBase: () => String = Cache.config(
    _.getString("urlBase").getOrElse(
      throw new IllegalStateException("Specify urlBase in configuration.")
    )
  )

  val PaypalApiUrl: () => String = Cache.config(
    _.getString("paypal.apiUrl").getOrElse(
      throw new IllegalStateException("Specify paypal.apiUrl in configuration.")
    )
  )

  val PaypalApiVersion: () => String = Cache.config(
    _.getString("paypal.apiVersion").getOrElse(
      throw new IllegalStateException("Specify paypal.apiVersion in configuration.")
    )
  )

  val PaypalUser: () => String = Cache.config(
    _.getString("paypal.user").getOrElse(
      throw new IllegalStateException("Specify paypal.user in configuration.")
    )
  )

  val PaypalPassword: () => String = Cache.config(
    _.getString("paypal.password").getOrElse(
      throw new IllegalStateException("Specify paypal.password in configuration.")
    )
  )

  val PaypalSignature: () => String = Cache.config(
    _.getString("paypal.signature").getOrElse(
      throw new IllegalStateException("Specify paypal.signature in configuration.")
    )
  )

  val PaypalRedirectUrl: () => String = Cache.config(
    _.getString("paypal.redirectUrl").getOrElse(
      throw new IllegalStateException("Specify paypal.redirectUrl in configuration.")
    )
  )

  val PaypalLocaleCode: () => String = Cache.config(
    _.getString("paypal.localeCode").getOrElse(
      throw new IllegalStateException("Specify paypal.localeCode in configuration.")
    )
  )

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
        val shippingDate = ShoppingCartShipping.find(login.userId).map(t => new DateTime(t)).getOrElse(new DateTime().plusDays(5))
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
          if (login.isAnonymousBuyer) {
            login.update(newShippingAddress)
          }
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

  def finalizeTransactionJa = NeedAuthenticated.async { implicit request =>
    implicit val login = request.user
    finalizeTransaction(CurrencyInfo.Jpy)
  }

  def finalizeTransaction(
    currency: CurrencyInfo
  )(
    implicit request: Request[AnyContent], login: LoginSession
  ): Future[Result] = tenderTypeForm.bindFromRequest.fold(
    formWithErrors => {
      logger.error("Unknown error. Tender type is not specified? form: " + formWithErrors)
      Future.successful(Redirect(routes.Shipping.confirmShippingAddressJa()))
    },
    transactionType => DB.withTransaction { implicit conn =>
      val (cart: ShoppingCartTotal, expErrors: Seq[ItemExpiredException]) =
        ShoppingCartItem.listItemsForUser(LocaleInfo.getDefault, login.userId)

      if (! expErrors.isEmpty) {
        Future.successful(Ok(views.html.itemExpired(expErrors)))
      }
      else if (cart.isEmpty) {
        logger.error("Shipping.finalizeTransaction(): shopping cart is empty.")
        throw new Error("Shipping.finalizeTransaction(): shopping cart is empty.")
      }
      else {
        val exceedStock: immutable.Map[(ItemId, Long), (String, String, Int, Long)] =
          ShoppingCartItem.itemsExceedStock(login.userId, LocaleInfo.getDefault)

        if (! exceedStock.isEmpty) {
          logger.error("Item exceed stock. " + exceedStock)
          Future.successful(Ok(views.html.itemStockExhausted(exceedStock)))
        }
        else if (ShoppingCartItem.isAllCoupon(login.userId)) {
          val persister = new TransactionPersister()
          val (tranId: Long, taxesBySite: immutable.Map[Site, immutable.Seq[TransactionLogTax]]) =
            persister.persist(
              Transaction(login.userId, currency, cart, None, ShippingTotal(), ShippingDate())
            )
          ShoppingCartItem.removeForUser(login.userId)
          ShoppingCartShipping.removeForUser(login.userId)
          val tran = persister.load(tranId, LocaleInfo.getDefault)
          NotificationMail.orderCompleted(loginSession.get, tran, None)
          RecommendEngine.onSales(loginSession.get, tran, None)
          Future.successful(
            Ok(views.html.showTransactionJa(tran, None, textMetadata(tran), siteItemMetadata(tran)))
          )
        }
        else {
          transactionType match {
            case "payByAccountingBill" => Future.successful(payByAccountingBill(currency, cart))
            case "payByPaypal" => payByPaypal(currency, cart)
          }
        }
      }
    }
  )

  def payByAccountingBill(
    currency:CurrencyInfo, cart: ShoppingCartTotal
  )(
    implicit request: Request[AnyContent], login: LoginSession, conn: Connection
  ): Result = {
    val his = ShippingAddressHistory.list(login.userId).head
    val addr = Address.byId(his.addressId)
    val shippingDateBySite = cart.sites.foldLeft(LongMap[ShippingDateEntry]()) { (sum, e) =>
      sum.updated(e.id.get, ShippingDateEntry(e.id.get, ShoppingCartShipping.find(login.userId, e.id.get)))
    }
    try {
      val persister = new TransactionPersister()
      val (tranId: Long, taxesBySite: immutable.Map[Site, immutable.Seq[TransactionLogTax]])
        = persister.persist(
          Transaction(
            login.userId, currency, cart, Some(addr), shippingFee(addr, cart), ShippingDate(shippingDateBySite)
          )
        )
      ShoppingCartItem.removeForUser(login.userId)
      ShoppingCartShipping.removeForUser(login.userId)
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

  def payByPaypal(
    currency:CurrencyInfo, cart: ShoppingCartTotal
  )(
    implicit request: Request[AnyContent], login: LoginSession
  ): Future[Result] = DB.withConnection { implicit conn =>
    val his = ShippingAddressHistory.list(login.userId).head
    val addr = Address.byId(his.addressId)
    val shippingDateBySite = cart.sites.foldLeft(LongMap[ShippingDateEntry]()) { (sum, e) =>
      sum.updated(e.id.get, ShippingDateEntry(e.id.get, ShoppingCartShipping.find(login.userId, e.id.get)))
    }

    try {
      val (tranId: Long, taxesBySite: immutable.Map[Site, immutable.Seq[TransactionLogTax]], token: Long) = {
        val persister = new TransactionPersister()
        persister.persistPaypal(
          Transaction(
            login.userId, currency, cart, Some(addr), shippingFee(addr, cart), ShippingDate(shippingDateBySite)
          )
        )
      }

      val successUrl = UrlBase() + routes.Paypal.onSuccess(tranId, token).url
      val cancelUrl = UrlBase() + routes.Shipping.cancelPaypal().url
      val resp: Future[WSResponse] = WS.url(PaypalApiUrl()).post(
        Map(
          "USER" -> Seq(PaypalUser()),
          "PWD" -> Seq(PaypalPassword()),
          "SIGNATURE" -> Seq(PaypalSignature()),
          "VERSION" -> Seq(PaypalApiVersion()),
          "PAYMENTREQUEST_0_PAYMENTACTION" -> Seq("Sale"),
          "PAYMENTREQUEST_0_AMT" -> Seq((cart.total + cart.outerTaxTotal).toString),
          "PAYMENTREQUEST_0_CURRENCYCODE" -> Seq(currency.currencyCode),
          "PAYMENTREQUEST_0_INVNUM" -> Seq(tranId.toString),
          "RETURNURL" -> Seq("http://ruimo.com"),
          "CANCELURL" -> Seq(cancelUrl),
          "METHOD" -> Seq("SetExpressCheckout"),
          "SOLUTIONTYPE" -> Seq("Sole"),
          "LANDINGPAGE" -> Seq("Billing"),
          "LOCALECODE" -> Seq(PaypalLocaleCode())
        )
      )

      resp.map { resp =>
        val body = URLDecoder.decode(resp.body, "UTF-8")
        logger.info("Paypal response: '" + body + "'")
        val values: immutable.Map[String, String] = immutable.Map[String, String]() ++ body.split("&").map { s =>
          val a = NameValuePattern.split(s, 2)
          a(0) -> a(1)
        }
        logger.info("Paypal response decoded: " + values)
        values("ACK") match {
          case "Success" =>
            TransactionLogPaypalStatus.update(tranId, PaypalStatus.PREPARED)
            Redirect(
              PaypalRedirectUrl(),
              Map(
                "cmd" -> Seq("_express-checkout"),
                "token" -> Seq(values("TOKEN"))
              )
            )
          case _ =>
            TransactionLogPaypalStatus.update(tranId, PaypalStatus.ERROR)
            throw new Error("Cannot start paypal checkout: '" + body + "'")
        }
      }
    }
    catch {
      case e: CannotShippingException => {
        Future.successful(
          Ok(views.html.cannotShipJa(cannotShip(cart, e, addr), addr, e.itemClass))
        )
      }
    }
  }

  def cancelPaypal = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    Ok(views.html.cancelPaypal())
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
