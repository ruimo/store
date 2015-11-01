package functional

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

  "Item query advanced" should {
    "All items should be shown by default" in {
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
            qs = List(), cs = "", ccs = "", sid = None, page = 0, pageSize = 10
          ).url + "?lang=" + lang.code
        )

        browser.await().atMost(5, TimeUnit.SECONDS).until(".qthumItem_name").isPresent
        0 to 4 foreach { i =>
          browser.find(".qthumItem", i).find(".qthumItem_name").getText === "item" + i
        }

        browser.find("#orderBySelect option[value='item.item_id DESC']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        def itemNameMatcher(idx: Int, expectedItemName: String): ExpectedCondition[Boolean] = new ExpectedCondition[Boolean] {
          def apply(d: WebDriver): Boolean =
            d.findElements(By.cssSelector("#queryBody .qthumItem_name"))
              .get(idx).findElement(By.cssSelector("a")).getText.trim().equals(expectedItemName)
        }

        0 to 4 foreach { i =>
          new WebDriverWait(browser.webDriver, 5).until(itemNameMatcher(i, "item" + (4 - i)))
        }

        browser.find("#orderBySelect option[value='item_price_history.unit_price ASC']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.await().atMost(5, TimeUnit.SECONDS).until("#queryBody .qthumItem").isPresent
        0 to 4 foreach { i =>
          new WebDriverWait(browser.webDriver, 5).until(itemNameMatcher(i, "item" + i))
        }

        1 === 1
      }}
    }
  }
}

