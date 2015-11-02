package functional

import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.support.ui.ExpectedCondition
import org.openqa.selenium.support.ui.WebDriverWait
import org.fluentlenium.core.Fluent
import com.google.common.base.{Function => Func}
import org.openqa.selenium.WebDriver
import org.openqa.selenium.By
import java.util.concurrent.TimeUnit
import org.specs2.mutable.Specification
import helpers.UrlHelper
import helpers.UrlHelper._
import play.api.test.Helpers._
import play.api.Play.current
import models._
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.i18n.{Lang, Messages}
import play.api.db.DB
import helpers.Helper._
import java.sql.Date.{valueOf => date}

class ItemQueryAdvancedSpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  def itemNameMatcher(idx: Int, expectedItemName: String): ExpectedCondition[Boolean] = new ExpectedCondition[Boolean] {
    def apply(d: WebDriver): Boolean = try {
      d.findElements(By.cssSelector("#queryBody .qthumItem_name"))
        .get(idx).findElement(By.cssSelector("a")).getText.trim().equals(expectedItemName)
    }
    catch {
      case e: StaleElementReferenceException => false
    }
  }

  "Item query advanced" should {
    "All items should be orderd." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val adminUser = loginWithTestUser(browser)

        val site = Site.createNew(LocaleInfo.Ja, "商店111")
        val cats = 0 to 4 map {i => Category.createNew(Map(LocaleInfo.Ja -> ("Cat" + i)))}
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "内税")
        val taxHis = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))
        val items = 0 to 4 map {i => Item.createNew(cats(i))}
        items.foreach {it => SiteItem.createNew(site, it)}
        val itemNames = 0 to 4 map {i => ItemName.createNew(items(i), Map(LocaleInfo.Ja -> ("item" + i)))}
        val itemDescs = 0 to 4 map {i => ItemDescription.createNew(items(i), site, "説明" + i)}
        val itemPrices = items.map {it => ItemPrice.createNew(it, site)}
        val itemPriceHistories = 0 to 4 map {i =>
          ItemPriceHistory.createNew(
            itemPrices(i), tax, CurrencyInfo.Jpy, BigDecimal(100 * i), None,
            BigDecimal(90 * i), date("9999-12-31")
          )
        }

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemQuery.queryAdvanced(
            qs = List(), cs = "", ccs = "", sid = None, page = 0, pageSize = 10,
            orderBySpec = "item.item_id", templateNo = 0
          ).url + "?lang=" + lang.code
        )

        browser.await().atMost(10, TimeUnit.SECONDS).until(".qthumItem_name").isPresent
        0 to 4 foreach { i =>
          browser.find("#queryBody .qthumItem", i).find(".qthumItem_name").getText === "item" + i
        }

        browser.find("#orderBySelect option[value='item.item_id DESC']").click()
        browser.await().atMost(10, TimeUnit.SECONDS).untilPage().isLoaded()

        0 to 4 foreach { i =>
          new WebDriverWait(browser.webDriver, 10).until(itemNameMatcher(i, "item" + (4 - i)))
        }

        browser.find("#orderBySelect option[value='item_price_history.unit_price ASC']").click()
        browser.await().atMost(10, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.await().atMost(10, TimeUnit.SECONDS).until("#queryBody .qthumItem").isPresent
        0 to 4 foreach { i =>
          new WebDriverWait(browser.webDriver, 10).until(itemNameMatcher(i, "item" + i))
        }

        1 === 1
      }}
    }

    "All items should be paged." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val adminUser = loginWithTestUser(browser)

        val site = Site.createNew(LocaleInfo.Ja, "商店111")
        val range = 0 to 10
        val cats = range map {i => Category.createNew(Map(LocaleInfo.Ja -> ("Cat" + i)))}
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "内税")
        val taxHis = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))
        val items = range map {i => Item.createNew(cats(i))}
        items.foreach {it => SiteItem.createNew(site, it)}
        val itemNames = range map {i => ItemName.createNew(items(i), Map(LocaleInfo.Ja -> ("item" + i)))}
        val itemDescs = range map {i => ItemDescription.createNew(items(i), site, "説明" + i)}
        val itemPrices = items.map {it => ItemPrice.createNew(it, site)}
        val itemPriceHistories = range map {i =>
          ItemPriceHistory.createNew(
            itemPrices(i), tax, CurrencyInfo.Jpy, BigDecimal(100 * i), None,
            BigDecimal(90 * i), date("9999-12-31")
          )
        }

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemQuery.queryAdvanced(
            qs = List(), cs = "", ccs = "", sid = None, page = 0, pageSize = 10,
            orderBySpec = "item.item_id", templateNo = 0
          ).url + "?lang=" + lang.code
        )

        browser.await().atMost(10, TimeUnit.SECONDS).until(".qthumItem_name").isPresent
        0 to 9 foreach { i =>
          browser.find("#queryBody .qthumItem", i).find(".qthumItem_name").getText === "item" + i
        }

        browser.await().atMost(10, TimeUnit.SECONDS).until(
          "#pagingPaneDestination button.prevPageButton[disabled='disabled']"
        ).isPresent
        browser.await().atMost(10, TimeUnit.SECONDS).until("#pagingPaneDestination .pageCount").hasText("1/2")
        browser.await().atMost(10, TimeUnit.SECONDS).until("#pagingPaneDestination button.nextPageButton").isPresent
        browser.find("#pagingPaneDestination .pageSizes", 0).find(".currentSize").getText === "10"
        browser.find("#pagingPaneDestination .pageSizes a", 0).getText === "25"
        browser.find("#pagingPaneDestination .pageSizes a", 1).getText === "50"
        browser.find("#pagingPaneDestination button.nextPageButton").getAttribute("disabled") === null

        browser.find("#pagingPaneDestination button.nextPageButton").click()
        browser.await().atMost(10, TimeUnit.SECONDS).until(
          "#pagingPaneDestination button.nextPageButton[disabled='disabled']"
        ).isPresent
        browser.await().atMost(10, TimeUnit.SECONDS).until("#pagingPaneDestination .pageCount").hasText("2/2")
        browser.await().atMost(10, TimeUnit.SECONDS).until("#pagingPaneDestination button.prevPageButton").isPresent
        browser.find("#pagingPaneDestination .pageSizes", 0).find(".currentSize").getText === "10"
        browser.find("#pagingPaneDestination .pageSizes a", 0).getText === "25"
        browser.find("#pagingPaneDestination .pageSizes a", 1).getText === "50"
        browser.find("#pagingPaneDestination button.prevPageButton").getAttribute("disabled") === null

        browser.find("#queryBody .qthumItem", 0).find(".qthumItem_name").getText === "item10"
        browser.find("#queryBody .qthumItem").size === 1

        browser.find("#pagingPaneDestination button.prevPageButton").click()
        browser.await().atMost(10, TimeUnit.SECONDS).until(
          "#pagingPaneDestination button.prevPageButton[disabled='disabled']"
        ).isPresent
        browser.await().atMost(10, TimeUnit.SECONDS).until("#pagingPaneDestination .pageCount").hasText("1/2")
        browser.await().atMost(10, TimeUnit.SECONDS).until("#pagingPaneDestination button.nextPageButton").isPresent
        browser.find("#pagingPaneDestination .pageSizes", 0).find(".currentSize").getText === "10"
        browser.find("#pagingPaneDestination .pageSizes a", 0).getText === "25"
        browser.find("#pagingPaneDestination .pageSizes a", 1).getText === "50"
        browser.find("#pagingPaneDestination button.nextPageButton").getAttribute("disabled") === null

        0 to 9 foreach { i =>
          browser.find("#queryBody .qthumItem", i).find(".qthumItem_name").getText === "item" + i
        }

        1 === 1
      }}
    }

    "All items should be queried by category." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val adminUser = loginWithTestUser(browser)

        val site = Site.createNew(LocaleInfo.Ja, "商店111")
        val range = 0 to 20
        val cats = range map {i => Category.createNew(Map(LocaleInfo.Ja -> ("Cat" + i)))}

        val oddCat = Category.createNew(Map(LocaleInfo.Ja -> ("OddCat")))
        Category.updateCategoryCode(oddCat.id.get, "10000000")
        val evenCat = Category.createNew(Map(LocaleInfo.Ja -> ("EvenCat")))
        Category.updateCategoryCode(evenCat.id.get, "10000001")

        val aboveFiveCat = Category.createNew(Map(LocaleInfo.Ja -> ("AboveTenCat")))
        Category.updateCategoryCode(aboveFiveCat.id.get, "2000000")

        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "内税")
        val taxHis = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))
        val items = range map {i => Item.createNew(cats(i))}
        items.foreach {it => SiteItem.createNew(site, it)}
        val itemNames = range map {i => ItemName.createNew(items(i), Map(LocaleInfo.Ja -> ("item" + i)))}
        val itemDescs = range map {i => ItemDescription.createNew(items(i), site, "説明" + i)}
        val itemPrices = items.map {it => ItemPrice.createNew(it, site)}
        val itemPriceHistories = range map {i =>
          ItemPriceHistory.createNew(
            itemPrices(i), tax, CurrencyInfo.Jpy, BigDecimal(100 * i), None,
            BigDecimal(90 * i), date("9999-12-31")
          )
        }
        range.filter(_ % 2 != 0).map { i =>
          SupplementalCategory.createNew(items(i).id.get, oddCat.id.get)
        }
        range.filter(_ > 5).map { i =>
          SupplementalCategory.createNew(items(i).id.get, aboveFiveCat.id.get)
        }
        
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemQuery.queryAdvanced(
            qs = List(), cs = "", ccs = "", sid = None, page = 0, pageSize = 10,
            orderBySpec = "item.item_id", templateNo = 0
          ).url + "?lang=" + lang.code
        )

        browser.await().atMost(10, TimeUnit.SECONDS).until(".qthumItem_name").isPresent
        0 to 9 foreach { i =>
          browser.find("#queryBody .qthumItem", i).find(".qthumItem_name").getText === "item" + i
        }

        browser.await().atMost(10, TimeUnit.SECONDS).until("#pagingPaneDestination .pageCount").hasText("1/3")

        browser.find("#categoryCondition .categoryConditionItem[data-category-code='10000000'] input").click()
        browser.await().atMost(10, TimeUnit.SECONDS).until("#pagingPaneDestination .pageCount").hasText("1/1")
        0 to 9 foreach { i =>
          browser.find("#queryBody .qthumItem", i).find(".qthumItem_name").getText === "item" + (i * 2 + 1)
        }

        browser.find(".category02").click()
        browser.await().atMost(10, TimeUnit.SECONDS).until(
          "#categoryCondition .categoryConditionItem[data-category-code='2000000'] input"
        ).areDisplayed

        browser.find("#categoryCondition .categoryConditionItem[data-category-code='2000000'] input").click()

        new WebDriverWait(browser.webDriver, 10).until(itemNameMatcher(0, "item7"))
        
        browser.find(".category01").click()
        browser.await().atMost(10, TimeUnit.SECONDS).until(
          "#categoryCondition .categoryConditionItem[data-category-code='10000000'] input"
        ).areDisplayed
        browser.find("#categoryCondition .categoryConditionItem[data-category-code='10000000'] input").click()
        browser.await().atMost(10, TimeUnit.SECONDS).until("#pagingPaneDestination .pageCount").hasText("1/2")

        browser.find("#queryBody .qthumItem").size === 10
        0 to 9 foreach { i =>
          browser.find("#queryBody .qthumItem", i).find(".qthumItem_name").getText === "item" + (i + 6)
        }
        browser.find("#pagingPaneDestination button.nextPageButton").click()

        new WebDriverWait(browser.webDriver, 10).until(itemNameMatcher(0, "item16"))
      }}
    }
  }
}

