package functional

import views.Titles
import helpers.UrlHelper
import helpers.UrlHelper._
import play.api.test._
import play.api.test.Helpers._
import play.api.Play.current
import java.sql.Connection
import models._
import helpers.ViewHelpers

import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.db.DB
import play.api.test.TestServer
import play.api.test.FakeApplication
import java.util.concurrent.TimeUnit
import helpers.Helper._
import java.sql.Date.{valueOf => date}
import play.api.i18n.{Lang, Messages}
import com.ruimo.scoins.Scoping._

class PaypalSpec extends Specification with SalesSpecBase {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  "Paypal" should {
    "Normal paypal transaction." in {
      val app = FakeApplication(
        additionalConfiguration =
          inMemoryDatabase() ++ disableMailer +
          ("anonymousUserPurchase" -> true) +
          ("fakePaypalRespons.enabled" -> true) +
          ("acceptableTenders.ANONYMOUS_BUYER" -> List("PAYPAL")) +
          ("fakePaypalRespons.body" -> "TOKEN=PAYPALTOKEN&CORRELATIONID=AAA&ACK=Success&VERSION=124.0&BUILD=18316154") +
          ("fakePaypalRespons.statusCode" -> "200") +
          (
            "paypal.redirectUrl" -> (
              "http://localhost:3333" + controllers.routes.Paypal.fakePaypal("", "").url.takeWhile(_ != '?')
            )
          )
      )
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val adminUser = loginWithTestUser(browser)
        logoff(browser)

        val site = Site.createNew(LocaleInfo.Ja, "Store01")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "外税")
        val taxHis = TaxHistory.createNew(tax, TaxType.OUTER_TAX, BigDecimal("5"), date("9999-12-31"))
        val tax2 = Tax.createNew
        val taxName2 = TaxName.createNew(tax2, LocaleInfo.Ja, "内税")
        val taxHis2 = TaxHistory.createNew(tax2, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))
        val item = Item.createNew(cat)
        ItemNumericMetadata.createNew(
          item, ItemNumericMetadataType.HEIGHT, 1
        )
        ItemTextMetadata.createNew(
          item, ItemTextMetadataType.ABOUT_HEIGHT, "Hello"
        )
        SiteItemNumericMetadata.createNew(
          site.id.get, item.id.get, SiteItemNumericMetadataType.STOCK, 20
        )
        SiteItemTextMetadata.createNew(
          site.id.get, item.id.get, SiteItemTextMetadataType.PRICE_MEMO, "World"
        )
        val siteItem = SiteItem.createNew(site, item)
        val itemClass = 1L
        SiteItemNumericMetadata.createNew(site.id.get, item.id.get, SiteItemNumericMetadataType.SHIPPING_SIZE, itemClass)
        val itemName = ItemName.createNew(item, Map(LocaleInfo.Ja -> "かえで"))
        val itemDesc = ItemDescription.createNew(item, site, "かえで説明")
        val itemPrice = ItemPrice.createNew(item, site)
        val iph = ItemPriceHistory.createNew(
          itemPrice, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
        )

        browser.goTo("http://localhost:3333" + itemQueryUrl())
        browser.await().atMost(5, TimeUnit.SECONDS).until(".addToCartButton").areDisplayed()
        browser.find(".addToCartButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).until("#doAnonymousLoginButton").areDisplayed()

        browser.find("#doAnonymousLoginButton").size === 1
        browser.find("#doAnonymousLoginButton").click()

        browser.find(".toEnterShippingAddress a").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        val box = ShippingBox.createNew(site.id.get, itemClass, 3, "box01")
        val fee = ShippingFee.createNew(box.id.get, CountryCode.JPN, JapanPrefecture.東京都.code())
        val feeHistory = ShippingFeeHistory.createNew(fee.id.get, tax2.id.get, BigDecimal(123), Some(100), date("9999-12-31"))

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.fill("#firstName").`with`("firstName01")
        browser.fill("#lastName").`with`("lastName01")
        browser.fill("#firstNameKana").`with`("firstNameKana01")
        browser.fill("#lastNameKana").`with`("lastNameKana01")
        browser.fill("#email").`with`("foo@bar.com")
        browser.fill("input[name='zip1']").`with`("146")
        browser.fill("input[name='zip2']").`with`("0082")
        browser.fill("#address1").`with`("address01")
        browser.fill("#address2").`with`("address02")
        browser.fill("#tel1").`with`("11111111")

        browser.find("#agreeCheck").click()
        browser.find("#enterShippingAddressForm input[type='submit']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#paypalimg").size === 1
        browser.find("#paypalimg").click()

        browser.find("#cmd").getText === "_express-checkout"
        browser.find("#token").getText === "PAYPALTOKEN"

        DB.withConnection { implicit conn =>
          val headers: Seq[TransactionLogHeader] = TransactionLogHeader.list()
          headers.size === 1
          headers(0).transactionType === TransactionTypeCode.PAYPAL_EXPRESS_CHECKOUT

          val paypalTran: TransactionLogPaypalStatus = TransactionLogPaypalStatus.byTransactionId(headers(0).id.get)
          paypalTran.transactionId === headers(0).id.get
          paypalTran.status === PaypalStatus.PREPARED

          browser.goTo(
            "http://localhost:3333" + controllers.routes.Paypal.onSuccess(
              paypalTran.transactionId + 1, paypalTran.token
            ).url.addParm("lang", lang.code)
          )
          browser.title === Messages("commonTitle", Titles.top).trim
          doWith(TransactionLogPaypalStatus.byTransactionId(headers(0).id.get)) { paypal =>
            paypal.status === PaypalStatus.PREPARED
          }

          browser.goTo(
            "http://localhost:3333" + controllers.routes.Paypal.onSuccess(
              paypalTran.transactionId, paypalTran.token + 1
            ).url.addParm("lang", lang.code)
          )
          browser.title === Messages("commonTitle", Titles.top).trim
          doWith(TransactionLogPaypalStatus.byTransactionId(headers(0).id.get)) { paypal =>
            paypal.status === PaypalStatus.PREPARED
          }

          browser.goTo(
            "http://localhost:3333" + controllers.routes.Paypal.onSuccess(
              paypalTran.transactionId, paypalTran.token
            ).url.addParm("lang", lang.code)
          )
          browser.title === Messages("commonTitle", Messages("end.transaction"))

          browser.find(".itemTableBody", 0).find(".itemNameBody").getText === "かえで"
          browser.find(".itemTableBody", 0).find(".siteName").getText === "Store01"
          browser.find(".itemTableBody", 0).find(".quantity").getText === "1"
          browser.find(".itemTableBody", 0).find(".itemPrice").getText === "999円"
          browser.find(".itemTableBody", 1).find(".subtotal").getText === "999円"

          browser.find(".itemTableBody", 2).find(".outerTaxAmount").getText ===
              ViewHelpers.toAmount(BigDecimal((999 * 0.05).asInstanceOf[Int]))
          browser.find(".itemTableBody", 3).find(".grandTotal").getText === 
              ViewHelpers.toAmount(BigDecimal((999 * 1.05).asInstanceOf[Int]))

          browser.find(".salesTotalBody", 0).find(".itemQuantity").getText === "1"
          browser.find(".salesTotalBody", 0).find(".itemPrice").getText === "1,048円"
          browser.find(".salesTotalBody", 1).find(".itemQuantity").getText === "1 箱"
          browser.find(".salesTotalBody", 1).find(".itemPrice").getText === "123円"
          browser.find(".salesTotalBody", 2).find(".itemPrice").getText === "1,171円"

          browser.find(".shippingAddress").find(".name").getText === "firstName01 lastName01"
          browser.find(".shippingAddress").find(".nameKana").getText === "firstNameKana01 lastNameKana01"
          browser.find(".shippingAddress").find(".zip").getText === "146 - 0082"
          browser.find(".shippingAddress").find(".prefecture").getText === "東京都"
          browser.find(".shippingAddress").find(".address1").getText === "address01"
          browser.find(".shippingAddress").find(".address2").getText === "address02"
          browser.find(".shippingAddress").find(".tel1").getText === "11111111"

          doWith(TransactionLogPaypalStatus.byTransactionId(headers(0).id.get)) { paypal =>
            paypal.transactionId === headers(0).id.get
            paypal.status === PaypalStatus.COMPLETED
          }
        }
      }}
    }

    "Paypal transaction error." in {
      val app = FakeApplication(
        additionalConfiguration =
          inMemoryDatabase() ++ disableMailer +
          ("anonymousUserPurchase" -> true) +
          ("fakePaypalRespons.enabled" -> true) +
          ("acceptableTenders.ANONYMOUS_BUYER" -> List("PAYPAL")) +
          ("fakePaypalRespons.body" -> "ACK=Fail&VERSION=124.0&BUILD=18316154") +
          ("fakePaypalRespons.statusCode" -> "200") +
          (
            "paypal.redirectUrl" -> (
              "http://localhost:3333" + controllers.routes.Paypal.fakePaypal("", "").url.takeWhile(_ != '?')
            )
          )
      )
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val adminUser = loginWithTestUser(browser)
        logoff(browser)

        val site = Site.createNew(LocaleInfo.Ja, "Store01")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "内税")
        val taxHis = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))
        val item = Item.createNew(cat)
        ItemNumericMetadata.createNew(
          item, ItemNumericMetadataType.HEIGHT, 1
        )
        ItemTextMetadata.createNew(
          item, ItemTextMetadataType.ABOUT_HEIGHT, "Hello"
        )
        SiteItemNumericMetadata.createNew(
          site.id.get, item.id.get, SiteItemNumericMetadataType.STOCK, 20
        )
        SiteItemTextMetadata.createNew(
          site.id.get, item.id.get, SiteItemTextMetadataType.PRICE_MEMO, "World"
        )
        val siteItem = SiteItem.createNew(site, item)
        val itemClass = 1L
        SiteItemNumericMetadata.createNew(site.id.get, item.id.get, SiteItemNumericMetadataType.SHIPPING_SIZE, itemClass)
        val itemName = ItemName.createNew(item, Map(LocaleInfo.Ja -> "かえで"))
        val itemDesc = ItemDescription.createNew(item, site, "かえで説明")
        val itemPrice = ItemPrice.createNew(item, site)
        val iph = ItemPriceHistory.createNew(
          itemPrice, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
        )

        browser.goTo("http://localhost:3333" + itemQueryUrl())
        browser.await().atMost(5, TimeUnit.SECONDS).until(".addToCartButton").areDisplayed()
        browser.find(".addToCartButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).until("#doAnonymousLoginButton").areDisplayed()

        browser.find("#doAnonymousLoginButton").size === 1
        browser.find("#doAnonymousLoginButton").click()

        browser.find(".toEnterShippingAddress a").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        val box = ShippingBox.createNew(site.id.get, itemClass, 3, "box01")
        val fee = ShippingFee.createNew(box.id.get, CountryCode.JPN, JapanPrefecture.東京都.code())
        val feeHistory = ShippingFeeHistory.createNew(fee.id.get, tax.id.get, BigDecimal(123), Some(100), date("9999-12-31"))

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.fill("#firstName").`with`("firstName01")
        browser.fill("#lastName").`with`("lastName01")
        browser.fill("#firstNameKana").`with`("firstNameKana01")
        browser.fill("#lastNameKana").`with`("lastNameKana01")
        browser.fill("#email").`with`("foo@bar.com")
        browser.fill("input[name='zip1']").`with`("146")
        browser.fill("input[name='zip2']").`with`("0082")
        browser.fill("#address1").`with`("address01")
        browser.fill("#address2").`with`("address02")
        browser.fill("#tel1").`with`("11111111")

        browser.find("#agreeCheck").click()
        browser.find("#enterShippingAddressForm input[type='submit']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#paypalimg").size === 1
        browser.find("#paypalimg").click()

        browser.title === Messages("commonTitle", Messages("paypalErrorTitle"))

        DB.withConnection { implicit conn =>
          val headers: Seq[TransactionLogHeader] = TransactionLogHeader.list()
          headers.size === 1
          headers(0).transactionType === TransactionTypeCode.PAYPAL_EXPRESS_CHECKOUT

          val paypalTran: TransactionLogPaypalStatus = TransactionLogPaypalStatus.byTransactionId(headers(0).id.get)
          paypalTran.transactionId === headers(0).id.get
          paypalTran.status === PaypalStatus.ERROR

          // on success should be rejected.
          browser.goTo(
            "http://localhost:3333" + controllers.routes.Paypal.onSuccess(
              paypalTran.transactionId, paypalTran.token
            ).url.addParm("lang", lang.code)
          )
          browser.title === Messages("commonTitle", Titles.top).trim
          doWith(TransactionLogPaypalStatus.byTransactionId(headers(0).id.get)) { paypal =>
            paypal.status === PaypalStatus.ERROR
          }
        }
      }}
    }

    "Paypal cancel transaction." in {
      val app = FakeApplication(
        additionalConfiguration =
          inMemoryDatabase() ++ disableMailer +
          ("anonymousUserPurchase" -> true) +
          ("fakePaypalRespons.enabled" -> true) +
          ("fakePaypalRespons.body" -> "TOKEN=PAYPALTOKEN&CORRELATIONID=AAA&ACK=Success&VERSION=124.0&BUILD=18316154") +
          ("acceptableTenders.ANONYMOUS_BUYER" -> List("PAYPAL")) +
          ("fakePaypalRespons.statusCode" -> "200") +
          (
            "paypal.redirectUrl" -> (
              "http://localhost:3333" + controllers.routes.Paypal.fakePaypal("", "").url.takeWhile(_ != '?')
            )
          )
      )
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val adminUser = loginWithTestUser(browser)
        logoff(browser)

        val site = Site.createNew(LocaleInfo.Ja, "Store01")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "内税")
        val taxHis = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))
        val item = Item.createNew(cat)
        ItemNumericMetadata.createNew(
          item, ItemNumericMetadataType.HEIGHT, 1
        )
        ItemTextMetadata.createNew(
          item, ItemTextMetadataType.ABOUT_HEIGHT, "Hello"
        )
        SiteItemNumericMetadata.createNew(
          site.id.get, item.id.get, SiteItemNumericMetadataType.STOCK, 20
        )
        SiteItemTextMetadata.createNew(
          site.id.get, item.id.get, SiteItemTextMetadataType.PRICE_MEMO, "World"
        )
        val siteItem = SiteItem.createNew(site, item)
        val itemClass = 1L
        SiteItemNumericMetadata.createNew(site.id.get, item.id.get, SiteItemNumericMetadataType.SHIPPING_SIZE, itemClass)
        val itemName = ItemName.createNew(item, Map(LocaleInfo.Ja -> "かえで"))
        val itemDesc = ItemDescription.createNew(item, site, "かえで説明")
        val itemPrice = ItemPrice.createNew(item, site)
        val iph = ItemPriceHistory.createNew(
          itemPrice, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
        )

        browser.goTo("http://localhost:3333" + itemQueryUrl())
        browser.await().atMost(5, TimeUnit.SECONDS).until(".addToCartButton").areDisplayed()
        browser.find(".addToCartButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).until("#doAnonymousLoginButton").areDisplayed()

        browser.find("#doAnonymousLoginButton").size === 1
        browser.find("#doAnonymousLoginButton").click()

        browser.find(".toEnterShippingAddress a").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        val box = ShippingBox.createNew(site.id.get, itemClass, 3, "box01")
        val fee = ShippingFee.createNew(box.id.get, CountryCode.JPN, JapanPrefecture.東京都.code())
        val feeHistory = ShippingFeeHistory.createNew(fee.id.get, tax.id.get, BigDecimal(123), Some(100), date("9999-12-31"))

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.fill("#firstName").`with`("firstName01")
        browser.fill("#lastName").`with`("lastName01")
        browser.fill("#firstNameKana").`with`("firstNameKana01")
        browser.fill("#lastNameKana").`with`("lastNameKana01")
        browser.fill("#email").`with`("foo@bar.com")
        browser.fill("input[name='zip1']").`with`("146")
        browser.fill("input[name='zip2']").`with`("0082")
        browser.fill("#address1").`with`("address01")
        browser.fill("#address2").`with`("address02")
        browser.fill("#tel1").`with`("11111111")

        browser.find("#agreeCheck").click()
        browser.find("#enterShippingAddressForm input[type='submit']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#paypalimg").size === 1
        browser.find("#paypalimg").click()

        browser.find("#cmd").getText === "_express-checkout"
        browser.find("#token").getText === "PAYPALTOKEN"

        DB.withConnection { implicit conn =>
          val headers: Seq[TransactionLogHeader] = TransactionLogHeader.list()
          headers.size === 1
          headers(0).transactionType === TransactionTypeCode.PAYPAL_EXPRESS_CHECKOUT

          val paypalTran: TransactionLogPaypalStatus = TransactionLogPaypalStatus.byTransactionId(headers(0).id.get)
          paypalTran.transactionId === headers(0).id.get
          paypalTran.status === PaypalStatus.PREPARED

          browser.goTo(
            "http://localhost:3333" + controllers.routes.Paypal.onCancel(
              paypalTran.transactionId, paypalTran.token
            ).url.addParm("lang", lang.code)
          )
          browser.title === Messages("commonTitle", Messages("cancelPayaplTitle"))
          doWith(TransactionLogPaypalStatus.byTransactionId(headers(0).id.get)) { paypal =>
            paypal.status === PaypalStatus.CANCELED
          }
        }
      }}
    }
  }
}


