package functional

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import helpers.Helper.disableMailer
import helpers.UrlHelper
import helpers.UrlHelper._
import org.openqa.selenium.By
import org.openqa.selenium.By._
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
import java.util.concurrent.TimeUnit
import java.sql.Connection
import scala.collection.JavaConversions._
import com.ruimo.scoins.Scoping._

class PriceControlSpec extends Specification with SalesSpecBase {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  "Item Price" should {
    "List price should be used for guest and anonymous users." in {
      val app = FakeApplication(
        additionalConfiguration = inMemoryDatabase(options = Map("MVCC" -> "true")) +
          ("itemPriceStrategy.ANONYMOUS_BUYER.type" -> "models.ListPriceStrategy") +
          ("itemPriceStrategy.GUEST.type" -> "models.ListPriceStrategy") +
          ("anonymousUserPurchase" -> "true") ++
          disableMailer
      )

      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        val adminUser = loginWithTestUser(browser)
        logoff(browser)

        val site = Site.createNew(LocaleInfo.Ja, "Store01")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "外税")
        val taxHis = TaxHistory.createNew(tax, TaxType.OUTER_TAX, BigDecimal("5"), date("9999-12-31"))
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
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        // List price should be used.
        browser.find(".queryItemUnitPrice").getText === "999円"
        browser.find(".addToCartButton.purchaseButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).until("#doLoginButton").areDisplayed()

        // We are not logged in yet.
        browser.title === Messages("commonTitle", Messages("login"))
        browser.find("#doAnonymousLoginButton").click()

        browser.find(".shoppingCartTable .tableBody .unitPrice").getText === "999円"
        browser.find(".shoppingCartTable .tableBody .price").getText === "999円"

        browser.find(".toEnterShippingAddressInner a").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.fill("#firstName").`with`("firstName01")
        browser.fill("#lastName").`with`("lastName01")
        browser.fill("#firstNameKana").`with`("firstNameKana01")
        browser.fill("#lastNameKana").`with`("lastNameKana01")
        browser.fill("#email").`with`("foo@bar.zzz")
        browser.fill("input[name='zip1']").`with`("146")
        browser.fill("input[name='zip2']").`with`("0082")
        browser.fill("#address1").`with`("address01")
        browser.fill("#address2").`with`("address02")
        browser.fill("#tel1").`with`("11111111")

        browser.find("#agreeCheck").click()
        browser.find("#enterShippingAddressForm input[type='submit']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".itemTable .itemTableBody .itemPrice").getText === "999円"
        browser.find(".itemTable .itemTableBody.subtotalWithoutTax .subtotal").getText === "999円"
        browser.find(".itemTable .itemTableBody.outerTax .outerTaxAmount").getText === "49円"
        browser.find(".itemTable .itemTableBody.total .grandTotal").getText === "1,048円"
        browser.find(".shipping .shippingTableBody .boxUnitPrice").getText === "123円"
        browser.find(".shipping .shippingTableBody .boxPrice").getText === "123円"
        browser.find(".salesTotal .salesTotalBody", 0).find(".itemPrice").getText === "1,048円"
        browser.find(".salesTotal .salesTotalBody", 1).find(".itemPrice").getText === "123円"
        browser.find(".salesTotal .salesTotalBody", 2).find(".itemPrice").getText === "1,171円"
      }}
    }
  }
}
