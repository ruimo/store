package functional

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

class TaxCalculationSpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  "Tax calculation" should {
    "be performed for each store." in {
      // Since oute tax is calculated by each store. The tax will be 7 yen in the following case:
      //
      // Tax rate = 8% (outer tax)
      //
      //              Price Tax  Tax(rounded)
      // store1 item1 47    3.76 3
      // store2 item2 59    4.72 4
      // -------------------------
      //                         7

      // If tax is NOT calculated by each store. The tax will be 8 yen in the following case:
      // !!! This should not be the case !!!
      //
      // Tax rate = 8% (outer tax)
      //
      //              Price Tax  Tax(rounded)
      // store1 item1 47    3.76 3
      // store2 item2 59    4.72 4
      // -------------------------
      //              106   8.48 8

      // This spec ensures the tax is actually calculated to 7 yen but 8 yen.

      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        import models.LocaleInfo.{Ja, En}
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val address = createAddress
        val addressHistory = ShippingAddressHistory.createNew(
          user.id.get, address
        )
        val tax = Tax.createNew
        val his = TaxHistory.createNew(tax, TaxType.OUTER_TAX, BigDecimal("8"), date("9999-12-31"))

        val site1 = Site.createNew(Ja, "商店1")
        val site2 = Site.createNew(Ja, "商店2")

        val cat1 = Category.createNew(Map(Ja -> "植木", En -> "Plant"))

        val item1 = Item.createNew(cat1)
        val item2 = Item.createNew(cat1)

        val name1 = ItemName.createNew(item1, Map(Ja -> "杉", En -> "Cedar"))
        val name2 = ItemName.createNew(item2, Map(Ja -> "梅", En -> "Ume"))
        
        val desc1 = ItemDescription.createNew(item1, site1, "杉説明")
        val desc2 = ItemDescription.createNew(item2, site2, "梅説明")

        val metadata1 = SiteItemNumericMetadata.createNew(
          site1.id.get, item1.id.get, SiteItemNumericMetadataType.SHIPPING_SIZE, 1
        )
        val metadata2 = SiteItemNumericMetadata.createNew(
          site2.id.get, item2.id.get, SiteItemNumericMetadataType.SHIPPING_SIZE, 1
        )

        val box1_1 = ShippingBox.createNew(
          site1.id.get, 1, 7, "商店1の箱1"
        )
        val box2_1 = ShippingBox.createNew(
          site2.id.get, 1, 3, "商店2の箱1"
        )

        val fee1 = ShippingFee.createNew(box1_1.id.get, CountryCode.JPN, JapanPrefecture.三重県.code)
        val fee2 = ShippingFee.createNew(box2_1.id.get, CountryCode.JPN, JapanPrefecture.三重県.code)

        val feeHis1 = ShippingFeeHistory.createNew(fee1.id.get, tax.id.get, BigDecimal(2345), None, date("9999-12-31"))
        val feeHis2 = ShippingFeeHistory.createNew(fee2.id.get, tax.id.get, BigDecimal(3333), None, date("9999-12-31"))

        SiteItem.createNew(site1, item1)
        SiteItem.createNew(site2, item2)

        val price1 = ItemPrice.createNew(item1, site1)
        val price2 = ItemPrice.createNew(item2, site2)

        val ph1 = ItemPriceHistory.createNew(price1, tax, CurrencyInfo.Jpy, BigDecimal(47), None, BigDecimal(0), date("9999-12-31"))
        val ph2 = ItemPriceHistory.createNew(price2, tax, CurrencyInfo.Jpy, BigDecimal(59), None, BigDecimal(0), date("9999-12-31"))

        val cart1 = ShoppingCartItem.addItem(user.id.get, site1.id.get, item1.id.get.id, 1)
        val cart2 = ShoppingCartItem.addItem(user.id.get, site2.id.get, item2.id.get.id, 1)

        val cartShipping1 = ShoppingCartShipping.updateOrInsert(user.id.get, site1.id.get, date("2013-12-01"))
        val cartShipping2 = ShoppingCartShipping.updateOrInsert(user.id.get, site2.id.get, date("2013-12-02"))

        browser.goTo(
          "http://localhost:3333" + controllers.routes.Shipping.confirmShippingAddressJa().url + "?lang=" + lang.code
        )
        browser.title === Messages("confirm.shipping.address")

        browser.find("table.itemTable").find("tr.itemTableBody").size === 5
        browser.find("table.itemTable")
          .find("tr.itemTableBody", 0)
          .find("td.itemName")
          .getText === "杉"

        browser.find("table.itemTable")
          .find("tr.itemTableBody", 0)
          .find("td.itemQuantity")
          .getText === "1"

        browser.find("table.itemTable")
          .find("tr.itemTableBody", 0)
          .find("td.itemPrice")
          .getText === String.format("%1$,d円", Integer.valueOf(47))

        browser.find("table.itemTable")
          .find("tr.itemTableBody", 0)
          .find("td.siteName")
          .getText === "商店1"

        browser.find("table.itemTable")
          .find("tr.itemTableBody", 0)
          .find("td.itemSize")
          .getText === Messages("item.size.1")

        browser.find("table.itemTable")
          .find("tr.itemTableBody", 1)
          .find("td.itemName")
          .getText === "梅"

        browser.find("table.itemTable")
          .find("tr.itemTableBody", 1)
          .find("td.itemQuantity")
          .getText === "1"

        browser.find("table.itemTable")
          .find("tr.itemTableBody", 1)
          .find("td.itemPrice")
          .getText === String.format("%1$,d円", Integer.valueOf(59))

        browser.find("table.itemTable")
          .find("tr.itemTableBody", 1)
          .find("td.siteName")
          .getText === "商店2"

        browser.find("table.itemTable")
          .find("tr.itemTableBody", 1)
          .find("td.itemSize")
          .getText === Messages("item.size.1")

        // 送料
        browser.find("h2.shippingSiteName").size === 2
        browser.find("h2.shippingSiteName", 0)
          .getText === "商店1"
        browser.find("h2.shippingSiteName", 1)
          .getText === "商店2"
        browser.find("h3.shippingDate", 0)
          .getText === "配送希望日: 2013年12月01日"
        browser.find("h3.shippingDate", 1)
          .getText === "配送希望日: 2013年12月02日"

        browser.find("table.shipping")
          .find("tr.shippingTableBody", 0)
          .find("td.boxName")
          .getText === "商店1の箱1"

        browser.find("table.shipping")
          .find("tr.shippingTableBody", 0)
          .find("td.boxUnitPrice")
          .getText === String.format("%1$,d円", Integer.valueOf(2345))

        browser.find("table.shipping")
          .find("tr.shippingTableBody", 0)
          .find("td.boxQuantity")
          .getText === "1 箱"

        browser.find("table.shipping")
          .find("tr.shippingTableBody", 0)
          .find("td.boxPrice")
          .getText === String.format("%1$,d円", Integer.valueOf(2345))

        browser.find("table.shipping", 1)
          .find("tr.shippingTableBody", 0)
          .find("td.boxName")
          .getText === "商店2の箱1"

        browser.find("table.shipping", 1)
          .find("tr.shippingTableBody", 0)
          .find("td.boxUnitPrice")
          .getText === String.format("%1$,d円", Integer.valueOf(3333))

        browser.find("table.shipping", 1)
          .find("tr.shippingTableBody", 0)
          .find("td.boxQuantity")
          .getText === "1 箱"

        browser.find("table.shipping", 1)
          .find("tr.shippingTableBody", 0)
          .find("td.boxPrice")
          .getText === String.format("%1$,d円", Integer.valueOf(3333))

        // Total quantity
        browser.find("table.salesTotal")
          .find("tr.salesTotalBody", 0)
          .find("td.itemQuantity")
          .getText === "2"

        // Total amount(including outer tax)
        browser.find("table.salesTotal")
          .find("tr.salesTotalBody", 0)
          .find("td.itemPrice")
          .getText === String.format(
            "%1$,d円",
            Integer.valueOf(
              47 + 59 + 8
            )
          )

        // Shipping box quantity
        browser.find("table.salesTotal")
          .find("tr.salesTotalBody", 1)
          .find("td.itemQuantity")
          .getText === "2 箱"

        // Shipping fee
        browser.find("table.salesTotal")
          .find("tr.salesTotalBody", 1)
          .find("td.itemPrice")
          .getText === String.format("%1$,d円", Integer.valueOf(2345 + 3333))

        browser.find("table.salesTotal")
          .find("tr.salesTotalBody", 2)
          .find("td.itemPrice")
          .getText === String.format(
            "%1$,d円", Integer.valueOf(
              47 + 59 + 8
              + 2345 + 3333
            )
          )

        // 送付先
        browser
          .find("table.shippingAddress")
          .find("tr.shippingTableBody.courtesyName")
          .find("td", 1)
          .getText === "firstName lastName"

        browser
          .find("table.shippingAddress")
          .find("tr.shippingTableBody.furiganaKana")
          .find("td", 1)
          .getText === "firstNameKana lastNameKana"

        browser
          .find("table.shippingAddress")
          .find("tr.shippingTableBody.email")
          .find("td", 1)
          .getText === "email1"

        browser
          .find("table.shippingAddress")
          .find("tr.shippingTableBody.zip")
          .find("td", 1)
          .getText === "zip1 - zip2"

        val addressLine = browser
          .find("table.shippingAddress")
          .find("tr.shippingTableBody.address")
          .find("td", 1)
          .getText

          addressLine.contains(JapanPrefecture.三重県.toString) === true
          addressLine.contains("address1")
          addressLine.contains("address2")

        browser
          .find("table.shippingAddress")
          .find("tr.shippingTableBody.tel1")
          .find("td", 1)
          .getText === "123-2345"
      }}
    }
  }

  def createAddress(implicit conn: Connection) = Address.createNew(
    countryCode = CountryCode.JPN,
    firstName = "firstName",
    lastName = "lastName",
    firstNameKana = "firstNameKana",
    lastNameKana = "lastNameKana",
    zip1 = "zip1",
    zip2 = "zip2",
    prefecture = JapanPrefecture.三重県,
    address1 = "address1",
    address2 = "address2",
    tel1 = "123-2345",
    email = "email1"
  )
}
