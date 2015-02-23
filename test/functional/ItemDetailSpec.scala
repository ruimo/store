package functional

import java.util.concurrent.TimeUnit
import play.api.test._
import play.api.test.Helpers._
import play.api.Play.current
import java.sql.Connection

import helpers.Helper._

import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.i18n.{Lang, Messages}
import models._
import play.api.db.DB
import play.api.test.TestServer
import play.api.test.FakeApplication
import java.sql.Date.{valueOf => date}
import controllers.ItemPictures
import java.nio.file.Files
import java.util
import java.nio.charset.Charset
import java.net.{HttpURLConnection, URL}
import java.io.{BufferedReader, InputStreamReader}
import java.text.SimpleDateFormat
import java.sql.Date.{valueOf => date}
import helpers.{ViewHelpers, QueryString}
import com.ruimo.scoins.Scoping._

class ItemDetailSpec extends Specification {
  "Item detail" should {
    "Can show list price with memo" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit def date2milli(d: java.sql.Date) = d.getTime
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site = Site.createNew(LocaleInfo.Ja, "Store01")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "tax01")
        val taxHis = TaxHistory.createNew(tax, TaxType.OUTER_TAX, BigDecimal("5"), date("9999-12-31"))

        val item = Item.createNew(cat)
        val siteItem = SiteItem.createNew(site, item)
        val itemName = ItemName.createNew(item, Map(LocaleInfo.Ja -> "かえで"))
        val itemDesc = ItemDescription.createNew(item, site, "かえで説明")
        val itemPrice = ItemPrice.createNew(item, site)
        val itemPriceHistory = ItemPriceHistory.createNew(
          itemPrice, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
        )

        browser.goTo(
          "http://localhost:3333"
          + controllers.routes.ItemDetail.show(item.id.get.id, site.id.get).url + "&lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".memo").getTexts.size === 0

        // add price memo
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.startChangeItem(item.id.get.id).url + "&lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        doWith(browser.find("#addSiteItemTextMetadataForm")) { form =>
          form.find(
            "option[value=\"" + SiteItemTextMetadataType.PRICE_MEMO.ordinal + "\"]"
          ).click()

          browser.fill("#addSiteItemTextMetadataForm #metadata").`with`("Price memo")

          form.find("input[type='submit']").click()
        }
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.goTo(
          "http://localhost:3333"
          + controllers.routes.ItemDetail.show(item.id.get.id, site.id.get).url + "&lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".itemDetailItemPrice .value .memo").getText === "Price memo"

        // add list price
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.startChangeItem(item.id.get.id).url + "&lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.fill("#itemPrices_0__listPrice").`with`("3000")
        browser.find("#changeItemPriceButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        // add list price memo
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.startChangeItem(item.id.get.id).url + "&lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        doWith(browser.find("#addSiteItemTextMetadataForm")) { form =>
          form.find(
            "option[value=\"" + SiteItemTextMetadataType.LIST_PRICE_MEMO.ordinal + "\"]"
          ).click()

          browser.fill("#addSiteItemTextMetadataForm #metadata").`with`("List price memo")

          form.find("input[type='submit']").click()
        }
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.goTo(
          "http://localhost:3333"
          + controllers.routes.ItemDetail.show(item.id.get.id, site.id.get).url + "&lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".itemDetailListPrice .value .memo").getText === "List price memo"
      }}
    }
  }
}
