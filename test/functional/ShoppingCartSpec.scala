package functional

import play.api.test.Helpers._
import play.api.Play.current
import helpers.Helper._
import play.api.db.DB
import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.i18n.{Lang, Messages}
import models._
import play.api.test.TestServer
import play.api.test.FakeApplication
import java.sql.Date.{valueOf => date}
import java.util.concurrent.TimeUnit
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxProfile}
import com.ruimo.scoins.Scoping._


class ShoppingCartSpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  "ShoppingCart" should {
    "Show cart dialog" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      val profile = new FirefoxProfile
      profile.setPreference("general.useragent.locale", "ja")
      profile.setPreference("intl.accept_languages", "ja, en")
      val firefox = new FirefoxDriver(profile)
      SeleniumHelpers.running(TestServer(3333, app), firefox) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)

        val site = Site.createNew(LocaleInfo.Ja, "商店1")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "植木"))
        val tax = Tax.createNew
        val taxHistory = TaxHistory.createNew(
          tax, TaxType.INNER_TAX, BigDecimal(5), date("9999-12-31")
        )
        val item = Item.createNew(cat)
        val siteItem = SiteItem.createNew(site, item)
        val itemName = ItemName.createNew(item, Map(LocaleInfo.Ja -> "かえで"))
        val itemDesc = ItemDescription.createNew(item, site, "かえで説明")
        val itemPrice = ItemPrice.createNew(item, site)
        val itemPriceHistory = ItemPriceHistory.createNew(
          itemPrice, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
        )

        val item2 = Item.createNew(cat)
        val siteItem2 = SiteItem.createNew(site, item2)
        val itemName2 = ItemName.createNew(item2, Map(LocaleInfo.Ja -> "松"))
        val itemDesc2 = ItemDescription.createNew(item2, site, "松説明")
        val itemPrice2 = ItemPrice.createNew(item2, site)
        val itemPriceHistory2 = ItemPriceHistory.createNew(
          itemPrice2, tax, CurrencyInfo.Jpy, BigDecimal(777), None, BigDecimal("666"), date("9999-12-31")
        )

        RecommendByAdmin.createNew(site.id.get, item.id.get.id, 10)
        RecommendByAdmin.createNew(site.id.get, item2.id.get.id, 20)

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemQuery.query(List()) + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).until(".addToCartButton").areDisplayed()
        browser.find(".addToCartButton", 0).click()

        browser.await().atMost(5, TimeUnit.SECONDS).until("#cartDialogAddedContent tr .body.itemName").hasSize.greaterThan(0)

        browser.find("#cartDialogAddedContent tr .body.itemName").getText === "かえで"
        browser.find("#cartDialogAddedContent tr .body.siteName").getText === "商店1"
        browser.find("#cartDialogAddedContent tr .body.unitPrice").getText === "999円"
        browser.find("#cartDialogAddedContent tr .body.quantity").getText === "1"
        browser.find("#cartDialogAddedContent tr .body.price").getText === "999円"

        browser.find("#cartDialogCurrentContent tr .body.itemName").getText === "かえで"
        browser.find("#cartDialogCurrentContent tr .body.siteName").getText === "商店1"
        browser.find("#cartDialogCurrentContent tr .body.unitPrice").getText === "999円"
        browser.find("#cartDialogCurrentContent tr .body.quantity").getText === "1"
        browser.find("#cartDialogCurrentContent tr .body.price").getText === "999円"

        doWith(browser.find(".recommendedItem")) { e =>
          e.find("a").getAttribute("href") ===
            "http://localhost:3333" + controllers.routes.ItemDetail.show(item2.id.get.id, site.id.get)
          doWith(e.find("a")) { a =>
            a.find("img").getAttribute("src") ===
              "http://localhost:3333" + controllers.routes.ItemPictures.getPicture(item2.id.get.id, 0)
            a.find("div.itemName").getText === "松"
            a.find("div.price").getText === "777円"
          }
        }

        // Close button
        browser.find(".ui-dialog-buttonset button").get(0).click()

        browser.find(".addToCartButton", 0).click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.await().atMost(5, TimeUnit.SECONDS).until("#cartDialogCurrentContent tr .body.quantity").hasText("2")
        browser.find("#cartDialogAddedContent tr .body.itemName").getText === "かえで"
        browser.find("#cartDialogAddedContent tr .body.siteName").getText === "商店1"
        browser.find("#cartDialogAddedContent tr .body.unitPrice").getText === "999円"
        browser.find("#cartDialogAddedContent tr .body.quantity").getText === "1"
        browser.find("#cartDialogAddedContent tr .body.price").getText === "999円"

        browser.find("#cartDialogCurrentContent tr .body.itemName").getText === "かえで"
        browser.find("#cartDialogCurrentContent tr .body.siteName").getText === "商店1"
        browser.find("#cartDialogCurrentContent tr .body.unitPrice").getText === "999円"
        browser.find("#cartDialogCurrentContent tr .body.quantity").getText === "2"
        browser.find("#cartDialogCurrentContent tr .body.price").getText === "1,998円"

        // Cart button
        browser.find(".ui-dialog-buttonset button").get(1).click()

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded
        browser.title() === Messages("commonTitle") + " " + Messages("shopping.cart")
      }}
    }
  }
}
