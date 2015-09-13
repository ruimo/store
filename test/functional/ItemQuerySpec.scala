package functional

import org.openqa.selenium.By;
import java.util.concurrent.TimeUnit
import helpers.UrlHelper
import helpers.UrlHelper._
import anorm._
import helpers.UrlHelper
import helpers.UrlHelper._
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
import controllers.NeedLogin
import com.ruimo.scoins.Scoping._

class ItemQuerySpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  "Query" should {
    // "Item empty" in {
    //   val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
    //   running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
    //     implicit val lang = Lang("ja")
    //     val user = loginWithTestUser(browser)
      
    //     browser.goTo(
    //       "http://localhost:3333" + controllers.routes.ItemQuery.query(List(""), 0, 10).url + "&lang=" + lang.code
    //     )

    //     browser.title === Messages("item.list")
    //     browser.$("table.queryItemTable").find("tr").size() === 1 // Since no item found, only header will be shown.
    //   }}
    // }

    // "Query with single item" in {
    //   val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
    //   running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
    //     implicit val lang = Lang("ja")
    //     val user = StoreUser.create(
    //       "userName", "firstName", Some("middleName"), "lastName", "email",
    //       1L, 2L, UserRole.ADMIN, Some("companyName")
    //     )
        
    //     val site = Site.createNew(LocaleInfo.Ja, "商店1")
    //     val cat = Category.createNew(Map(LocaleInfo.Ja -> "植木"))
    //     val tax = Tax.createNew
    //     val taxHistory = TaxHistory.createNew(
    //       tax, TaxType.INNER_TAX, BigDecimal(5), date("9999-12-31")
    //     )
    //     val item = Item.createNew(cat)
    //     val siteItem = SiteItem.createNew(site, item)
    //     val itemName = ItemName.createNew(item, Map(LocaleInfo.Ja -> "かえで"))
    //     val itemDesc = ItemDescription.createNew(item, site, "かえで説明")
    //     val itemPrice = ItemPrice.createNew(item, site)
    //     val itemPriceHistory = ItemPriceHistory.createNew(
    //       itemPrice, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
    //     )
        
    //     if (NeedLogin.needAuthenticationEntirely) {
    //       loginWithTestUser(browser)
    //     }

    //     browser.goTo(
    //       "http://localhost:3333" + controllers.routes.ItemQuery.query(List(""), 0, 10).url + "&lang=" + lang.code
    //     )

    //     browser.title === Messages("item.list")
    //     doWith(browser.$("tr.queryItemTableBody")) { body =>
    //       body.size() === 1
    //       body.find("td.queryItemItemName").find("a").getText === "かえで"
    //       body.find("td.queryItemSite").getText === "商店1"
    //       body.find("td.queryItemUnitPrice").getText === "999円"
    //     }

    //     browser.goTo(
    //       "http://localhost:3333" + 
    //       controllers.routes.ItemQuery.queryByCategory(List(""), Some(cat.id.get), 0, 10).url + "&lang=" + lang.code
    //     )

    //     browser.title === Messages("item.list")
    //     doWith(browser.$("tr.queryItemTableBody")) { body =>
    //       body.size() === 1
    //       body.find("td.queryItemItemName").find("a").getText === "かえで"
    //       body.find("td.queryItemSite").getText === "商店1"
    //       body.find("td.queryItemUnitPrice").getText === "999円"
    //     }

    //     browser.goTo(
    //       "http://localhost:3333" + 
    //       controllers.routes.ItemQuery.queryByCategory(List(""), Some(cat.id.get + 1), 0, 10).url + "&lang=" + lang.code
    //     )

    //     browser.title === Messages("item.list")
    //     browser.$("tr.queryItemTableBody").size === 0

    //     // Search by item name
    //     browser.goTo(
    //       "http://localhost:3333" + controllers.routes.ItemQuery.query(List("かえで"), 0, 10).url + "&lang=" + lang.code
    //     )

    //     browser.title === Messages("item.list")
    //     doWith(browser.$("tr.queryItemTableBody")) { body =>
    //       body.size() === 1
    //       body.find("td.queryItemItemName").find("a").getText === "かえで"
    //       body.find("td.queryItemSite").getText === "商店1"
    //       body.find("td.queryItemUnitPrice").getText === "999円"
    //     }

    //     // Search by item description
    //     browser.goTo(
    //       "http://localhost:3333" + controllers.routes.ItemQuery.query(List("かえで説明"), 0, 10).url + "&lang=" + lang.code
    //     )

    //     browser.title === Messages("item.list")
    //     doWith(browser.$("tr.queryItemTableBody")) { body =>
    //       body.size() === 1
    //       body.find("td.queryItemItemName").find("a").getText === "かえで"
    //       body.find("td.queryItemSite").getText === "商店1"
    //       body.find("td.queryItemUnitPrice").getText === "999円"
    //     }

    //     browser.goTo(
    //       "http://localhost:3333" + controllers.routes.ItemQuery.query(List("もみじ"), 0, 10).url + "&lang=" + lang.code
    //     )

    //     browser.title === Messages("item.list")
    //     browser.$("tr.queryItemTableBody").size === 0

    //     browser.goTo(
    //       "http://localhost:3333"
    //       + controllers.routes.ItemQuery.queryByCategory(List("もみじ"), Some(cat.id.get), 0, 10).url
    //       + "&lang=" + lang.code
    //     )

    //     browser.title === Messages("item.list")
    //     browser.$("tr.queryItemTableBody").size === 0
    //   }}
    // }

    // "Query with two conditions" in {
    //   val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
    //   running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
    //     implicit val lang = Lang("ja")
    //     val user = StoreUser.create(
    //       "userName", "firstName", Some("middleName"), "lastName", "email",
    //       1L, 2L, UserRole.ADMIN, Some("companyName")
    //     )
        
    //     val site = Site.createNew(LocaleInfo.Ja, "商店1")
    //     val cat = Category.createNew(Map(LocaleInfo.Ja -> "植木"))
    //     val tax = Tax.createNew
    //     val taxHistory = TaxHistory.createNew(
    //       tax, TaxType.INNER_TAX, BigDecimal(5), date("9999-12-31")
    //     )
    //     val item = Item.createNew(cat)
    //     val siteItem = SiteItem.createNew(site, item)
    //     val itemName = ItemName.createNew(item, Map(LocaleInfo.Ja -> "松"))
    //     val itemDesc = ItemDescription.createNew(item, site, "松 常緑")
    //     val itemPrice = ItemPrice.createNew(item, site)
    //     val itemPriceHistory = ItemPriceHistory.createNew(
    //       itemPrice, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
    //     )
        
    //     // Search by two conditions name
    //     browser.goTo(
    //       "http://localhost:3333" + 
    //       controllers.routes.ItemQuery.query(List("松", "常緑"), 0, 10).url + "&lang=" + lang.code
    //     )

    //     browser.title === Messages("item.list")
    //     val body2 = browser.$("tr.queryItemTableBody")
    //     body2.size() === 1
    //     body2.find("td.queryItemItemName").find("a").getText === "松"
    //     body2.find("td.queryItemSite").getText === "商店1"
    //     body2.find("td.queryItemUnitPrice").getText === "999円"

    //     // Search by conditions where that include no match
    //     browser.goTo(
    //       "http://localhost:3333" + 
    //       controllers.routes.ItemQuery.query(List("松", "常緑", "落葉"), 0, 10).url + "&lang=" + lang.code
    //     )

    //     browser.title === Messages("item.list")
    //     val body4 = browser.$("tr.queryItemTableBody")
    //     body4.size() === 0
    //   }}
    // }

    // "Query with category" in {
    //   val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
    //   running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
    //     implicit val lang = Lang("ja")
    //     val user = StoreUser.create(
    //       "userName", "firstName", Some("middleName"), "lastName", "email",
    //       1L, 2L, UserRole.ADMIN, Some("companyName")
    //     )
        
    //     val site = Site.createNew(LocaleInfo.Ja, "商店1")
    //     val cat1 = Category.createNew(Map(LocaleInfo.Ja -> "植木"))
    //     val cat2 = Category.createNew(Map(LocaleInfo.Ja -> "植木2"))
    //     val tax = Tax.createNew
    //     val taxHistory = TaxHistory.createNew(
    //       tax, TaxType.INNER_TAX, BigDecimal(5), date("9999-12-31")
    //     )
    //     val item1 = Item.createNew(cat1)
    //     val item2 = Item.createNew(cat2)
    //     SiteItem.createNew(site, item1)
    //     SiteItem.createNew(site, item2)
    //     ItemName.createNew(item1, Map(LocaleInfo.Ja -> "松"))
    //     ItemName.createNew(item2, Map(LocaleInfo.Ja -> "梅"))
    //     ItemDescription.createNew(item1, site, "松 常緑")
    //     ItemDescription.createNew(item2, site, "梅 常緑")
    //     val itemPrice1 = ItemPrice.createNew(item1, site)
    //     ItemPriceHistory.createNew(
    //       itemPrice1, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
    //     )
    //     val itemPrice2 = ItemPrice.createNew(item2, site)
    //     ItemPriceHistory.createNew(
    //       itemPrice2, tax, CurrencyInfo.Jpy, BigDecimal(333), None, BigDecimal("222"), date("9999-12-31")
    //     )
        
    //     // Search by category
    //     browser.goTo(
    //       "http://localhost:3333" + 
    //       controllers.routes.ItemQuery.queryByCategory(List(), Some(cat1.id.get), 0, 10).url + "&lang=" + lang.code
    //     )

    //     browser.title === Messages("item.list")
    //     doWith(browser.$("tr.queryItemTableBody")) { body =>
    //       body.size() === 1
    //       body.find("td.queryItemItemName").find("a").getText === "松"
    //       body.find("td.queryItemSite").getText === "商店1"
    //       body.find("td.queryItemUnitPrice").getText === "999円"
    //     }

    //     browser.goTo(
    //       "http://localhost:3333" + 
    //       controllers.routes.ItemQuery.queryByCategory(List(), Some(cat2.id.get), 0, 10).url + "&lang=" + lang.code
    //     )

    //     browser.title === Messages("item.list")
    //     doWith(browser.$("tr.queryItemTableBody")) { body =>
    //       body.size() === 1
    //       body.find("td.queryItemItemName").find("a").getText === "梅"
    //       body.find("td.queryItemSite").getText === "商店1"
    //       body.find("td.queryItemUnitPrice").getText === "333円"
    //     }
    //   }}
    // }

    // "Query with site" in {
    //   val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
    //   running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
    //     implicit val lang = Lang("ja")
    //     val user = StoreUser.create(
    //       "userName", "firstName", Some("middleName"), "lastName", "email",
    //       1L, 2L, UserRole.ADMIN, Some("companyName")
    //     )
        
    //     val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
    //     val site2 = Site.createNew(LocaleInfo.Ja, "商店2")
    //     val cat = Category.createNew(Map(LocaleInfo.Ja -> "植木"))
    //     val tax = Tax.createNew
    //     val taxHistory = TaxHistory.createNew(
    //       tax, TaxType.INNER_TAX, BigDecimal(5), date("9999-12-31")
    //     )
    //     val item1 = Item.createNew(cat)
    //     val item2 = Item.createNew(cat)
    //     SiteItem.createNew(site1, item1)
    //     SiteItem.createNew(site2, item2)
    //     ItemName.createNew(item1, Map(LocaleInfo.Ja -> "松"))
    //     ItemName.createNew(item2, Map(LocaleInfo.Ja -> "梅"))
    //     ItemDescription.createNew(item1, site1, "松 常緑")
    //     ItemDescription.createNew(item2, site2, "梅 常緑")
    //     val itemPrice1 = ItemPrice.createNew(item1, site1)
    //     ItemPriceHistory.createNew(
    //       itemPrice1, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
    //     )
    //     val itemPrice2 = ItemPrice.createNew(item2, site2)
    //     ItemPriceHistory.createNew(
    //       itemPrice2, tax, CurrencyInfo.Jpy, BigDecimal(333), None, BigDecimal("222"), date("9999-12-31")
    //     )
        
    //     // Search by site
    //     browser.goTo(
    //       "http://localhost:3333" + 
    //       controllers.routes.ItemQuery.queryBySite(List(), Some(site1.id.get), 0, 10).url + "&lang=" + lang.code
    //     )

    //     browser.title === Messages("item.list")
    //     doWith(browser.$("tr.queryItemTableBody")) { body =>
    //       body.size() === 1
    //       body.find("td.queryItemItemName").find("a").getText === "松"
    //       body.find("td.queryItemSite").getText === "商店1"
    //       body.find("td.queryItemUnitPrice").getText === "999円"
    //     }

    //     browser.goTo(
    //       "http://localhost:3333" + 
    //       controllers.routes.ItemQuery.queryBySite(List(), Some(site2.id.get), 0, 10).url + "&lang=" + lang.code
    //     )

    //     browser.title === Messages("item.list")
    //     doWith(browser.$("tr.queryItemTableBody")) { body =>
    //       body.size() === 1
    //       body.find("td.queryItemItemName").find("a").getText === "梅"
    //       body.find("td.queryItemSite").getText === "商店2"
    //       body.find("td.queryItemUnitPrice").getText === "333円"
    //     }
    //   }}
    // }

    // "Query with site and category" in {
    //   val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
    //   running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
    //     implicit val lang = Lang("ja")

    //     val user = StoreUser.create(
    //       "userName", "firstName", Some("middleName"), "lastName", "email",
    //       1L, 2L, UserRole.ADMIN, Some("companyName")
    //     )
        
    //     val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
    //     val site2 = Site.createNew(LocaleInfo.Ja, "商店2")
    //     val cat1 = Category.createNew(Map(LocaleInfo.Ja -> "植木"))
    //     val cat2 = Category.createNew(Map(LocaleInfo.Ja -> "植木2"))
    //     val tax = Tax.createNew
    //     val taxHistory = TaxHistory.createNew(
    //       tax, TaxType.INNER_TAX, BigDecimal(5), date("9999-12-31")
    //     )
    //     val item1 = Item.createNew(cat1)
    //     val item2 = Item.createNew(cat1)
    //     val item3 = Item.createNew(cat2)
    //     val item4 = Item.createNew(cat2)
    //     val item5 = Item.createNew(cat2)
    //     SiteItem.createNew(site1, item1)
    //     SiteItem.createNew(site2, item2)
    //     SiteItem.createNew(site1, item3)
    //     SiteItem.createNew(site2, item4)
    //     SiteItem.createNew(site2, item5)
    //     ItemName.createNew(item1, Map(LocaleInfo.Ja -> "松"))
    //     ItemName.createNew(item2, Map(LocaleInfo.Ja -> "梅"))
    //     ItemName.createNew(item3, Map(LocaleInfo.Ja -> "桜"))
    //     ItemName.createNew(item4, Map(LocaleInfo.Ja -> "あやめ"))
    //     ItemName.createNew(item5, Map(LocaleInfo.Ja -> "藤"))
    //     ItemDescription.createNew(item1, site1, "松 常緑")
    //     ItemDescription.createNew(item2, site2, "梅 常緑")
    //     ItemDescription.createNew(item3, site1, "桜 常緑")
    //     ItemDescription.createNew(item4, site2, "あやめ 常緑")
    //     ItemDescription.createNew(item5, site2, "藤 常緑")
    //     val itemPrice1 = ItemPrice.createNew(item1, site1)
    //     ItemPriceHistory.createNew(
    //       itemPrice1, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
    //     )
    //     val itemPrice2 = ItemPrice.createNew(item2, site2)
    //     ItemPriceHistory.createNew(
    //       itemPrice2, tax, CurrencyInfo.Jpy, BigDecimal(333), None, BigDecimal("222"), date("9999-12-31")
    //     )
    //     val itemPrice3 = ItemPrice.createNew(item3, site1)
    //     ItemPriceHistory.createNew(
    //       itemPrice3, tax, CurrencyInfo.Jpy, BigDecimal(222), None, BigDecimal("444"), date("9999-12-31")
    //     )
    //     val itemPrice4 = ItemPrice.createNew(item4, site2)
    //     ItemPriceHistory.createNew(
    //       itemPrice4, tax, CurrencyInfo.Jpy, BigDecimal(111), None, BigDecimal("999"), date("9999-12-31")
    //     )
    //     val itemPrice5 = ItemPrice.createNew(item5, site2)
    //     ItemPriceHistory.createNew(
    //       itemPrice5, tax, CurrencyInfo.Jpy, BigDecimal(123), None, BigDecimal("987"), date("9999-12-31")
    //     )
        
    //     // Search by site1 and cat1
    //     browser.goTo(
    //       "http://localhost:3333" + 
    //       controllers.routes.ItemQuery.queryBySiteAndCategory(
    //         List(), Some(site1.id.get), Some(cat1.id.get), 0, 10
    //       ).url.addParm("lang", lang.code)
    //     )

    //     browser.title === Messages("item.list")
    //     doWith(browser.$("tr.queryItemTableBody")) { body =>
    //       body.size() === 1
    //       body.find("td.queryItemItemName").find("a").getText === "松"
    //       body.find("td.queryItemSite").getText === "商店1"
    //       body.find("td.queryItemUnitPrice").getText === "999円"
    //     }

    //     // Search by site1 and cat2
    //     browser.goTo(
    //       "http://localhost:3333" + 
    //       controllers.routes.ItemQuery.queryBySiteAndCategory(
    //         List(), Some(site1.id.get), Some(cat2.id.get), 0, 10
    //       ).url.addParm("lang", lang.code)
    //     )

    //     browser.title === Messages("item.list")
    //     doWith(browser.$("tr.queryItemTableBody")) { body =>
    //       body.size() === 1
    //       body.find("td.queryItemItemName").find("a").getText === "桜"
    //       body.find("td.queryItemSite").getText === "商店1"
    //       body.find("td.queryItemUnitPrice").getText === "222円"
    //     }

    //     // Search by site2 and cat1
    //     browser.goTo(
    //       "http://localhost:3333" + 
    //       controllers.routes.ItemQuery.queryBySiteAndCategory(
    //         List(), Some(site2.id.get), Some(cat1.id.get), 0, 10
    //       ).url.addParm("lang", lang.code)
    //     )

    //     browser.title === Messages("item.list")
    //     doWith(browser.$("tr.queryItemTableBody")) { body =>
    //       body.size() === 1
    //       body.find("td.queryItemItemName").find("a").getText === "梅"
    //       body.find("td.queryItemSite").getText === "商店2"
    //       body.find("td.queryItemUnitPrice").getText === "333円"
    //     }

    //     // Search by site2 and cat2
    //     browser.goTo(
    //       "http://localhost:3333" + 
    //       controllers.routes.ItemQuery.queryBySiteAndCategory(
    //         List(), Some(site2.id.get), Some(cat2.id.get), 0, 10
    //       ).url.addParm("lang", lang.code)
    //     )

    //     browser.title === Messages("item.list")
    //     doWith(browser.$("tr.queryItemTableBody")) { body =>
    //       body.size() === 2
    //       body.find("td.queryItemItemName", 0).find("a").getText === "あやめ"
    //       body.find("td.queryItemSite", 0).getText === "商店2"
    //       body.find("td.queryItemUnitPrice", 0).getText === "111円"
    //       body.find("td.queryItemItemName", 1).find("a").getText === "藤"
    //       body.find("td.queryItemSite", 1).getText === "商店2"
    //       body.find("td.queryItemUnitPrice", 1).getText === "123円"
    //     }
    //   }}
    // }

    // "Query with supplemental category" in {
    //   val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
    //   running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
    //     implicit val lang = Lang("ja")
    //     val user = StoreUser.create(
    //       "userName", "firstName", Some("middleName"), "lastName", "email",
    //       1L, 2L, UserRole.ADMIN, Some("companyName")
    //     )
        
    //     val site = Site.createNew(LocaleInfo.Ja, "商店1")
    //     val cat1 = Category.createNew(Map(LocaleInfo.Ja -> "植木"))
    //     val cat2 = Category.createNew(Map(LocaleInfo.Ja -> "植木2"))
    //     val tax = Tax.createNew
    //     val taxHistory = TaxHistory.createNew(
    //       tax, TaxType.INNER_TAX, BigDecimal(5), date("9999-12-31")
    //     )
    //     val item1 = Item.createNew(cat1)
    //     SiteItem.createNew(site, item1)
    //     ItemName.createNew(item1, Map(LocaleInfo.Ja -> "松"))
    //     ItemDescription.createNew(item1, site, "松 常緑")
    //     val itemPrice1 = ItemPrice.createNew(item1, site)
    //     ItemPriceHistory.createNew(
    //       itemPrice1, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
    //     )
        
    //     // Search by category
    //     browser.goTo(
    //       "http://localhost:3333" + 
    //       controllers.routes.ItemQuery.queryByCategory(List(), Some(cat1.id.get), 0, 10).url + "&lang=" + lang.code
    //     )

    //     browser.title === Messages("item.list")
    //     doWith(browser.$("tr.queryItemTableBody")) { body =>
    //       body.size() === 1
    //       body.find("td.queryItemItemName").find("a").getText === "松"
    //       body.find("td.queryItemSite").getText === "商店1"
    //       body.find("td.queryItemUnitPrice").getText === "999円"
    //     }

    //     browser.goTo(
    //       "http://localhost:3333" + 
    //       controllers.routes.ItemQuery.queryByCategory(List(), Some(cat2.id.get), 0, 10).url + "&lang=" + lang.code
    //     )

    //     browser.title === Messages("item.list")
    //     browser.find("tr.queryItemTableBody").getTexts.size === 0

    //     SupplementalCategory.createNew(item1.id.get, cat2.id.get)

    //     // Search by category
    //     browser.goTo(
    //       "http://localhost:3333" + 
    //       controllers.routes.ItemQuery.queryByCategory(List(), Some(cat1.id.get), 0, 10).url + "&lang=" + lang.code
    //     )

    //     browser.title === Messages("item.list")
    //     doWith(browser.$("tr.queryItemTableBody")) { body =>
    //       body.size() === 1
    //       body.find("td.queryItemItemName").find("a").getText === "松"
    //       body.find("td.queryItemSite").getText === "商店1"
    //       body.find("td.queryItemUnitPrice").getText === "999円"
    //     }

    //     browser.goTo(
    //       "http://localhost:3333" + 
    //       controllers.routes.ItemQuery.queryByCategory(List(), Some(cat2.id.get), 0, 10).url + "&lang=" + lang.code
    //     )

    //     browser.title === Messages("item.list")
    //     doWith(browser.$("tr.queryItemTableBody")) { body =>
    //       body.size() === 1
    //       body.find("td.queryItemItemName").find("a").getText === "松"
    //       body.find("td.queryItemSite").getText === "商店1"
    //       body.find("td.queryItemUnitPrice").getText === "999円"
    //     }
    //   }}
    // }

    "Order by drop down." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)

        val site = Site.createNew(LocaleInfo.Ja, "商店1")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "植木"))
        val tax = Tax.createNew
        val taxHistory = TaxHistory.createNew(
          tax, TaxType.INNER_TAX, BigDecimal(5), date("9999-12-31")
        )
        val item01 = Item.createNew(cat)
        val item02 = Item.createNew(cat)
        val siteItem01 = SiteItem.createNew(site, item01)
        val siteItem02 = SiteItem.createNew(site, item02)
        val itemName01 = ItemName.createNew(item01, Map(LocaleInfo.Ja -> "item 01"))
        val itemName02 = ItemName.createNew(item02, Map(LocaleInfo.Ja -> "item 02"))
        val itemDesc01 = ItemDescription.createNew(item01, site, "desc01")
        val itemDesc02 = ItemDescription.createNew(item02, site, "desc02")
        val itemPrice01 = ItemPrice.createNew(item01, site)
        val itemPrice02 = ItemPrice.createNew(item02, site)
        val itemPriceHistory01 = ItemPriceHistory.createNew(
          itemPrice01, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
        )
        val itemPriceHistory02 = ItemPriceHistory.createNew(
          itemPrice02, tax, CurrencyInfo.Jpy, BigDecimal(222), None, BigDecimal("111"), date("9999-12-31")
        )
        SQL(
          "update site_item set created = {date} where item_id = {id}"
        ).on(
          'date -> new java.sql.Timestamp(siteItem02.created + 1000),
          'id -> item02.id.get.id
        ).executeUpdate()

        browser.goTo(
          "http://localhost:3333" + 
            controllers.routes.ItemQuery.query(
              q = List(), page = 0, pageSize = 10, templateNo = 0
            ).url.addParm("lang", lang.code)
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#sortDropDown option[value='older']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.webDriver.findElement(By.id("sortDropDown"))
          .findElement(By.cssSelector("option[value=\"older\"]")).isSelected === true

        browser.find(".queryItemItemName a", 0).getText === "item 01"
        browser.find(".queryItemItemName a", 1).getText === "item 02"

        browser.find("#sortDropDown option[value='newer']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.webDriver.findElement(By.id("sortDropDown"))
          .findElement(By.cssSelector("option[value=\"newer\"]")).isSelected === true

        browser.find(".queryItemItemName a", 0).getText === "item 02"
        browser.find(".queryItemItemName a", 1).getText === "item 01"

        browser.find("#sortDropDown option[value='name']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.webDriver.findElement(By.id("sortDropDown"))
          .findElement(By.cssSelector("option[value=\"name\"]")).isSelected === true

        browser.find(".queryItemItemName a", 0).getText === "item 01"
        browser.find(".queryItemItemName a", 1).getText === "item 02"

        browser.find("#sortDropDown option[value='nameReverse']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.webDriver.findElement(By.id("sortDropDown"))
          .findElement(By.cssSelector("option[value=\"nameReverse\"]")).isSelected === true

        browser.find(".queryItemItemName a", 0).getText === "item 02"
        browser.find(".queryItemItemName a", 1).getText === "item 01"

        browser.find("#sortDropDown option[value='price']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.webDriver.findElement(By.id("sortDropDown"))
          .findElement(By.cssSelector("option[value=\"price\"]")).isSelected === true

        browser.find(".queryItemItemName a", 0).getText === "item 02"
        browser.find(".queryItemItemName a", 1).getText === "item 01"

        browser.find("#sortDropDown option[value='priceReverse']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.webDriver.findElement(By.id("sortDropDown"))
          .findElement(By.cssSelector("option[value=\"priceReverse\"]")).isSelected === true

        browser.find(".queryItemItemName a", 0).getText === "item 01"
        browser.find(".queryItemItemName a", 1).getText === "item 02"
      }}
    }
  }
}
