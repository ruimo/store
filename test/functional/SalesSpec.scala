package functional

import org.joda.time.DateTime
import helpers.Helper.disableMailer
import helpers.UrlHelper
import helpers.UrlHelper._
import org.openqa.selenium.By
import org.openqa.selenium.By._
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
import java.util.concurrent.TimeUnit
import java.sql.Connection
import scala.collection.JavaConversions._
import com.ruimo.scoins.Scoping._

class SalesSpec extends Specification with SalesSpecBase  {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  "Sales" should {
    "Can sell item." in {
      val app = FakeApplication(
        additionalConfiguration = inMemoryDatabase(options = Map("MVCC" -> "true")) ++ disableMailer
      )
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        val adminUser = loginWithTestUser(browser)
        
        val site = Site.createNew(LocaleInfo.Ja, "Store01")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "内税")
        val taxHis = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))
        val item = Item.createNew(cat)
        ItemNumericMetadata.createNew(
          item, ItemNumericMetadataType.HEIGHT, 1
        )
        ItemTextMetadata.createNew(
          item, ItemTextMetadataType.ABOUT_HEIGHT, "Hello"
        )
        SiteItemNumericMetadata.createNew(
          site.id.get, item.id.get, SiteItemNumericMetadataType.STOCK, 2
        )
        SiteItemTextMetadata.createNew(
          site.id.get, item.id.get, SiteItemTextMetadataType.PRICE_MEMO, "World"
        )
        val siteItem = SiteItem.createNew(site, item)
        val itemClass = 1L
        SiteItemNumericMetadata.createNew(site.id.get, item.id.get, SiteItemNumericMetadataType.SHIPPING_SIZE, itemClass)
        val itemName = ItemName.createNew(item, Map(LocaleInfo.Ja -> "かえで"))
        val itemDesc = ItemDescription.createNew(item, site, "かえで説明")
        val itemPrice = ItemPrice.createNew(item, site)
        val itemPriceHistory = ItemPriceHistory.createNew(
          itemPrice, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
        )

        browser.goTo("http://localhost:3333" + itemQueryUrl())
        browser.await().atMost(5, TimeUnit.SECONDS).until(".addToCartButton").areDisplayed()
        browser.find(".addToCartButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).until(".ui-dialog-buttonset button").areDisplayed()
        // Goto cart button
        browser.find(".ui-dialog-buttonset button").get(1).click()

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find(".toEnterShippingAddressInner a").click()

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.fill("#firstName").`with`("firstName01")
        browser.fill("#lastName").`with`("lastName01")
        browser.fill("#firstNameKana").`with`("firstNameKana01")
        browser.fill("#lastNameKana").`with`("lastNameKana01")
        browser.fill("input[name='zip1']").`with`("146")
        browser.fill("input[name='zip2']").`with`("0082")
        browser.fill("#address1").`with`("address01")
        browser.fill("#address2").`with`("address02")
        browser.fill("#tel1").`with`("11111111")
        browser.find("#enterShippingAddressForm input[type='submit']").click()

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("commonTitle") + " " + Messages("cannot.ship.title")
        browser.find(".backToShippingLink").click()

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        val box = ShippingBox.createNew(site.id.get, itemClass, 3, "box01")
        val fee = ShippingFee.createNew(box.id.get, CountryCode.JPN, JapanPrefecture.東京都.code())
        val feeHistory = ShippingFeeHistory.createNew(fee.id.get, tax.id.get, BigDecimal(123), Some(100), date("9999-12-31"))
        browser.find("#enterShippingAddressForm input[type='submit']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("commonTitle") + " " + Messages("confirm.shipping.address")
        browser.find(".itemTableBody .itemName").getText === "かえで"
        browser.find(".itemTableBody .siteName").getText === "Store01"
        if (itemSizeExists)
          browser.find(".itemTableBody .itemSize").getText === Messages("item.size." + itemClass)
        browser.find(".itemTableBody .itemQuantity").getText === "1"
        browser.find(".itemTableBody .itemPrice").getText === "999円"
        browser.find(".itemTableBody .outerTaxAmount").getText === "0円"
        browser.find(".itemTableBody .grandTotal").getText === "999円"
        browser.find(".shippingTableBody .boxName").getText === "box01"
        browser.find(".shippingTableBody .boxUnitPrice").getText === "123円"
        browser.find(".shippingTableBody .boxQuantity").getText === "1 箱"
        browser.find(".shippingTableBody .boxPrice").getText === "123円"
        browser.find(".salesTotalBody", 0).find(".itemQuantity").getText === "1"
        browser.find(".salesTotalBody", 0).find(".itemPrice").getText === "999円"
        browser.find(".salesTotalBody", 1).find(".itemQuantity").getText === "1 箱"
        browser.find(".salesTotalBody", 1).find(".itemPrice").getText === "123円"
        browser.find(".salesTotalBody", 2).find(".itemPrice").getText === "1,122円"
        doWith(browser.find(".shippingAddress")) { e =>
          e.find(".shippingTableBody td.name").getText === "firstName01 lastName01"
          e.find(".shippingTableBody td.nameKana").getText === "firstNameKana01 lastNameKana01"
          e.find(".shippingTableBody td.zip").getText === "146 - 0082"
          e.find(".address .prefecture").getText === JapanPrefecture.東京都.toString
          e.find(".address .address1").getText === "address01"
          e.find(".address .address2").getText === "address02"
          e.find(".shippingTableBody .tel1").getText === "11111111"
        }
        browser.find("#finalizeTransactionForm input[type='submit']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("commonTitle") + " " + Messages("end.transaction")

        browser.find(".itemTableBody .itemNameBody").getText === "かえで"
        browser.find(".itemTableBody .siteName").getText === "Store01"
        if (itemSizeExists)
          browser.find(".itemTableBody .size").getText === Messages("item.size." + itemClass)
        browser.find(".itemTableBody .quantity").getText === "1"
        browser.find(".itemTableBody .itemPrice").getText === "999円"
        browser.find(".itemTableBody .subtotal").getText === "999円"
        browser.find(".itemTableBody .outerTaxAmount").getText === "0円"
        browser.find(".itemTableBody .grandTotal").getText === "999円"
        browser.find(".itemTableBody .siteName").getText === "Store01"
        browser.find(".shippingTableBody .boxName").getText === "box01"
        browser.find(".shippingTableBody .boxUnitPrice").getText === "123円"
        browser.find(".shippingTableBody .boxQuantity").getText === "1 箱"
        browser.find(".shippingTableBody .boxPrice").getText === "123円"
        browser.find(".salesTotal .salesTotalBody", 0).find(".itemQuantity").getText === "1"
        browser.find(".salesTotal .salesTotalBody", 0).find(".itemPrice").getText === "999円"
        browser.find(".salesTotal .salesTotalBody", 1).find(".itemQuantity").getText === "1 箱"
        browser.find(".salesTotal .salesTotalBody", 1).find(".itemPrice").getText === "123円"
        browser.find(".salesTotal .salesTotalBody", 2).find(".itemPrice").getText === "1,122円"
        doWith(browser.find(".shippingAddress")) { e =>
          e.find(".shippingTableBody .name").getText === "firstName01 lastName01"
          e.find(".shippingTableBody .nameKana").getText === "firstNameKana01 lastNameKana01"
          e.find(".shippingTableBody .zip").getText === "146 - 0082"

          e.find(".prefecture").getText === JapanPrefecture.東京都.toString
          e.find(".address1").getText === "address01"
          e.find(".address2").getText === "address02"
        }

        browser.find(".shippingTableBody .tel1").getText === "11111111"

        val headers = TransactionLogHeader.list()
        headers.size === 1
        val tran: PersistedTransaction = (new TransactionPersister()).load(headers(0).id.get, LocaleInfo.Ja)
        doWith(tran.tranSiteLog) { siteLog =>
          siteLog.size === 1
          siteLog(site.id.get).siteId === site.id.get
        }
        doWith(tran.siteTable) { siteTable =>
          siteTable.size === 1
          siteTable.head === site
        }
        val addressId = doWith(tran.shippingTable) { shippingTable =>
          shippingTable.size === 1
          val shippings = shippingTable(site.id.get)
          shippings.size === 1
          doWith(shippings.head) { shipping =>
            shipping.amount === BigDecimal(123)
            shipping.costAmount === Some(BigDecimal(100))
            shipping.itemClass === itemClass
            shipping.boxSize === 3
            shipping.taxId === tax.id.get
            shipping.boxCount === 1
            shipping.boxName === "box01"
            shipping.addressId
          }
        }

        doWith(Address.byId(addressId)) { addr =>
          addr.countryCode === CountryCode.JPN
          addr.firstName === "firstName01"
          addr.middleName === ""
          addr.lastName === "lastName01"
          addr.firstNameKana === "firstNameKana01"
          addr.lastNameKana === "lastNameKana01"
          addr.zip1 === "146"
          addr.zip2 === "0082"
          addr.zip3 === ""
          addr.prefecture === JapanPrefecture.東京都
          addr.address1 === "address01"
          addr.address2 === "address02"
          addr.tel1 === "11111111"
        }

        doWith(tran.taxTable) { taxTable =>
          taxTable.size === 1
          doWith(taxTable(site.id.get)) { taxes =>
            taxes.size === 1
            doWith(taxes.head) { tax =>
              tax.taxId === tax.id.get
              tax.taxType === TaxType.INNER_TAX
              tax.rate === BigDecimal(5)
              tax.targetAmount === BigDecimal(1122)
              tax.amount === BigDecimal(1122 * 5 / 105)
            }
          }
        }

        doWith(tran.itemTable) { itemTable =>
          doWith(itemTable(site.id.get)) { items =>
            items.size === 1
            doWith(items.head) { it =>
              doWith(it._1) { itemName =>
                itemName.localeId === LocaleInfo.Ja.id
                itemName.itemId === item.id.get
                itemName.name === "かえで"
              }

              doWith(it._2) { tranItem =>
                tranItem.quantity === 1
                tranItem.amount === BigDecimal(999)
                tranItem.costPrice === BigDecimal(888)
                tranItem.taxId === tax.id.get

                doWith(TransactionLogItemNumericMetadata.list(tranItem.id.get)) { mdTable =>
                  mdTable.size === 1
                  doWith(mdTable.head) { md =>
                    md.metadataType === ItemNumericMetadataType.HEIGHT
                    md.metadata === 1
                  }
                }

                doWith(TransactionLogItemTextMetadata.list(tranItem.id.get)) { mdTable =>
                  mdTable.size === 1
                  doWith(mdTable.head) { md =>
                    md.metadataType === ItemTextMetadataType.ABOUT_HEIGHT
                    md.metadata === "Hello"
                  }
                }

                doWith(TransactionLogSiteItemNumericMetadata.list(tranItem.id.get).toSeq) { mdTable =>
                  mdTable.size === 2
                  doWith(mdTable(0)) { md =>
                    md.metadataType === SiteItemNumericMetadataType.STOCK
                    md.metadata === 2
                  }
                  doWith(mdTable(1)) { md =>
                    md.metadataType === SiteItemNumericMetadataType.SHIPPING_SIZE
                    md.metadata === itemClass
                  }
                }

                doWith(TransactionLogSiteItemTextMetadata.list(tranItem.id.get)) { mdTable =>
                  mdTable.size === 1
                  doWith(mdTable.head) { md =>
                    md.metadataType === SiteItemTextMetadataType.PRICE_MEMO
                    md.metadata === "World"
                  }
                }
              }

              it._3 === None
            }
          }
        }
      }}
    }

    "If price is expired, error should be shown." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ disableMailer)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        val adminUser = loginWithTestUser(browser)
        val site = Site.createNew(LocaleInfo.Ja, "Store01")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "内税")
        val taxHis = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))
        val item = Item.createNew(cat)
        val item2 = Item.createNew(cat)
        ItemNumericMetadata.createNew(
          item, ItemNumericMetadataType.HEIGHT, 1
        )
        ItemTextMetadata.createNew(
          item, ItemTextMetadataType.ABOUT_HEIGHT, "Hello"
        )
        SiteItemNumericMetadata.createNew(
          site.id.get, item.id.get, SiteItemNumericMetadataType.STOCK, 2
        )
        SiteItemTextMetadata.createNew(
          site.id.get, item.id.get, SiteItemTextMetadataType.PRICE_MEMO, "World"
        )
        val siteItem = SiteItem.createNew(site, item)
        SiteItem.createNew(site, item2)
        val itemClass = 1L
        SiteItemNumericMetadata.createNew(site.id.get, item.id.get, SiteItemNumericMetadataType.SHIPPING_SIZE, itemClass)
        val itemName = ItemName.createNew(item, Map(LocaleInfo.Ja -> "かえで"))
        val itemName2 = ItemName.createNew(item2, Map(LocaleInfo.Ja -> "まつ"))
        val itemDesc = ItemDescription.createNew(item, site, "かえで説明")
        val itemDesc2 = ItemDescription.createNew(item2, site, "まつ説明")
        val itemPrice = ItemPrice.createNew(item, site)
        val itemPrice2 = ItemPrice.createNew(item2, site)
        val iph = ItemPriceHistory.createNew(
          itemPrice, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
        )
        val iph2 = ItemPriceHistory.createNew(
          itemPrice2, tax, CurrencyInfo.Jpy, BigDecimal(1999), None, BigDecimal("1888"), date("9999-12-31")
        )

        browser.goTo("http://localhost:3333" + itemQueryUrl())
        browser.await().atMost(5, TimeUnit.SECONDS).until(".addToCartButton").areDisplayed()
        browser.find(".addToCartButton").click()
        browser.await().atMost(30, TimeUnit.SECONDS).until(".ui-dialog-buttonset button").areDisplayed()
        browser.find(".addToCartButton", 1).click()
        browser.await().atMost(30, TimeUnit.SECONDS).until(".ui-dialog-buttonset button").areDisplayed()

        // Expire price history.
        ItemPriceHistory.update(
          iph.id.get, iph.taxId, iph.currency.id, iph.unitPrice, iph.listPrice, iph.costPrice,
          new DateTime(System.currentTimeMillis - 10000)
        )

        // Goto cart button
        browser.find(".ui-dialog-buttonset button").get(1).click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("commonTitle") + " " + Messages("itemExpiredTitle")
        browser.find(".expiredItemRow").getTexts.size === 1
        browser.find(".expiredItemRow .siteName").getText === site.name
        browser.find(".expiredItemRow .itemName").getText === itemName(Ja).name
        browser.find("#removeExpiredItemsButton").click()
        browser.find(".shoppingCartTable tr").size === 2
        browser.find(".shoppingCartTable tr", 1).find("td").getText === itemName2(Ja).name
      }}
    }

    "Item expires at shipping confirmation." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ disableMailer)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        val adminUser = loginWithTestUser(browser)
        
        val site = Site.createNew(LocaleInfo.Ja, "Store01")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "内税")
        val taxHis = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))
        val item = Item.createNew(cat)
        ItemNumericMetadata.createNew(
          item, ItemNumericMetadataType.HEIGHT, 1
        )
        ItemTextMetadata.createNew(
          item, ItemTextMetadataType.ABOUT_HEIGHT, "Hello"
        )
        SiteItemNumericMetadata.createNew(
          site.id.get, item.id.get, SiteItemNumericMetadataType.STOCK, 2
        )
        SiteItemTextMetadata.createNew(
          site.id.get, item.id.get, SiteItemTextMetadataType.PRICE_MEMO, "World"
        )
        val siteItem = SiteItem.createNew(site, item)
        val itemClass = 1L
        SiteItemNumericMetadata.createNew(site.id.get, item.id.get, SiteItemNumericMetadataType.SHIPPING_SIZE, itemClass)
        val itemName = ItemName.createNew(item, Map(LocaleInfo.Ja -> "かえで"))
        val itemDesc = ItemDescription.createNew(item, site, "かえで説明")
        val itemPrice = ItemPrice.createNew(item, site)
        val iph = ItemPriceHistory.createNew(
          itemPrice, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
        )

        browser.goTo("http://localhost:3333" + itemQueryUrl())
        browser.await().atMost(5, TimeUnit.SECONDS).until(".addToCartButton").areDisplayed()
        browser.find(".addToCartButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).until(".ui-dialog-buttonset button").areDisplayed()
        // Goto cart button
        browser.find(".ui-dialog-buttonset button").get(1).click()

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find(".toEnterShippingAddressInner a").click()

        val box = ShippingBox.createNew(site.id.get, itemClass, 3, "box01")
        val fee = ShippingFee.createNew(box.id.get, CountryCode.JPN, JapanPrefecture.東京都.code())
        val feeHistory = ShippingFeeHistory.createNew(fee.id.get, tax.id.get, BigDecimal(123), Some(100), date("9999-12-31"))

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.fill("#firstName").`with`("firstName01")
        browser.fill("#lastName").`with`("lastName01")
        browser.fill("#firstNameKana").`with`("firstNameKana01")
        browser.fill("#lastNameKana").`with`("lastNameKana01")
        browser.fill("input[name='zip1']").`with`("146")
        browser.fill("input[name='zip2']").`with`("0082")
        browser.fill("#address1").`with`("address01")
        browser.fill("#address2").`with`("address02")
        browser.fill("#tel1").`with`("11111111")

        // Expire price history.
        ItemPriceHistory.update(
          iph.id.get, iph.taxId, iph.currency.id, iph.unitPrice, iph.listPrice, iph.costPrice,
          new DateTime(System.currentTimeMillis - 10000)
        )

        browser.find("#enterShippingAddressForm input[type='submit']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("commonTitle") + " " + Messages("itemExpiredTitle")
        browser.find(".expiredItemRow").getTexts.size === 1
        browser.find(".expiredItemRow .siteName").getText === site.name
        browser.find(".expiredItemRow .itemName").getText === itemName(Ja).name
        browser.find("#removeExpiredItemsButton").click()
        browser.find(".shoppingCartEmpty").getText === Messages("shopping.cart.empty")
      }}
    }

    "Item expires on finalizing transaction." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ disableMailer)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        val adminUser = loginWithTestUser(browser)
        
        val site = Site.createNew(LocaleInfo.Ja, "Store01")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "内税")
        val taxHis = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))
        val item = Item.createNew(cat)
        ItemNumericMetadata.createNew(
          item, ItemNumericMetadataType.HEIGHT, 1
        )
        ItemTextMetadata.createNew(
          item, ItemTextMetadataType.ABOUT_HEIGHT, "Hello"
        )
        SiteItemNumericMetadata.createNew(
          site.id.get, item.id.get, SiteItemNumericMetadataType.STOCK, 2
        )
        SiteItemTextMetadata.createNew(
          site.id.get, item.id.get, SiteItemTextMetadataType.PRICE_MEMO, "World"
        )
        val siteItem = SiteItem.createNew(site, item)
        val itemClass = 1L
        SiteItemNumericMetadata.createNew(site.id.get, item.id.get, SiteItemNumericMetadataType.SHIPPING_SIZE, itemClass)
        val itemName = ItemName.createNew(item, Map(LocaleInfo.Ja -> "かえで"))
        val itemDesc = ItemDescription.createNew(item, site, "かえで説明")
        val itemPrice = ItemPrice.createNew(item, site)
        val iph = ItemPriceHistory.createNew(
          itemPrice, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
        )

        browser.goTo("http://localhost:3333" + itemQueryUrl())
        browser.await().atMost(5, TimeUnit.SECONDS).until(".addToCartButton").areDisplayed()
        browser.find(".addToCartButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).until(".ui-dialog-buttonset button").areDisplayed()

        val box = ShippingBox.createNew(site.id.get, itemClass, 3, "box01")
        val fee = ShippingFee.createNew(box.id.get, CountryCode.JPN, JapanPrefecture.東京都.code())
        val feeHistory = ShippingFeeHistory.createNew(fee.id.get, tax.id.get, BigDecimal(123), Some(100), date("9999-12-31"))

        // Goto cart button
        browser.find(".ui-dialog-buttonset button").get(1).click()

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find(".toEnterShippingAddressInner a").click()

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.fill("#firstName").`with`("firstName01")
        browser.fill("#lastName").`with`("lastName01")
        browser.fill("#firstNameKana").`with`("firstNameKana01")
        browser.fill("#lastNameKana").`with`("lastNameKana01")
        browser.fill("input[name='zip1']").`with`("146")
        browser.fill("input[name='zip2']").`with`("0082")
        browser.fill("#address1").`with`("address01")
        browser.fill("#address2").`with`("address02")
        browser.fill("#tel1").`with`("11111111")

        browser.find("#enterShippingAddressForm input[type='submit']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title() === Messages("commonTitle") + " " + Messages("confirm.shipping.address")

        // Expire price history.
        ItemPriceHistory.update(
          iph.id.get, iph.taxId, iph.currency.id, iph.unitPrice, iph.listPrice, iph.costPrice,
          new DateTime(System.currentTimeMillis - 10000)
        )
        
        browser.find("#finalizeTransactionForm input[type='submit']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("commonTitle") + " " + Messages("itemExpiredTitle")
        browser.find(".expiredItemRow").getTexts.size === 1
        browser.find(".expiredItemRow .siteName").getText === site.name
        browser.find(".expiredItemRow .itemName").getText === itemName(Ja).name
        browser.find("#removeExpiredItemsButton").click()
        browser.find(".shoppingCartEmpty").getText === Messages("shopping.cart.empty")
      }}
    }
  }
}
