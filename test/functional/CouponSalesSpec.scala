package functional

import org.joda.time.format.DateTimeFormat
import helpers.ViewHelpers
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
import com.ruimo.scoins.Scoping._

class CouponSalesSpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  "Coupon sale" should {
    "If the all of items are coupon, shipping address should be skipped." in {
      val app = FakeApplication(
        additionalConfiguration = inMemoryDatabase(options = Map("MVCC" -> "true"))
      )
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        import models.LocaleInfo.{Ja, En}
        implicit val lang = Lang("ja")

        val user = loginWithTestUser(browser)
        val tax = Tax.createNew
        val his = TaxHistory.createNew(tax, TaxType.OUTER_TAX, BigDecimal("8"), date("9999-12-31"))
        val site1 = Site.createNew(Ja, "商店1")
        val site2 = Site.createNew(Ja, "商店2")
        val cat1 = Category.createNew(Map(Ja -> "植木", En -> "Plant"))

        val item1 = Item.createNew(cat1)
        val item2 = Item.createNew(cat1)
        val item3 = Item.createNew(cat1)
        
        Coupon.updateAsCoupon(item1.id.get)
        Coupon.updateAsCoupon(item2.id.get)
        Coupon.updateAsCoupon(item3.id.get)

        val name1 = ItemName.createNew(item1, Map(Ja -> "クーポン1", En -> "Coupon1"))
        val name2 = ItemName.createNew(item2, Map(Ja -> "クーポン2", En -> "Coupon2"))
        val name3 = ItemName.createNew(item3, Map(Ja -> "クーポン3", En -> "Coupon3"))

        val desc1 = ItemDescription.createNew(item1, site1, "クーポン1説明")
        val desc2 = ItemDescription.createNew(item2, site2, "クーポン2説明")
        val desc3 = ItemDescription.createNew(item3, site2, "クーポン3説明")

        SiteItem.createNew(site1, item1)
        SiteItem.createNew(site2, item2)
        SiteItem.createNew(site2, item3)

        val price1 = ItemPrice.createNew(item1, site1)
        val price2 = ItemPrice.createNew(item2, site2)
        val price3 = ItemPrice.createNew(item3, site2)

        val ph1 = ItemPriceHistory.createNew(
          price1, tax, CurrencyInfo.Jpy, BigDecimal(101), None, BigDecimal(90), date("9999-12-31")
        )
        val ph2 = ItemPriceHistory.createNew(
          price2, tax, CurrencyInfo.Jpy, BigDecimal(301), None, BigDecimal(200), date("9999-12-31")
        )
        val ph3 = ItemPriceHistory.createNew(
          price3, tax, CurrencyInfo.Jpy, BigDecimal(401), None, BigDecimal(390), date("9999-12-31")
        )

        val cart1 = ShoppingCartItem.addItem(user.id.get, site1.id.get, item1.id.get.id, 15)
        val cart2 = ShoppingCartItem.addItem(user.id.get, site2.id.get, item2.id.get.id, 28)
        val cart3 = ShoppingCartItem.addItem(user.id.get, site2.id.get, item3.id.get.id, 40)

        browser.goTo(
          "http://localhost:3333" + controllers.routes.Shipping.startEnterShippingAddress().url + "?lang=" + lang.code
        )

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("commonTitle", Messages("confirm.shipping.address"))

        doWith(browser.find(".itemTableBody", 0)) { b =>
          b.find(".itemName").getText === "クーポン1"
          b.find(".siteName").getText === "商店1"
          b.find(".itemQuantity").getText === "15"
          b.find(".itemPrice").getText === ViewHelpers.toAmount(BigDecimal(101 * 15))
        }

        doWith(browser.find(".itemTableBody", 1)) { b =>
          b.find(".itemName").getText === "クーポン2"
          b.find(".siteName").getText === "商店2"
          b.find(".itemQuantity").getText === "28"
          b.find(".itemPrice").getText === ViewHelpers.toAmount(BigDecimal(301 * 28))
        }

        doWith(browser.find(".itemTableBody", 2)) { b =>
          b.find(".itemName").getText === "クーポン3"
          b.find(".siteName").getText === "商店2"
          b.find(".itemQuantity").getText === "40"
          b.find(".itemPrice").getText === ViewHelpers.toAmount(BigDecimal(401 * 40))
        }

        browser.find(".subtotal").getText === ViewHelpers.toAmount(
          BigDecimal(101 * 15 + 301 * 28 + 401 * 40)
        )

        val now = System.currentTimeMillis
        browser.find("#finalizeTransactionForm input").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        doWith(browser.find(".itemTableBody", 0)) { b =>
          b.find(".itemName .itemNameBody").getText === "クーポン1"
          b.find(".siteName").getText === "商店1"
          b.find(".quantity").getText === "15"
          b.find(".itemPrice").getText === ViewHelpers.toAmount(BigDecimal(1515))
        }

        doWith(browser.find(".itemTableBody", 1)) { b =>
          b.find(".itemName .itemNameBody").getText === "クーポン2"
          b.find(".siteName").getText === "商店2"
          b.find(".quantity").getText === "28"
          b.find(".itemPrice").getText === ViewHelpers.toAmount(BigDecimal(8428))
        }

        doWith(browser.find(".itemTableBody", 2)) { b =>
          b.find(".itemName .itemNameBody").getText === "クーポン3"
          b.find(".siteName").getText === "商店2"
          b.find(".quantity").getText === "40"
          b.find(".itemPrice").getText === ViewHelpers.toAmount(BigDecimal(16040))
        }

        browser.find(".itemTableBody", 2).find(".itemName input").click()

        val currentWindow = browser.webDriver.getWindowHandle
        val allWindows = browser.webDriver.getWindowHandles
        allWindows.remove(currentWindow)
        browser.webDriver.switchTo().window(allWindows.iterator.next)

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".date").find("span", 1).getText() === 
          DateTimeFormat.forPattern(Messages("published.date.format")).print(now)
        browser.find(".siteName").getText() === Messages("coupon.user.company.name", "Company1")
        browser.find(".name").getText() === justOneSpace(
          Messages("coupon.user.name", "Admin", "", "Manager")
        )
      }}
    }
  }
}
