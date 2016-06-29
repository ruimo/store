package functional

import anorm._
import play.api.test.Helpers._
import play.api.Play.current
import helpers.Helper._
import models._
import org.joda.time.format.DateTimeFormat
import play.api.db.DB
import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.i18n.{Lang, Messages}
import java.sql.Date.{valueOf => date}
import LocaleInfo._
import java.sql.Connection
import scala.collection.JavaConversions._
import scala.collection.immutable
import com.ruimo.scoins.Scoping._
import java.util.concurrent.TimeUnit

class AnonymousBuyerSpec extends Specification with SalesSpecBase {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  "Anonymous buyer" should {
    "If anonymousUserPurchase is false, purchase by anonymous should not be shown." in {
      val app = FakeApplication(
        additionalConfiguration = inMemoryDatabase() ++ defaultConf + ("anonymousUserPurchase" -> false)
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
        val siteItem = SiteItem.createNew(site, item)
        val itemClass = 1L
        SiteItemNumericMetadata.createNew(site.id.get, item.id.get, SiteItemNumericMetadataType.SHIPPING_SIZE, itemClass)
        val itemName = ItemName.createNew(item, Map(LocaleInfo.Ja -> "かえで"))
        val itemDesc = ItemDescription.createNew(item, site, "かえで説明")
        val itemPrice = ItemPrice.createNew(item, site)
        val itemPriceHistory = ItemPriceHistory.createNew(
          itemPrice, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
        )

        browser.goTo("http://localhost:3333" + itemQueryUrl())
        browser.await().atMost(5, TimeUnit.SECONDS).until(".addToCartButton").areDisplayed()
        browser.find(".addToCartButton").click()

        browser.find("#doAnonymousLoginButton").size === 0
      }}
    }

    "If anonymousUserPurchase is true, purchase by anonymous is permitted." in {
      val app = FakeApplication(
        additionalConfiguration = inMemoryDatabase() ++ defaultConf +
          ("anonymousUserPurchase" -> true) +
          ("acceptableTenders.ANONYMOUS_BUYER" -> List("PAYPAL"))
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
        val siteItem = SiteItem.createNew(site, item)
        val itemClass = 1L
        SiteItemNumericMetadata.createNew(site.id.get, item.id.get, SiteItemNumericMetadataType.SHIPPING_SIZE, itemClass)
        val itemName = ItemName.createNew(item, Map(LocaleInfo.Ja -> "かえで"))
        val itemDesc = ItemDescription.createNew(item, site, "かえで説明")
        val itemPrice = ItemPrice.createNew(item, site)
        val itemPriceHistory = ItemPriceHistory.createNew(
          itemPrice, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
        )
        val box = ShippingBox.createNew(site.id.get, itemClass, 3, "box01")
        val fee = ShippingFee.createNew(box.id.get, CountryCode.JPN, JapanPrefecture.東京都.code())
        val feeHistory = ShippingFeeHistory.createNew(fee.id.get, tax.id.get, BigDecimal(123), Some(100), date("9999-12-31"))

        browser.goTo("http://localhost:3333" + itemQueryUrl())
        browser.await().atMost(5, TimeUnit.SECONDS).until(".addToCartButton").areDisplayed()
        browser.find(".addToCartButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#doAnonymousLoginButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("commonTitle", Messages("shopping.cart"))
        browser.find(".shoppingCartTable tr", 1).find("td", 0).getText === "かえで"

        // Anonymou user should be registered.
        val users: Seq[StoreUser] = StoreUser.all
        users.size === 2
        doWith(users.filter(_.userName != "administrator").head) { u =>
          u.userName.startsWith("anon") === true
          u.firstName === Messages("guest")
          u.middleName === None
          u.lastName === ""
          u.email === ""
          u.userRole === UserRole.ANONYMOUS
          u.companyName === None
        }

        // Order history link should not be shown.
        browser.find(".orderHistoryLink").size === 0

        browser.find(".toEnterShippingAddressInner a").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find("#loginWelcomeMessage").getText === WelcomeMessage.welcomeMessage

        browser.title === Messages("commonTitle", Messages("enter.shipping.address"))
        browser.fill("#firstName").`with`("firstname")
        browser.fill("#lastName").`with`("lastname")
        browser.fill("#firstNameKana").`with`("firstnamekana")
        browser.fill("#lastNameKana").`with`("lastnamekana")
        browser.fill("#email").`with`("null@aaa.com")
        browser.fill("input[name='zip1']").`with`("123")
        browser.fill("input[name='zip2']").`with`("2345")
        browser.find("#prefecture option[value='13']").click()
        browser.fill("#address1").`with`("address01")
        browser.fill("#address2").`with`("address02")
        browser.fill("#address3").`with`("address03")
        browser.fill("#tel1").`with`("12345678")

        if (browser.find("#agreeCheck").size != 0) {
          browser.find("#agreeCheck").click()
        }
        browser.find("input[type='submit']").click()

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("button.payByAccountingBill").size === 0
        browser.find("#paypalimg").size === 1
      }}
    }
  }
}

