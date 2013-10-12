package functional

import play.api.test._
import play.api.test.Helpers._
import play.api.Play.current
import java.sql.Connection

import functional.Helper._
import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.i18n.{Lang, Messages}
import models._
import play.api.db.DB
import play.api.test.TestServer
import play.api.test.FakeApplication
import java.sql.Date.{valueOf => date}

object ConfirmShippingSpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  "ConfirmShipping" should {
    "No item." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val address = createAddress
        val addressHistory = ShippingAddressHistory.createNew(
          user.id.get, address
        )

        browser.goTo(
          "http://localhost:3333" + controllers.routes.Shipping.confirmShippingAddressJa().url + "?lang=" + lang.code
        )
        browser.title === Messages("confirm.shipping.address")
        browser.find("table.itemTable").size === 0
        browser.find("table.shipping").size === 0

        browser
          .find("table.shippingAddress")
          .find("tr.shippingTableBody")
          .find("td", 1)
          .getText === "firstName lastName"

        browser
          .find("table.shippingAddress")
          .find("tr.shippingTableBody", 1)
          .find("td", 1)
          .getText === "firstNameKana lastNameKana"

        browser
          .find("table.shippingAddress")
          .find("tr.shippingTableBody", 2)
          .find("td", 1)
          .getText === "zip1 - zip2"

        val addressLine = browser
          .find("table.shippingAddress")
          .find("tr.shippingTableBody", 3)
          .find("td", 1)
          .getText

          addressLine.contains(JapanPrefecture.三重県.toString) === true
          addressLine.contains("address1")
          addressLine.contains("address2")

        browser
          .find("table.shippingAddress")
          .find("tr.shippingTableBody", 4)
          .find("td", 1)
          .getText === "123-2345"
      }}
    }

    "More than one site and more than item classes." in {
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
        val item3 = Item.createNew(cat1)

        val name1 = ItemName.createNew(item1, Map(Ja -> "杉", En -> "Cedar"))
        val name2 = ItemName.createNew(item2, Map(Ja -> "梅", En -> "Ume"))
        val name3 = ItemName.createNew(item3, Map(Ja -> "竹", En -> "Bamboo"))
        
        val desc1 = ItemDescription.createNew(item1, site1, "杉説明")
        val desc2 = ItemDescription.createNew(item2, site2, "梅説明")
        val desc3 = ItemDescription.createNew(item3, site2, "竹説明")

        val metadata1 = SiteItemNumericMetadata.createNew(
          site1.id.get, item1.id.get, SiteItemNumericMetadataType.SHIPPING_SIZE, 1
        )
        val metadata2 = SiteItemNumericMetadata.createNew(
          site2.id.get, item2.id.get, SiteItemNumericMetadataType.SHIPPING_SIZE, 1
        )
        val metadata3 = SiteItemNumericMetadata.createNew(
          site2.id.get, item3.id.get, SiteItemNumericMetadataType.SHIPPING_SIZE, 2
        )

        val box1_1 = ShippingBox.createNew(
          site1.id.get, 1, 7, "商店1の箱1"
        )
        val box2_1 = ShippingBox.createNew(
          site2.id.get, 1, 3, "商店2の箱1"
        )
        val box2_2 = ShippingBox.createNew(
          site2.id.get, 2, 5, "商店2の箱2"
        )

        val fee1 = ShippingFee.createNew(box1_1.id.get, CountryCode.JPN, JapanPrefecture.三重県.code)
        val fee2 = ShippingFee.createNew(box2_1.id.get, CountryCode.JPN, JapanPrefecture.三重県.code)
        val fee3 = ShippingFee.createNew(box2_2.id.get, CountryCode.JPN, JapanPrefecture.三重県.code)

        val feeHis1 = ShippingFeeHistory.createNew(fee1.id.get, tax.id.get, BigDecimal(2345), date("9999-12-31"))
        val feeHis2 = ShippingFeeHistory.createNew(fee2.id.get, tax.id.get, BigDecimal(3333), date("9999-12-31"))
        val feeHis3 = ShippingFeeHistory.createNew(fee3.id.get, tax.id.get, BigDecimal(4444), date("9999-12-31"))

        SiteItem.createNew(site1, item1)
        SiteItem.createNew(site2, item2)
        SiteItem.createNew(site2, item3)

        val price1 = ItemPrice.createNew(item1, site1)
        val price2 = ItemPrice.createNew(item2, site2)
        val price3 = ItemPrice.createNew(item3, site2)

        val ph1 = ItemPriceHistory.createNew(price1, tax, CurrencyInfo.Jpy, BigDecimal(101), date("9999-12-31"))
        val ph2 = ItemPriceHistory.createNew(price2, tax, CurrencyInfo.Jpy, BigDecimal(301), date("9999-12-31"))
        val ph3 = ItemPriceHistory.createNew(price3, tax, CurrencyInfo.Jpy, BigDecimal(401), date("9999-12-31"))

        val cart1 = ShoppingCartItem.addItem(user.id.get, site1.id.get, item1.id.get, 15)
        val cart2 = ShoppingCartItem.addItem(user.id.get, site2.id.get, item2.id.get, 28)
        val cart3 = ShoppingCartItem.addItem(user.id.get, site2.id.get, item3.id.get, 40)

        browser.goTo(
          "http://localhost:3333" + controllers.routes.Shipping.confirmShippingAddressJa().url + "?lang=" + lang.code
        )
        browser.title === Messages("confirm.shipping.address")

        browser.find("table.itemTable").find("tr.itemTableBody").size === 6
        browser.find("table.itemTable")
          .find("tr.itemTableBody", 0)
          .find("td.itemName")
          .getText === "杉"

        browser.find("table.itemTable")
          .find("tr.itemTableBody", 0)
          .find("td.itemQuantity")
          .getText === "15"

        browser.find("table.itemTable")
          .find("tr.itemTableBody", 0)
          .find("td.itemPrice")
          .getText === String.format("%1$,d円", Integer.valueOf(15 * 101))

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
          .getText === "28"

        browser.find("table.itemTable")
          .find("tr.itemTableBody", 1)
          .find("td.itemPrice")
          .getText === String.format("%1$,d円", Integer.valueOf(28 * 301))

        browser.find("table.itemTable")
          .find("tr.itemTableBody", 1)
          .find("td.siteName")
          .getText === "商店2"

        browser.find("table.itemTable")
          .find("tr.itemTableBody", 1)
          .find("td.itemSize")
          .getText === Messages("item.size.1")

        browser.find("table.itemTable")
          .find("tr.itemTableBody", 2)
          .find("td.itemName")
          .getText === "竹"

        browser.find("table.itemTable")
          .find("tr.itemTableBody", 2)
          .find("td.itemQuantity")
          .getText === "40"

        browser.find("table.itemTable")
          .find("tr.itemTableBody", 2)
          .find("td.itemPrice")
          .getText === String.format("%1$,d円", Integer.valueOf(40 * 401))

        browser.find("table.itemTable")
          .find("tr.itemTableBody", 2)
          .find("td.siteName")
          .getText === "商店2"

        browser.find("table.itemTable")
          .find("tr.itemTableBody", 2)
          .find("td.itemSize")
          .getText === Messages("item.size.2")

        browser.find("table.itemTable")
          .find("tr.subtotalWithoutTax")
          .find("td.subtotal")
          .getText === String.format(
            "%1$,d円",
            Integer.valueOf(15 * 101 + 28 * 301 + 40 * 401)
          )

        // 送料
        browser.find("h2.shippingSiteName").size === 2
        browser.find("h2.shippingSiteName", 0)
          .getText === "商店1"
        browser.find("h2.shippingSiteName", 1)
          .getText === "商店2"

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
          .getText === "3 個"

        browser.find("table.shipping")
          .find("tr.shippingTableBody", 0)
          .find("td.boxPrice")
          .getText === String.format("%1$,d円", Integer.valueOf(3 * 2345))

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
          .getText === "10 個"

        browser.find("table.shipping", 1)
          .find("tr.shippingTableBody", 0)
          .find("td.boxPrice")
          .getText === String.format("%1$,d円", Integer.valueOf(10 * 3333))

        browser.find("table.shipping", 1)
          .find("tr.shippingTableBody", 1)
          .find("td.boxName")
          .getText === "商店2の箱2"

        browser.find("table.shipping", 1)
          .find("tr.shippingTableBody", 1)
          .find("td.boxUnitPrice")
          .getText === String.format("%1$,d円", Integer.valueOf(4444))

        browser.find("table.shipping", 1)
          .find("tr.shippingTableBody", 1)
          .find("td.boxQuantity")
          .getText === "8 個"

        browser.find("table.shipping", 1)
          .find("tr.shippingTableBody", 1)
          .find("td.boxPrice")
          .getText === String.format("%1$,d円", Integer.valueOf(8 * 4444))

        // Total quantity
        browser.find("table.salesTotal")
          .find("tr.salesTotalBody", 0)
          .find("td.itemQuantity")
          .getText === (15 + 28 + 40) + " 個"

        // Total amount(including outer tax)
        browser.find("table.salesTotal")
          .find("tr.salesTotalBody", 0)
          .find("td.itemPrice")
          .getText === String.format(
            "%1$,d円",
            Integer.valueOf(
              15 * 101 + 28 * 301 + 40 * 401
              + (15 * 101 + 28 * 301 + 40 * 401) * 8 / 100
            )
          )

        // Shipping box quantity
        browser.find("table.salesTotal")
          .find("tr.salesTotalBody", 1)
          .find("td.itemQuantity")
          .getText === (3 + 10 + 8) + " 個"

        // Shipping fee
        browser.find("table.salesTotal")
          .find("tr.salesTotalBody", 1)
          .find("td.itemPrice")
          .getText === String.format("%1$,d円", Integer.valueOf(3 * 2345 + 10 * 3333 + 8 * 4444))

        browser.find("table.salesTotal")
          .find("tr.salesTotalBody", 2)
          .find("td.itemPrice")
          .getText === String.format(
            "%1$,d円", Integer.valueOf(
              15 * 101 + 28 * 301 + 40 * 401
              + (15 * 101 + 28 * 301 + 40 * 401) * 8 / 100
              + (3 * 2345 + 10 * 3333 + 8 * 4444)
            )
          )

        // 送付先
        browser
          .find("table.shippingAddress")
          .find("tr.shippingTableBody")
          .find("td", 1)
          .getText === "firstName lastName"

        browser
          .find("table.shippingAddress")
          .find("tr.shippingTableBody", 1)
          .find("td", 1)
          .getText === "firstNameKana lastNameKana"

        browser
          .find("table.shippingAddress")
          .find("tr.shippingTableBody", 2)
          .find("td", 1)
          .getText === "zip1 - zip2"

        val addressLine = browser
          .find("table.shippingAddress")
          .find("tr.shippingTableBody", 3)
          .find("td", 1)
          .getText

          addressLine.contains(JapanPrefecture.三重県.toString) === true
          addressLine.contains("address1")
          addressLine.contains("address2")

        browser
          .find("table.shippingAddress")
          .find("tr.shippingTableBody", 4)
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
    tel1 = "123-2345"
  )
}
