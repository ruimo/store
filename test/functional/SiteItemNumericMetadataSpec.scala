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

class SiteItemNumericMetadataSpec extends Specification {
  "Site item numeric metadata" should {
    "Can create records." in {
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
        val item = Item.createNew(cat)
        val siteItem = SiteItem.createNew(site, item)
        val itemName = ItemName.createNew(item, Map(LocaleInfo.Ja -> "かえで"))
        val itemDesc = ItemDescription.createNew(item, site, "かえで説明")
        val itemPrice = ItemPrice.createNew(item, site)
        val itemPriceHistory = ItemPriceHistory.createNew(
          itemPrice, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
        )

        // Item should be shown.
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemQuery.query(List("かえで")).url.addParm("lang", lang.code)
        )
        browser.find("div.itemNotFound").size === 0

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.startChangeItem(item.id.get.id).url.addParm("lang", lang.code)
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        // Hide
        browser.find("#addSiteItemMetadataForm select[name='metadataType'] option[value='" + SiteItemNumericMetadataType.HIDE.ordinal + "']").click()
        browser.fill("#addSiteItemMetadataForm input[name='metadata']").`with`("1")
        browser.find("#addSiteItemMetadataForm input[type='submit']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#siteItemMetadatas_0_siteId").getAttribute("value") === site.id.get.toString
        browser.find("#siteItemMetadatas_0_metadataType").getAttribute("value") === SiteItemNumericMetadataType.HIDE.ordinal.toString
        browser.find("#siteItemMetadatas_0_metadata").getAttribute("value") === "1"
        browser.find("#siteItemMetadatas_0_validUntil").getAttribute("value") === "9999-12-31 23:59:59"

        // Same date should be rejected.
        browser.find("#addSiteItemMetadataForm select[name='metadataType'] option[value='" + SiteItemNumericMetadataType.HIDE.ordinal + "']").click()
        browser.fill("#addSiteItemMetadataForm input[name='metadata']").`with`("0")
        browser.find("#addSiteItemMetadataForm input[type='submit']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find("#addSiteItemMetadataForm td.metadataType div.error div.error").getText === Messages("unique.constraint.violation")
        browser.find("#addSiteItemMetadataForm td.validUntil div.error div.error").getText === Messages("unique.constraint.violation")
        // Header + body + change Button
        browser.find("#changeSiteItemMetadataForm tr").size === 3

        // validUntils are different
        browser.find("#addSiteItemMetadataForm select[name='metadataType'] option[value='" + SiteItemNumericMetadataType.HIDE.ordinal + "']").click()
        browser.fill("#addSiteItemMetadataForm input[name='metadata']").`with`("0")
        browser.fill("#addSiteItemMetadataForm input[name='validUntil']").`with`("2000-12-31 00:00:00")

        browser.find("#addSiteItemMetadataForm input[type='submit']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find("div.message").getText === Messages("itemIsUpdated")
        browser.find("#siteItemMetadatas_0_metadataType").getAttribute("value") === SiteItemNumericMetadataType.HIDE.ordinal.toString
        browser.find("#siteItemMetadatas_0_metadata").getAttribute("value") === "0"
        browser.find("#siteItemMetadatas_0_validUntil").getAttribute("value") === "2000-12-31 00:00:00"

        browser.find("#siteItemMetadatas_1_metadataType").getAttribute("value") === SiteItemNumericMetadataType.HIDE.ordinal.toString
        browser.find("#siteItemMetadatas_1_metadata").getAttribute("value") === "1"
        browser.find("#siteItemMetadatas_1_validUntil").getAttribute("value") === "9999-12-31 23:59:59"
        
        // Item should be hidden.
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemQuery.query(List("かえで")).url.addParm("lang", lang.code)
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find("div.itemNotFound").size === 1

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.startChangeItem(item.id.get.id).url.addParm("lang", lang.code)
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        val now = java.lang.Long.valueOf(System.currentTimeMillis + 1000 * 60 * 10)

        browser.fill("#siteItemMetadatas_0_validUntil").`with`(String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS", now))
        browser.find(".updateSiteItemNumericMetadata").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        // Item should be shown.
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemQuery.query(List()).url.addParm("lang", lang.code)
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find("div.itemNotFound").size === 0

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.startChangeItem(item.id.get.id).url.addParm("lang", lang.code)
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find("#changeSiteItemMetadataForm tr", 1).find("button").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#changeSiteItemMetadataForm tr").size === 3
        browser.find("#siteItemMetadatas_0_siteId").getAttribute("value") === site.id.get.toString
        browser.find("#siteItemMetadatas_0_metadataType").getAttribute("value") === SiteItemNumericMetadataType.HIDE.ordinal.toString
        browser.find("#siteItemMetadatas_0_metadata").getAttribute("value") === "1"
        browser.find("#siteItemMetadatas_0_validUntil").getAttribute("value") === "9999-12-31 23:59:59"

        // Item should be hidden.
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemQuery.query(List()).url.addParm("lang", lang.code)
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find("div.itemNotFound").size === 1
      }}
    }
  }
}
