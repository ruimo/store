package functional

import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.db.DB
import play.api.i18n.{Messages, Lang}
import play.api.test.Helpers._
import helpers.Helper._
import play.api.Play.current
import models._
import play.api.test.TestServer
import play.api.test.FakeApplication
import scala.Some
import java.sql.Date.{valueOf => date}

class ItemQuerySpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  "Query" should {
    "Item empty" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
      
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemQuery.query(List(""), 0, 10).url + "&lang=" + lang.code
        )

        browser.title === Messages("item.list")
        browser.$("table.queryItemTable").find("tr").size() === 1 // Since no item found, only header will be shown.
      }}
    }

    "Query with single item" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = StoreUser.create(
          "userName", "firstName", Some("middleName"), "lastName", "email",
          1L, 2L, UserRole.ADMIN, Some("companyName")
        )
        
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
          itemPrice, tax, CurrencyInfo.Jpy, BigDecimal(999), BigDecimal("888"), date("9999-12-31")
        )
        
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemQuery.query(List(""), 0, 10).url + "&lang=" + lang.code
        )

        browser.title === Messages("item.list")
        val body1 = browser.$("tr.queryItemTableBody")
        body1.size() === 1
        body1.find("td.queryItemItemName").find("a").getText === "かえで"
        body1.find("td.queryItemSite").getText === "商店1"
        body1.find("td.queryItemUnitPrice").getText === "999円"

        // Search by item name
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemQuery.query(List("かえで"), 0, 10).url + "&lang=" + lang.code
        )

        browser.title === Messages("item.list")
        val body2 = browser.$("tr.queryItemTableBody")
        body2.size() === 1
        body2.find("td.queryItemItemName").find("a").getText === "かえで"
        body2.find("td.queryItemSite").getText === "商店1"
        body2.find("td.queryItemUnitPrice").getText === "999円"

        // Search by item description
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemQuery.query(List("かえで説明"), 0, 10).url + "&lang=" + lang.code
        )

        browser.title === Messages("item.list")
        val body3 = browser.$("tr.queryItemTableBody")
        body3.size() === 1
        body3.find("td.queryItemItemName").find("a").getText === "かえで"
        body3.find("td.queryItemSite").getText === "商店1"
        body3.find("td.queryItemUnitPrice").getText === "999円"

        // Search by item description
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemQuery.query(List("もみじ"), 0, 10).url + "&lang=" + lang.code
        )

        browser.title === Messages("item.list")
        val body4 = browser.$("tr.queryItemTableBody")
        body4.size() === 0
      }}
    }

    "Query with two conditions" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = StoreUser.create(
          "userName", "firstName", Some("middleName"), "lastName", "email",
          1L, 2L, UserRole.ADMIN, Some("companyName")
        )
        
        val site = Site.createNew(LocaleInfo.Ja, "商店1")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "植木"))
        val tax = Tax.createNew
        val taxHistory = TaxHistory.createNew(
          tax, TaxType.INNER_TAX, BigDecimal(5), date("9999-12-31")
        )
        val item = Item.createNew(cat)
        val siteItem = SiteItem.createNew(site, item)
        val itemName = ItemName.createNew(item, Map(LocaleInfo.Ja -> "松"))
        val itemDesc = ItemDescription.createNew(item, site, "松 常緑")
        val itemPrice = ItemPrice.createNew(item, site)
        val itemPriceHistory = ItemPriceHistory.createNew(
          itemPrice, tax, CurrencyInfo.Jpy, BigDecimal(999), BigDecimal("888"), date("9999-12-31")
        )
        
        // Search by two conditions name
        browser.goTo(
          "http://localhost:3333" + 
          controllers.routes.ItemQuery.query(List("松", "常緑"), 0, 10).url + "&lang=" + lang.code
        )

        browser.title === Messages("item.list")
        val body2 = browser.$("tr.queryItemTableBody")
        body2.size() === 1
        body2.find("td.queryItemItemName").find("a").getText === "松"
        body2.find("td.queryItemSite").getText === "商店1"
        body2.find("td.queryItemUnitPrice").getText === "999円"

        // Search by conditions where that include no match
        browser.goTo(
          "http://localhost:3333" + 
          controllers.routes.ItemQuery.query(List("松", "常緑", "落葉"), 0, 10).url + "&lang=" + lang.code
        )

        browser.title === Messages("item.list")
        val body4 = browser.$("tr.queryItemTableBody")
        body4.size() === 0
      }}
    }
  }
}
