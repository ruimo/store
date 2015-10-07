package functional

import helpers.UrlHelper
import helpers.UrlHelper._
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

class ItemMaintenanceSpec extends Specification {
  "Item maintenance" should {
    "Create new item." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit def date2milli(d: java.sql.Date) = d.getTime
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site = Site.createNew(LocaleInfo.Ja, "Store01")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "外税")
        val taxHis = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.startCreateNewItem().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#siteId").find("option").getText() === "Store01"
        browser.find("#categoryId").find("option").getText() === "Cat01"
        browser.find("#taxId").find("option").getText() === "外税"
        browser.fill("#description").`with`("Description01")

        browser.fill("#itemName").`with`("ItemName01")
        browser.fill("#price").`with`("1234")
        browser.fill("#costPrice").`with`("2345")
        browser.find("#createNewItemForm").find("input[type='submit']").click

        browser.find(".message").getText() === Messages("itemIsCreated")

        val itemList = Item.list(None, LocaleInfo.Ja, QueryString()).records

        itemList.size === 1
        doWith(itemList.head) { item =>
          item._2.name === "ItemName01"
          item._3.description === "Description01"
          item._4.name === "Store01"
          item._5.unitPrice === BigDecimal("1234")
          item._5.costPrice === BigDecimal("2345")
          item._5.listPrice === None
          Coupon.isCoupon(item._1.id.get) === false
        }
      }}
    }

    "Create new item and set list price." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit def date2milli(d: java.sql.Date) = d.getTime
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site = Site.createNew(LocaleInfo.Ja, "Store01")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "外税")
        val taxHis = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.startCreateNewItem().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#siteId").find("option").getText() === "Store01"
        browser.find("#categoryId").find("option").getText() === "Cat01"
        browser.find("#taxId").find("option").getText() === "外税"
        browser.fill("#description").`with`("Description01")

        browser.fill("#itemName").`with`("ItemName01")
        browser.fill("#price").`with`("1234")
        browser.fill("#costPrice").`with`("2345")
        browser.find("#createNewItemForm").find("input[type='submit']").click
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".message").getText() === Messages("itemIsCreated")

        val itemList = Item.list(None, LocaleInfo.Ja, QueryString()).records
        itemList.size === 1

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.startChangeItem(itemList.head._1.id.get.id).url + "&lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#itemNames_0_itemName").getAttribute("value") === "ItemName01"
        browser.find("#categoryId option").getText === "Cat01"
        browser.find("#itemPrices_0_taxId option").getText === "外税"
        browser.find("#itemPrices_0_itemPrice").getAttribute("value") === "1234.00"
        browser.find("#itemPrices_0_listPrice").getAttribute("value") === ""
        browser.find("#itemPrices_0_costPrice").getAttribute("value") === "2345.00"
        browser.find("#itemPrices_0_validUntil").getAttribute("value") === "9999-12-31 23:59:59"

        browser.fill("#itemPrices_0_listPrice").`with`("3000")
        browser.find("#changeItemPriceButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#itemPrices_0_listPrice").getAttribute("value") === "3000.00"

        browser.fill("#itemPrices_0_listPrice").`with`("")
        browser.find("#changeItemPriceButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#itemPrices_0_listPrice").getAttribute("value") === ""
      }}
    }

    "Create new item with list price." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit def date2milli(d: java.sql.Date) = d.getTime
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site = Site.createNew(LocaleInfo.Ja, "Store01")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "外税")
        val taxHis = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.startCreateNewItem().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#siteId").find("option").getText() === "Store01"
        browser.find("#categoryId").find("option").getText() === "Cat01"
        browser.find("#taxId").find("option").getText() === "外税"
        browser.fill("#description").`with`("Description01")

        browser.fill("#itemName").`with`("ItemName01")
        browser.fill("#price").`with`("1234")
        browser.fill("#listPrice").`with`("3000")
        browser.fill("#costPrice").`with`("2345")
        browser.find("#createNewItemForm").find("input[type='submit']").click

        browser.find(".message").getText() === Messages("itemIsCreated")

        val itemList = Item.list(None, LocaleInfo.Ja, QueryString()).records

        itemList.size === 1
        doWith(itemList.head) { item =>
          item._2.name === "ItemName01"
          item._3.description === "Description01"
          item._4.name === "Store01"
          item._5.unitPrice === BigDecimal("1234")
          item._5.listPrice === Some(BigDecimal("3000"))
          item._5.costPrice === BigDecimal("2345")
          Coupon.isCoupon(item._1.id.get) === false
        }
      }}
    }

    "Create new item with price memo." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit def date2milli(d: java.sql.Date) = d.getTime
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site = Site.createNew(LocaleInfo.Ja, "Store01")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "外税")
        val taxHis = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.startCreateNewItem().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#siteId").find("option").getText() === "Store01"
        browser.find("#categoryId").find("option").getText() === "Cat01"
        browser.find("#taxId").find("option").getText() === "外税"
        browser.fill("#description").`with`("Description01")

        browser.fill("#itemName").`with`("ItemName01")
        browser.fill("#price").`with`("1234")
        browser.fill("#costPrice").`with`("2345")
        browser.find("#createNewItemForm").find("input[type='submit']").click
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".message").getText() === Messages("itemIsCreated")

        val itemList = Item.list(None, LocaleInfo.Ja, QueryString()).records
        itemList.size === 1

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.startChangeItem(itemList.head._1.id.get.id).url + "&lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#itemNames_0_itemName").getAttribute("value") === "ItemName01"
        browser.find("#categoryId option").getText === "Cat01"
        browser.find("#itemPrices_0_taxId option").getText === "外税"
        browser.find("#itemPrices_0_itemPrice").getAttribute("value") === "1234.00"
        browser.find("#itemPrices_0_listPrice").getAttribute("value") === ""
        browser.find("#itemPrices_0_costPrice").getAttribute("value") === "2345.00"
        browser.find("#itemPrices_0_validUntil").getAttribute("value") === "9999-12-31 23:59:59"

        doWith(browser.find("#addSiteItemTextMetadataForm")) { form =>
          form.find(
            "option[value=\"" + SiteItemTextMetadataType.PRICE_MEMO.ordinal + "\"]"
          ).click()

          browser.fill("#addSiteItemTextMetadataForm #metadata").`with`("Price memo")

          form.find("input[type='submit']").click()
        }
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find("#siteItemTextMetadatas_0_metadata").getAttribute("value") === "Price memo"

        browser.find(".removeSiteItemTextMetadataButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".removeSiteItemTextMetadataButton").getTexts.size === 0
      }}
    }

    "Create new coupon item." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit def date2milli(d: java.sql.Date) = d.getTime
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site = Site.createNew(LocaleInfo.Ja, "Store01")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "外税")
        val taxHis = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.startCreateNewItem().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#isCoupon").click()
        browser.find("#siteId").find("option").getText() === "Store01"
        browser.find("#categoryId").find("option").getText() === "Cat01"
        browser.find("#taxId").find("option").getText() === "外税"
        browser.fill("#description").`with`("Description01")

        browser.fill("#itemName").`with`("ItemName01")
        browser.fill("#price").`with`("1234")
        browser.fill("#costPrice").`with`("2345")
        browser.find("#createNewItemForm").find("input[type='submit']").click

        browser.find(".message").getText() === Messages("itemIsCreated")

        val itemList = Item.list(None, LocaleInfo.Ja, QueryString()).records

        itemList.size === 1
        doWith(itemList.head) { item =>
          item._2.name === "ItemName01"
          item._3.description === "Description01"
          item._4.name === "Store01"
          item._5.unitPrice === BigDecimal("1234")
          item._5.costPrice === BigDecimal("2345")
          Coupon.isCoupon(item._1.id.get) === true
        }
      }}
    }

    "Can edit item that has no handling stores." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit def date2milli(d: java.sql.Date) = d.getTime
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site = Site.createNew(LocaleInfo.Ja, "Store01")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "外税")
        val taxHis = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.startCreateNewItem().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#siteId").find("option").getText() === "Store01"
        browser.find("#categoryId").find("option").getText() === "Cat01"
        browser.find("#taxId").find("option").getText() === "外税"
        browser.fill("#description").`with`("Description01")

        browser.fill("#itemName").`with`("ItemName01")
        browser.fill("#price").`with`("1234")
        browser.fill("#costPrice").`with`("2345")
        browser.find("#createNewItemForm").find("input[type='submit']").click

        browser.find(".message").getText() === Messages("itemIsCreated")

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.editItem(List("")).url + "&lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        doWith(browser.find(".itemTableBody")) { tr =>
          tr.find(".itemTableItemName").getText === "ItemName01"
          tr.find(".itemTableSiteName").getText === "Store01"
          tr.find(".itemTablePrice").getText === ViewHelpers.toAmount(BigDecimal(1234))
        }

        val itemId = browser.find(".itemTableBody .itemTableItemId a").getText.toLong

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.startChangeItem(itemId).url + "&lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        // Now handling site becomes zero.
        browser.find(".deleteHandlingSiteButton").click()

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.editItem(List("")).url + "&lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        doWith(browser.find(".itemTableBody")) { tr =>
          tr.find(".itemTableItemName").getText === "ItemName01"
          tr.find(".itemTableSiteName").getText === "-"
          tr.find(".itemTablePrice").getText === "-"
        }
      }}
    }

    def createNormalUser(userName: String = "user"): StoreUser = DB.withConnection { implicit conn =>
      StoreUser.create(
        userName, "Admin", None, "Manager", "admin@abc.com",
        4151208325021896473L, -1106301469931443100L, UserRole.NORMAL, Some("Company1")
      )
    }

    def login(browser: TestBrowser, userName: String = "administrator") {
      browser.goTo("http://localhost:3333" + controllers.routes.Admin.index.url)
      browser.fill("#userName").`with`(userName)
      browser.fill("#password").`with`("password")
      browser.click("#doLoginButton")
    }

    "Store owner cannot edit item name." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit def date2milli(d: java.sql.Date) = d.getTime
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site = Site.createNew(LocaleInfo.Ja, "Store01")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "外税")
        val taxHis = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

        val user01 = createNormalUser("user01")
        val siteOwner = SiteUser.createNew(user01.id.get, site.id.get)

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.startCreateNewItem().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find("#siteId").find("option").getText() === "Store01"
        browser.find("#categoryId").find("option").getText() === "Cat01"
        browser.find("#taxId").find("option").getText() === "外税"
        browser.fill("#description").`with`("Description01")

        browser.fill("#itemName").`with`("ItemName01")
        browser.fill("#price").`with`("1234")
        browser.fill("#costPrice").`with`("2345")
        browser.find("#createNewItemForm").find("input[type='submit']").click

        browser.find(".message").getText() === Messages("itemIsCreated")

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.editItem(List("")).url + "&lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        doWith(browser.find(".itemTableBody")) { tr =>
          tr.find(".itemTableItemName").getText === "ItemName01"
          tr.find(".itemTableSiteName").getText === "Store01"
          tr.find(".itemTablePrice").getText === ViewHelpers.toAmount(BigDecimal(1234))
        }

        val itemId = ItemId(browser.find(".itemTableBody .itemTableItemId a").getText.toLong)

        ItemName.add(itemId, LocaleInfo.En.id, "ItemName01-EN")

        logoff(browser)
        login(browser, "user01")
        // Store owner cannot remove item.
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.removeItemName(itemId.id, LocaleInfo.Ja.id).url + "&lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        ItemName.list(itemId).size === 2
        browser.title === Messages("commonTitle") + " " + Messages("company.name")
      }}
    }

    "Supplemental category maintenance." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit def date2milli(d: java.sql.Date) = d.getTime
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site01 = Site.createNew(LocaleInfo.Ja, "Store01")
        val cat01 = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
        val cat02 = Category.createNew(Map(LocaleInfo.Ja -> "Cat02"))
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "外税")
        val taxHis = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.startCreateNewItem().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#siteId").find("option", 0).getText() === "Store01"
        browser.find("#siteId option[value='" + site01.id.get + "']").click()
        browser.find("#categoryId").find("option").getText() === "Cat01"
        browser.find("#taxId").find("option").getText() === "外税"
        browser.fill("#description").`with`("Description01")

        browser.fill("#itemName").`with`("ItemName01")
        browser.fill("#price").`with`("1234")
        browser.fill("#costPrice").`with`("2345")
        browser.find("#createNewItemForm").find("input[type='submit']").click

        browser.find(".message").getText() === Messages("itemIsCreated")

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.startChangeItem(1000).url.addParm("lang", lang.code)
        )

        SupplementalCategory.byItem(ItemId(1000)).size === 0
        browser.find("#removeSupplementalCategoriesTable .removeSupplementalCategoryRow").getTexts.size === 0

        browser.find("#addSupplementalCategoryForm option[value='1000']").getTexts.size === 1
        browser.find("#addSupplementalCategoryForm option[value='1001']").getTexts.size === 1

        browser.find("#addSupplementalCategoryForm option[value='1001']").click()
        browser.find("#addSupplementalCategoryForm input[type='submit']").click()

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".message").getText === Messages("itemIsUpdated")
        browser.find("#removeSupplementalCategoriesTable .removeSupplementalCategoryRow").getTexts.size === 1
        SupplementalCategory.byItem(ItemId(1000)).size === 1
        browser.find("#removeSupplementalCategoriesTable .categoryName").getText === "Cat02"
        browser.find("#removeSupplementalCategoriesTable .removeForm input[type='submit']").click()

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find(".message").getText === Messages("itemIsUpdated")
        browser.find("#removeSupplementalCategoriesTable .removeSupplementalCategoryRow").getTexts.size === 0
        SupplementalCategory.byItem(ItemId(1000)).size === 0
      }}
    }

    "Creating an item that is treated by two sites." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit def date2milli(d: java.sql.Date) = d.getTime
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site01 = Site.createNew(LocaleInfo.Ja, "Store01")
        val site02 = Site.createNew(LocaleInfo.Ja, "Store02")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "外税")
        val taxHis = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.startCreateNewItem().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#siteId").find("option", 0).getText() === "Store01"
        browser.find("#siteId option[value='" + site01.id.get + "']").click()
        browser.find("#categoryId").find("option").getText() === "Cat01"
        browser.find("#taxId").find("option").getText() === "外税"
        browser.fill("#description").`with`("Description01")

        browser.fill("#itemName").`with`("ItemName01")
        browser.fill("#price").`with`("1234")
        browser.fill("#costPrice").`with`("2345")
        browser.find("#createNewItemForm").find("input[type='submit']").click

        browser.find(".message").getText() === Messages("itemIsCreated")

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.startChangeItem(1000).url.addParm("lang", lang.code)
        )

        1===1
      }}
    }
  }
}
