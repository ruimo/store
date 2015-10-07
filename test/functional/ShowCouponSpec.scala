package functional

import java.util.concurrent.TimeUnit
import helpers.Helper.disableMailer
import play.api.test.Helpers._
import play.api.Play.current
import helpers.Helper._
import models._
import org.joda.time.format.DateTimeFormat
import play.api.db.DB
import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.i18n.{Lang, Messages}
import java.sql.Date.{valueOf => date}
import LocaleInfo._
import java.sql.Connection
import com.ruimo.scoins.Scoping._

class ShowCouponSpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime
  val conf = inMemoryDatabase() ++ disableMailer

  "ShowCoupon" should {
    "Can show coupon." in {
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site = Site.createNew(LocaleInfo.Ja, "商店111")
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
        SiteItemNumericMetadata.add(item.id.get, site.id.get, SiteItemNumericMetadataType.INSTANT_COUPON, 1)

        browser.goTo(
          "http://localhost:3333" + controllers.routes.CouponHistory.showInstantCoupon(site.id.get, item.id.get.id).url + "&lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("commonTitle") + " " + Messages("coupon.title")
        browser.find("td.siteName").getText.indexOf(user.companyName.get) !== -1
        browser.find("td.name").getText.indexOf(user.fullName) !== -1
        browser.find("td.tranId").getTexts.size === 0
      }}
    }

    "If thw item is not flaged as instant coupon, page should redirect to top." in {
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site = Site.createNew(LocaleInfo.Ja, "商店111")
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

        browser.goTo(
          "http://localhost:3333" + controllers.routes.CouponHistory.showInstantCoupon(site.id.get, item.id.get.id).url + "&lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("commonTitle") + " " + Messages("company.name")
      }}
    }
  }
}
