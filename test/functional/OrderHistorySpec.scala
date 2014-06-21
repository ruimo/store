package functional

import org.specs2.mutable.Specification
import models._
import java.sql.Date.{valueOf => date}
import java.sql.Connection
import LocaleInfo._
import play.api.i18n.{Lang, Messages}
import play.api.db._
import helpers.Helper._
import play.api.test._
import play.api.test.Helpers._
import play.api.Play.current
import helpers.ViewHelpers
import org.joda.time.format.DateTimeFormat
import java.util.concurrent.TimeUnit

class OrderHistorySpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  case class Tran(
    now: Long,
    tranHeader: TransactionLogHeader,
    tranSiteHeader: Seq[TransactionLogSite],
    transporter1: Transporter,
    transporter2: Transporter,
    transporterName1: TransporterName,
    transporterName2: TransporterName,
    address: Address
  )

  "Order history" should {
    "Show login user's order history" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val tran = createTransaction(lang, user)
        browser.goTo(
          "http://localhost:3333" + controllers.routes.OrderHistory.showOrderHistory() + "?lang=" + lang.code
        )
        browser.title === Messages("order.history.title")
        doWith(browser.find(".orderHistoryInnerTable1")) { b =>
          b.find(".transactionTime").getText ===
            "%1$tY/%1$tm/%1$td %1$tH:%1$tM".format(tran.now)
          b.find(".tranNo").getText === tran.tranHeader.id.toString
          val user = StoreUser(tran.tranHeader.userId)
          b.find(".buyerName").getText === user.firstName + " " + user.lastName
          b.find(".subtotal").getText === ViewHelpers.toAmount(tran.tranSiteHeader.head.totalAmount)
          b.find(".outerTaxAmount").getText === ViewHelpers.toAmount(0)
          b.find(".total").getText === 
            ViewHelpers.toAmount(tran.tranSiteHeader.head.totalAmount)
        }
        doWith(browser.find(".shippingAddressTable")) { b =>
          b.find(".name").getText === tran.address.firstName + " " + tran.address.lastName
          b.find(".zip").getText === tran.address.zip1 + " - " + tran.address.zip2
          b.find(".prefecture").getText === tran.address.prefecture.toString
          b.find(".address1").getText === tran.address.address1
          b.find(".address2").getText === tran.address.address2
          b.find(".tel1").getText === tran.address.tel1
          b.find(".comment").getText === tran.address.comment
        }
        doWith(browser.find(".orderHistoryInnerTable2")) { b =>
          b.find(".status").getText === Messages("transaction.status.ORDERED")
          b.find(".shippingDate").getText === 
            DateTimeFormat.forPattern(Messages("shipping.date.format")).print(
              date("2013-02-03")
            )
        }
        doWith(browser.find(".orderHistoryInnerTable3")) { b =>
          b.find("td.itemName").getText === "植木1"
          b.find("td.unitPrice").getText === "100円"
          b.find("td.quantity").getText === "3"
          b.find("td.price").find(".body").getText === "300円"

          b.find("td.itemName").get(1).getText === "植木3"
          b.find("td.unitPrice").get(1).getText === "300円"
          b.find("td.quantity").get(1).getText === "7"
          b.find("td.price").get(1).find(".body").getText === "2,100円"

          b.find("td.subtotalBody").find(".body").getText === "2,400円"
        }
        doWith(browser.find(".orderHistoryInnerTable4")) { b =>
          b.find("td.boxName").getText === "site-box1"
          b.find("td.boxPrice").getText === "123円"
          b.find("td.subtotalBody").getText === "123円"
        }

        doWith(browser.find(".orderHistoryInnerTable1").get(1)) { b =>
          b.find(".transactionTime").getText ===
            "%1$tY/%1$tm/%1$td %1$tH:%1$tM".format(tran.now)
          b.find(".tranNo").getText === tran.tranHeader.id.toString
          val user = StoreUser(tran.tranHeader.userId)
          b.find(".buyerName").getText === user.firstName + " " + user.lastName
          b.find(".subtotal").getText === ViewHelpers.toAmount(1468)
          b.find(".outerTaxAmount").getText === ViewHelpers.toAmount(0)
          b.find(".total").getText === 
            ViewHelpers.toAmount(1468)
        }
        doWith(browser.find(".shippingAddressTable").get(1)) { b =>
          b.find(".name").getText === tran.address.firstName + " " + tran.address.lastName
          b.find(".zip").getText === tran.address.zip1 + " - " + tran.address.zip2
          b.find(".prefecture").getText === tran.address.prefecture.toString
          b.find(".address1").getText === tran.address.address1
          b.find(".address2").getText === tran.address.address2
          b.find(".tel1").getText === tran.address.tel1
          b.find(".comment").getText === tran.address.comment
        }
        doWith(browser.find(".orderHistoryInnerTable2").get(1)) { b =>
          b.find(".status").getText === Messages("transaction.status.ORDERED")
          b.find(".shippingDate").getText === 
            DateTimeFormat.forPattern(Messages("shipping.date.format")).print(
              date("2013-02-04")
            )
        }
        doWith(browser.find(".orderHistoryInnerTable3").get(1)) { b =>
          b.find("td.itemName").getText === "植木2"
          b.find("td.unitPrice").getText === "200円"
          b.find("td.quantity").getText === "5"
          b.find("td.price").find(".body").getText === "1,000円"

          b.find("td.subtotalBody").find(".body").getText === "1,000円"
        }
        doWith(browser.find(".orderHistoryInnerTable4").get(1)) { b =>
          b.find("td.boxName").getText === "site-box2"
          b.find("td.boxPrice").getText === "468円"
          b.find("td.subtotalBody").getText === "468円"
        }
      }}
    }

    "Can add item that is bought before" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val tran = createTransaction(lang, user)
        browser.goTo(
          "http://localhost:3333" + controllers.routes.Purchase.clear() + "?lang=" + lang.code
        )
        browser.goTo(
          "http://localhost:3333" + controllers.routes.OrderHistory.showOrderHistory() + "?lang=" + lang.code
        )
        browser.title === Messages("order.history.title")
        browser.find(".orderHistoryInnerTable3").get(0).find("button").get(0).click()

        browser.await().atMost(10, TimeUnit.SECONDS).until(".ui-dialog-buttonset").areDisplayed()
        browser.find(".ui-dialog-titlebar").find("span.ui-dialog-title").getText === Messages("shopping.cart")
        doWith(browser.find("#cartDialogContent")) { b =>
          b.find("td.itemName").getText === "植木1"
          b.find("td.siteName").getText === "商店1"
          b.find("td.unitPrice").getText === "100円"
          b.find("td.quantity").getText === "1"
          b.find("td.price").getText === "100円"
        }

        browser.find(".ui-dialog-buttonset").find("button").get(0).click()
        browser.await().atMost(10, TimeUnit.SECONDS).until(".ui-dialog-buttonset").areNotDisplayed()

        browser.find(".orderHistoryInnerTable3").get(0).find("button").get(2).click()
        browser.await().atMost(10, TimeUnit.SECONDS).until(".ui-dialog-buttonset").areDisplayed()

        browser.find(".ui-dialog-titlebar").find("span.ui-dialog-title").getText === Messages("shopping.cart")
        doWith(browser.find("#cartDialogContent")) { b =>
          b.find("td.itemName").getText === "植木1"
          b.find("td.siteName").getText === "商店1"
          b.find("td.unitPrice").getText === "100円"
          b.find("td.quantity").getText === "4"
          b.find("td.price").getText === "400円"

          b.find("td.itemName").get(1).getText === "植木3"
          b.find("td.siteName").get(1).getText === "商店1"
          b.find("td.unitPrice").get(1).getText === "300円"
          b.find("td.quantity").get(1).getText === "7"
          b.find("td.price").get(1).getText === "2,100円"
        }

        browser.find(".ui-dialog-buttonset").find("button").get(0).click()
        browser.await().atMost(10, TimeUnit.SECONDS).until(".ui-dialog-buttonset").areNotDisplayed()

        browser.find(".orderHistoryInnerTable3").get(1).find("button").get(0).click()
        browser.await().atMost(10, TimeUnit.SECONDS).until(".ui-dialog-buttonset").areDisplayed()

        browser.find(".ui-dialog-titlebar").find("span.ui-dialog-title").getText === Messages("shopping.cart")
        doWith(browser.find("#cartDialogContent")) { b =>
          b.find("td.itemName").getText === "植木1"
          b.find("td.siteName").getText === "商店1"
          b.find("td.unitPrice").getText === "100円"
          b.find("td.quantity").getText === "4"
          b.find("td.price").getText === "400円"

          b.find("td.itemName").get(1).getText === "植木3"
          b.find("td.siteName").get(1).getText === "商店1"
          b.find("td.unitPrice").get(1).getText === "300円"
          b.find("td.quantity").get(1).getText === "7"
          b.find("td.price").get(1).getText === "2,100円"

          b.find("td.itemName").get(2).getText === "植木2"
          b.find("td.siteName").get(2).getText === "商店2"
          b.find("td.unitPrice").get(2).getText === "200円"
          b.find("td.quantity").get(2).getText === "1"
          b.find("td.price").get(2).getText === "200円"
        }
      }}
    }
  }

  def createTransaction(lang: Lang, user: StoreUser)(implicit conn: Connection): Tran = {
    val tax = Tax.createNew
    val taxHis = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

    val site1 = Site.createNew(Ja, "商店1")
    val site2 = Site.createNew(Ja, "商店2")
    
    val cat1 = Category.createNew(
      Map(Ja -> "植木", En -> "Plant")
    )
    
    val item1 = Item.createNew(cat1)
    val item2 = Item.createNew(cat1)
    val item3 = Item.createNew(cat1)
    
    SiteItem.createNew(site1, item1)
    SiteItem.createNew(site2, item2)
    SiteItem.createNew(site1, item3)
    
    SiteItemNumericMetadata.createNew(site1.id.get, item1.id.get, SiteItemNumericMetadataType.SHIPPING_SIZE, 1L)
    SiteItemNumericMetadata.createNew(site2.id.get, item2.id.get, SiteItemNumericMetadataType.SHIPPING_SIZE, 1L)
    SiteItemNumericMetadata.createNew(site1.id.get, item3.id.get, SiteItemNumericMetadataType.SHIPPING_SIZE, 1L)
    
    val itemName1 = ItemName.createNew(item1, Map(Ja -> "植木1"))
    val itemName2 = ItemName.createNew(item2, Map(Ja -> "植木2"))
    val itemName3 = ItemName.createNew(item3, Map(Ja -> "植木3"))
    
    val itemDesc1 = ItemDescription.createNew(item1, site1, "desc1")
    val itemDesc2 = ItemDescription.createNew(item2, site2, "desc2")
    val itemDesc3 = ItemDescription.createNew(item3, site1, "desc3")
    
    val itemPrice1 = ItemPrice.createNew(item1, site1)
    val itemPrice2 = ItemPrice.createNew(item2, site2)
    val itemPrice3 = ItemPrice.createNew(item3, site1)
    
    val itemPriceHis1 = ItemPriceHistory.createNew(
      itemPrice1, tax, CurrencyInfo.Jpy, BigDecimal("100"), BigDecimal("90"), date("9999-12-31")
    )
    val itemPriceHis2 = ItemPriceHistory.createNew(
      itemPrice2, tax, CurrencyInfo.Jpy, BigDecimal("200"), BigDecimal("190"), date("9999-12-31")
    )
    val itemPriceHis3 = ItemPriceHistory.createNew(
      itemPrice3, tax, CurrencyInfo.Jpy, BigDecimal("300"), BigDecimal("290"), date("9999-12-31")
    )
    
    val shoppingCartItem1 = ShoppingCartItem.addItem(user.id.get, site1.id.get, item1.id.get, 3)
    val shoppingCartItem2 = ShoppingCartItem.addItem(user.id.get, site2.id.get, item2.id.get, 5)
    val shoppingCartItem3 = ShoppingCartItem.addItem(user.id.get, site1.id.get, item3.id.get, 7)
    
    val shoppingCartTotal1 = List(
      ShoppingCartTotalEntry(
        shoppingCartItem1,
        itemName1(Ja),
        itemDesc1,
        site1,
        itemPriceHis1,
        taxHis
      )
    )
    
    val addr1 = Address.createNew(
      countryCode = CountryCode.JPN,
      firstName = "firstName1",
      lastName = "lastName1",
      zip1 = "zip1",
      zip2 = "zip2",
      prefecture = JapanPrefecture.東京都,
      address1 = "address1-1",
      address2 = "address1-2",
      tel1 = "tel1-1",
      comment = "comment1"
    )
    
    val trans1 = Transporter.createNew
    val trans2 = Transporter.createNew
    val transName1 = TransporterName.createNew(
      trans1.id.get, Ja, "トマト運輸"
    )
    val transName2 = TransporterName.createNew(
      trans2.id.get, Ja, "ヤダワ急便"
    )
    
    val box1 = ShippingBox.createNew(site1.id.get, 1L, 3, "site-box1")
    val box2 = ShippingBox.createNew(site2.id.get, 1L, 2, "site-box2")
    
    val fee1 = ShippingFee.createNew(box1.id.get, CountryCode.JPN, JapanPrefecture.東京都.code)
    val fee2 = ShippingFee.createNew(box2.id.get, CountryCode.JPN, JapanPrefecture.東京都.code)
    
    val feeHis1 = ShippingFeeHistory.createNew(
      fee1.id.get, tax.id.get, BigDecimal(123), date("9999-12-31")
    )
    val feeHis2 = ShippingFeeHistory.createNew(
      fee2.id.get, tax.id.get, BigDecimal(234), date("9999-12-31")
    )
    val now = System.currentTimeMillis

    val shippingTotal1 = ShippingFeeHistory.feeBySiteAndItemClass(
      CountryCode.JPN, JapanPrefecture.東京都.code,
      ShippingFeeEntries().add(site1, 1L, 3).add(site2, 1L, 4),
      now
    )
    val shippingDate1 = ShippingDate(
      Map(
        site1.id.get -> ShippingDateEntry(site1.id.get, date("2013-02-03")),
        site2.id.get -> ShippingDateEntry(site2.id.get, date("2013-02-04"))
      )
    )

    val cartTotal1 = ShoppingCartItem.listItemsForUser(Ja, user.id.get)
    val tranId = (new TransactionPersister).persist(
      Transaction(user.id.get, CurrencyInfo.Jpy, cartTotal1, addr1, shippingTotal1, shippingDate1, now)
    )
    val tranList = TransactionLogHeader.list()
    val tranSiteList = TransactionLogSite.list()

    Tran(
      now,
      tranList(0),
      tranSiteList,
      transporter1 = trans1,
      transporter2 = trans2,
      transporterName1 = transName1,
      transporterName2 = transName2,
      addr1
    )
  }
}
