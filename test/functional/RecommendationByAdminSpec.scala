package functional

import org.openqa.selenium.By
import java.util.concurrent.TimeUnit
import play.api.test.Helpers._
import play.api.i18n.{Lang, Messages}
import play.api.db.DB
import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import java.sql.Connection
import models._
import helpers.Helper._
import LocaleInfo._
import java.sql.Date.{valueOf => date}
import play.api.Play.current
import com.ruimo.scoins.Scoping._

class RecommendationByAdminSpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  "recommendation by admin maintenance" should {
    "Can create record" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val adminUser = loginWithTestUser(browser)
        val tax = Tax.createNew
        val sites = Vector(Site.createNew(Ja, "商店1"), Site.createNew(Ja, "商店2"))
        val cat1 = Category.createNew(
          Map(Ja -> "植木", En -> "Plant")
        )
        val items = Vector(Item.createNew(cat1), Item.createNew(cat1), Item.createNew(cat1))
        SiteItem.createNew(sites(0), items(0))
        SiteItem.createNew(sites(1), items(1))
        SiteItem.createNew(sites(0), items(2))

        SiteItemNumericMetadata.createNew(sites(0).id.get, items(2).id.get, SiteItemNumericMetadataType.HIDE, 1)
        val itemNames = Vector(
          ItemName.createNew(items(0), Map(Ja -> "植木1")),
          ItemName.createNew(items(1), Map(Ja -> "植木2")),
          ItemName.createNew(items(2), Map(Ja -> "植木3"))
        )
        val itemPrice1 = ItemPrice.createNew(items(0), sites(0))
        val itemPrice2 = ItemPrice.createNew(items(1), sites(1))
        val itemPrice3 = ItemPrice.createNew(items(2), sites(0))
        val itemPriceHistories = Vector(
          ItemPriceHistory.createNew(
            itemPrice1, tax, CurrencyInfo.Jpy, BigDecimal("100"), None, BigDecimal("90"), date("9999-12-31")
          ),
          ItemPriceHistory.createNew(
            itemPrice2, tax, CurrencyInfo.Jpy, BigDecimal("200"), None, BigDecimal("190"), date("9999-12-31")
          ),
          ItemPriceHistory.createNew(
            itemPrice3, tax, CurrencyInfo.Jpy, BigDecimal("300"), None, BigDecimal("290"), date("9999-12-31")
          )
        )

        browser.goTo(
          "http://localhost:3333" + controllers.routes.RecommendationByAdmin.selectItem(List()) + "?lang=" + lang.code
        )

        // Hidden item should be shown in maintenance functions.
        browser.find(".itemTableBody").size === 3

        // Sorted by item name.
        browser.find(".itemTableItemId", 0).getText === items(0).id.get.toString
        browser.find(".itemTableItemId", 1).getText === items(1).id.get.toString
        browser.find(".itemTableItemId", 2).getText === items(2).id.get.toString

        browser.find(".itemName", 0).getText === "植木1"
        browser.find(".itemName", 1).getText === "植木2"
        browser.find(".itemName", 2).getText === "植木3"

        browser.find(".itemTableSiteName", 0).getText === "商店1"
        browser.find(".itemTableSiteName", 1).getText === "商店2"
        browser.find(".itemTableSiteName", 2).getText === "商店1"

        browser.find(".itemTablePrice", 0).getText === "100円"
        browser.find(".itemTablePrice", 1).getText === "200円"
        browser.find(".itemTablePrice", 2).getText === "300円"

        DB.withConnection { implicit conn =>
          RecommendByAdmin.listByScore(showDisabled = true, locale = LocaleInfo.Ja).records.size === 0
        }

        browser.find(".addRecommendationByAdminForm input[type='submit']", 0).click
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".addRecommendationByAdminForm input[type='submit']", 1).click
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".addRecommendationByAdminForm input[type='submit']", 2).click
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        var recoId1: Long = 0
        var recoId2: Long = 0
        DB.withConnection { implicit conn =>
          // Hidden item should not be listed.
          doWith(RecommendByAdmin.listByScore(showDisabled = true, locale = LocaleInfo.Ja).records) { rec =>
            rec.size === 2
            rec(0)._1.siteId === sites(0).id.get
            rec(0)._1.itemId === items(0).id.get.id
            rec(0)._1.score === 1
            rec(0)._1.enabled === true

            rec(0)._2 === Some(itemNames(0)(LocaleInfo.Ja))
            rec(0)._3 === Some(sites(0))

            rec(1)._1.siteId === sites(1).id.get
            rec(1)._1.itemId === items(1).id.get.id
            rec(1)._1.score === 1
            rec(1)._1.enabled === true

            rec(1)._2 === Some(itemNames(1)(LocaleInfo.Ja))
            rec(1)._3 === Some(sites(1))
          }
        }

        browser.goTo(
          "http://localhost:3333" + controllers.routes.RecommendationByAdmin.startUpdate() + "?lang=" + lang.code
        )

        browser.find(".recommendationByAdminTable.body").size === 2
        recoId1 = browser.find(".idInput", 0).getAttribute("value").toLong
        recoId2 = browser.find(".idInput", 1).getAttribute("value").toLong

        // Check validation
        browser.find(".scoreInput", 0).text("")
        browser.find(".updateButton", 0).click
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".recommendationByAdminTable.body")
          .find(".error").getText === Messages("error.number")

        browser.find(".scoreInput", 0).text("ABC")
        browser.find(".updateButton", 0).click
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".recommendationByAdminTable.body")
          .find(".error").getText === Messages("error.number")

        browser.find(".scoreInput", 0).text("-1")
        browser.find(".updateButton", 0).click
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".recommendationByAdminTable.body")
          .find(".error").getText === Messages("error.min", 0)

        browser.find(".scoreInput", 0).text("123")
        browser.find(".recommendationByAdminTable.body", 0).find("input[type='checkbox']").click
        browser.find(".updateButton", 0).click
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        DB.withConnection { implicit conn =>
          doWith(RecommendByAdmin(recoId1)) { rec =>
            rec.score === 123
            rec.enabled === false
          }
        }

        browser.find(".scoreInput", 0).getAttribute("value") === "123"
        browser.webDriver.findElements(
          By.cssSelector(".recommendationByAdminTable.body input[type='checkbox']")
        ).get(0).isSelected === false

        browser.find(".removeRecommendationByAdminForm", 0).find("input[type='submit']").click
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".removeRecommendationByAdminForm").size === 1
        browser.find(".idInput").getAttribute("value").toLong === recoId2

        DB.withConnection { implicit conn =>
          doWith(RecommendByAdmin.listByScore(showDisabled = true, locale = LocaleInfo.Ja).records) { rec =>
            rec.size === 1

            rec(0)._1.siteId === sites(1).id.get
            rec(0)._1.itemId === items(1).id.get.id
            rec(0)._1.score === 1
            rec(0)._1.enabled === true

            rec(0)._2 === Some(itemNames(1)(LocaleInfo.Ja))
            rec(0)._3 === Some(sites(1))
          }          
        }
      }}      
    }
  }
}
