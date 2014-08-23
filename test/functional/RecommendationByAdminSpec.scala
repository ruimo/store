package functional

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
            itemPrice1, tax, CurrencyInfo.Jpy, BigDecimal("100"), BigDecimal("90"), date("9999-12-31")
          ),
          ItemPriceHistory.createNew(
            itemPrice2, tax, CurrencyInfo.Jpy, BigDecimal("200"), BigDecimal("190"), date("9999-12-31")
          ),
          ItemPriceHistory.createNew(
            itemPrice3, tax, CurrencyInfo.Jpy, BigDecimal("300"), BigDecimal("290"), date("9999-12-31")
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
      }}      
    }
  }
}
