package functional

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
import scala.collection.JavaConversions._
import com.ruimo.scoins.Scoping._

class AccountingBillSpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  case class Master(
    sites: Vector[Site],
    items: Vector[Item],
    itemNames: Vector[Map[LocaleInfo, ItemName]],
    itemPriceHistories: Vector[ItemPriceHistory],
    boxes: Vector[ShippingBox],
    transporters: Vector[Transporter],
    transporterNames: Vector[TransporterName]
  )

  case class Tran(
    shoppingCartItems: Vector[ShoppingCartItem],
    shippingDate: ShippingDate,
    tranHeader: TransactionLogHeader,
    tranSiteHeader: Seq[TransactionLogSite]
  )

  "Accounting bill" should {
    "Only transaction for the specified store will be shown" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val adminUser = loginWithTestUser(browser)

        // Need site for other customizations where company is selected from sites available in the application.
        val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
        createNormalUser(
          browser, "user01", "password01", "user01@mail.xxx", "firstName01", "lastName01", "company01"
        )
        val user = StoreUser.findByUserName("user01").get

        val master = createMaster
        val tran = createTransaction(master, user)
        browser.goTo(
          "http://localhost:3333" + controllers.routes.AccountingBill.index() + "?lang=" + lang.code
        )
        browser.find("#siteDropDown").find("option[value=\"" + master.sites(1).id.get + "\"]").click()

        browser.fill("#storeYear").`with`("%tY".format(tran.tranHeader.transactionTime))
        browser.fill("#storeMonth").`with`("%tm".format(tran.tranHeader.transactionTime))
        browser.find("#storeSubmit").click();

        // Only transaction of tran.sites(1) should be shown.
        browser.find(".accountingBillTable").size() === 1
        doWith(browser.find(".accountingBillTable")) { tbl =>
          doWith(tbl.find(".accountingBillHeaderTable")) { headerTbl =>
            headerTbl.find(".tranId").getText === tran.tranHeader.id.get.toString
            headerTbl.find(".tranDateTime").getText === "%1$tY/%1$tm/%1$td %1$tH:%1$tM".format(tran.tranHeader.transactionTime)
            headerTbl.find(".tranBuyer").getText === user.firstName + " " + user.lastName
          }

          doWith(tbl.find(".transactionStatus")) { statusTbl =>
            statusTbl.find(".status").getText === Messages("transaction.status.ORDERED")
            statusTbl.find(".shippingDate").getText === DateTimeFormat.forPattern(Messages("shipping.date.format")).print(
              tran.shippingDate.bySiteId(master.sites(1).id.get).shippingDate
            )
          }

          val quantity = tran.shoppingCartItems.filter(_.siteId == master.sites(1).id.get).head.quantity
          tbl.find(".itemName.body").size === 1
          tbl.find(".itemName.body").getText === master.itemNames(1)(Ja).name
          tbl.find(".quantity.body").getText === quantity.toString
          tbl.find(".price.body").getText ===
            "%,.0f円".format(master.itemPriceHistories(1).costPrice * quantity)
          tbl.find(".subtotal.body").getText ===
            "%,.0f円".format(master.itemPriceHistories(1).costPrice * quantity)
          tbl.find(".boxName.body").getText === master.boxes.filter(_.siteId == master.sites(1).id.get).head.boxName
          val boxCount = (5 + 1) / 2
          tbl.find(".boxCount.body").getText === boxCount.toString
          tbl.find(".boxPrice.body").getText === "%,d円".format(234 * boxCount)
          tbl.find(".subtotal.box.body").getText === "%,d円".format(234 * boxCount)
          tbl.find(".tax.body").getText ===
            "%,d円".format((master.itemPriceHistories(1).costPrice * quantity).toInt * 5 / 100)
          tbl.find(".total.body").getText ===
            "%,d円".format(
              (master.itemPriceHistories(1).costPrice * quantity).toInt +
              (master.itemPriceHistories(1).costPrice * quantity).toInt * 5 / 100 +
              234 * boxCount
            )
        }
      }}
    }

    "All transaction by users will be shown" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val adminUser = loginWithTestUser(browser)
        createNormalUser(
          browser, "user01", "password01", "user01@mail.xxx", "firstName01", "lastName01", "company01"
        )
        createNormalUser(
          browser, "user02", "password02", "user02@mail.xxx", "firstName02", "lastName02", "company02"
        )
        val user01 = StoreUser.findByUserName("user01").get
        val user02 = StoreUser.findByUserName("user02").get

        val master = createMaster
        val trans = Vector(
          createTransaction(master, user01),
          createTransaction(master, user02),
          createTransaction(master, user01)
        )
        browser.goTo(
          "http://localhost:3333" + controllers.routes.AccountingBill.index() + "?lang=" + lang.code
        )
        browser.find("#siteDropDown").find("option[value=\"" + master.sites(1).id.get + "\"]").click()

        browser.fill("#userYear").`with`("%tY".format(trans(0).tranHeader.transactionTime))
        browser.fill("#userMonth").`with`("%tm".format(trans(0).tranHeader.transactionTime))
        browser.find("#userSubmit").click();

        browser.find(".accountingBillTable").size() === 6
        val (tbl0, tbl1) = if (
          browser.find(".accountingBillTable", 0).find(".itemName.body").size == 2
        ) (0, 1) else (1, 0)

        doWith(browser.find(".accountingBillTable", tbl0)) { tbl =>
          doWith(tbl.find(".accountingBillHeaderTable")) { headerTbl =>
            headerTbl.find(".tranId").getText === trans(2).tranHeader.id.get.toString
            headerTbl.find(".tranDateTime").getText === "%1$tY/%1$tm/%1$td %1$tH:%1$tM".format(trans(2).tranHeader.transactionTime)
            headerTbl.find(".tranBuyer").getText === user01.firstName + " " + user01.lastName
          }

          doWith(tbl.find(".transactionStatus")) { statusTbl =>
            statusTbl.find(".status").getText === Messages("transaction.status.ORDERED")
            statusTbl.find(".shippingDate").getText === DateTimeFormat.forPattern(Messages("shipping.date.format")).print(
              trans(0).shippingDate.bySiteId(master.sites(0).id.get).shippingDate
            )
          }
          val items = trans(0).shoppingCartItems.filter(_.siteId == master.sites(0).id.get)
          tbl.find(".itemName.body").size === 2
          tbl.find(".itemName.body", 0).getText === master.itemNames(0)(Ja).name
          tbl.find(".itemName.body", 1).getText === master.itemNames(2)(Ja).name

          tbl.find(".quantity.body", 0).getText === items(0).quantity.toString
          tbl.find(".quantity.body", 1).getText === items(1).quantity.toString
          tbl.find(".price.body", 0).getText ===
            "%,.0f円".format(master.itemPriceHistories(0).unitPrice * items(0).quantity)
          tbl.find(".price.body", 1).getText ===
            "%,.0f円".format(master.itemPriceHistories(2).unitPrice * items(1).quantity)
          tbl.find(".subtotal.body").getText ===
            "%,.0f円".format(
              master.itemPriceHistories(0).unitPrice * items(0).quantity +
              master.itemPriceHistories(2).unitPrice * items(1).quantity
            )
          tbl.find(".boxName.body").getText === master.boxes.filter(_.siteId == master.sites(0).id.get).head.boxName
          val boxCount = 1
          tbl.find(".boxCount.body").getText === boxCount.toString
          tbl.find(".boxPrice.body").getText === "%,d円".format(123 * boxCount)
          tbl.find(".subtotal.box.body").getText === "%,d円".format(123 * boxCount)
          tbl.find(".tax.body").getText ===
            "%,d円".format(
              (
                master.itemPriceHistories(0).unitPrice * items(0).quantity +
                master.itemPriceHistories(2).unitPrice * items(1).quantity
              ).toInt * 5 / 100
            )
          tbl.find(".total.body").getText ===
            "%,d円".format(
              (
                master.itemPriceHistories(0).unitPrice * items(0).quantity +
                master.itemPriceHistories(2).unitPrice * items(1).quantity
              ).toInt * 105 / 100 +
              123 * boxCount
            )
        }

        // Should be ordered by user and transaction id.
        doWith(browser.find(".accountingBillTable", tbl1)) { tbl =>
          doWith(tbl.find(".accountingBillHeaderTable")) { headerTbl =>
            headerTbl.find(".tranId").getText === trans(2).tranHeader.id.get.toString
            headerTbl.find(".tranBuyer").getText === user01.firstName + " " + user01.lastName
          }
        }

        doWith(browser.find(".accountingBillTable", 2)) { tbl =>
          doWith(tbl.find(".accountingBillHeaderTable")) { headerTbl =>
            headerTbl.find(".tranId").getText === trans(0).tranHeader.id.get.toString
            headerTbl.find(".tranBuyer").getText === user01.firstName + " " + user01.lastName
          }
        }

        doWith(browser.find(".accountingBillTable", 3)) { tbl =>
          doWith(tbl.find(".accountingBillHeaderTable")) { headerTbl =>
            headerTbl.find(".tranId").getText === trans(0).tranHeader.id.get.toString
            headerTbl.find(".tranBuyer").getText === user01.firstName + " " + user01.lastName
          }
        }

        doWith(browser.find(".accountingBillTable", 4)) { tbl =>
          doWith(tbl.find(".accountingBillHeaderTable")) { headerTbl =>
            headerTbl.find(".tranId").getText === trans(1).tranHeader.id.get.toString
            headerTbl.find(".tranBuyer").getText === user02.firstName + " " + user02.lastName
          }
        }

        doWith(browser.find(".accountingBillTable", 5)) { tbl =>
          doWith(tbl.find(".accountingBillHeaderTable")) { headerTbl =>
            headerTbl.find(".tranId").getText === trans(1).tranHeader.id.get.toString
            headerTbl.find(".tranBuyer").getText === user02.firstName + " " + user02.lastName
          }
        }
      }}
    }
  }

  def createMaster(implicit conn: Connection): Master = {
    val taxes = Vector(
      Tax.createNew, Tax.createNew
    )
    val taxHistories = Vector(
      TaxHistory.createNew(taxes(0), TaxType.OUTER_TAX, BigDecimal("5"), date("9999-12-31")),
      TaxHistory.createNew(taxes(1), TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))
    )

    val sites = Vector(Site.createNew(Ja, "商店1"), Site.createNew(Ja, "商店2"))
    
    val cat1 = Category.createNew(
      Map(Ja -> "植木", En -> "Plant")
    )
    
    val items = Vector(Item.createNew(cat1), Item.createNew(cat1), Item.createNew(cat1))
    
    SiteItem.createNew(sites(0), items(0))
    SiteItem.createNew(sites(1), items(1))
    SiteItem.createNew(sites(0), items(2))
    
    SiteItemNumericMetadata.createNew(sites(0).id.get, items(0).id.get, SiteItemNumericMetadataType.SHIPPING_SIZE, 1L)
    SiteItemNumericMetadata.createNew(sites(1).id.get, items(1).id.get, SiteItemNumericMetadataType.SHIPPING_SIZE, 1L)
    SiteItemNumericMetadata.createNew(sites(0).id.get, items(2).id.get, SiteItemNumericMetadataType.SHIPPING_SIZE, 1L)
    
    val itemNames = Vector(
      ItemName.createNew(items(0), Map(Ja -> "植木1")),
      ItemName.createNew(items(1), Map(Ja -> "植木2")),
      ItemName.createNew(items(2), Map(Ja -> "植木3"))
    )
    
    val itemDesc1 = ItemDescription.createNew(items(0), sites(0), "desc1")
    val itemDesc2 = ItemDescription.createNew(items(1), sites(1), "desc2")
    val itemDesc3 = ItemDescription.createNew(items(2), sites(0), "desc3")
    
    val itemPrice1 = ItemPrice.createNew(items(0), sites(0))
    val itemPrice2 = ItemPrice.createNew(items(1), sites(1))
    val itemPrice3 = ItemPrice.createNew(items(2), sites(0))
    
    val itemPriceHistories = Vector(
      ItemPriceHistory.createNew(
        itemPrice1, taxes(0), CurrencyInfo.Jpy, BigDecimal("100"), BigDecimal("90"), date("9999-12-31")
      ),
      ItemPriceHistory.createNew(
        itemPrice2, taxes(0), CurrencyInfo.Jpy, BigDecimal("200"), BigDecimal("190"), date("9999-12-31")
      ),
      ItemPriceHistory.createNew(
        itemPrice3, taxes(0), CurrencyInfo.Jpy, BigDecimal("300"), BigDecimal("290"), date("9999-12-31")
      )
    )

    val boxes = Vector(
      ShippingBox.createNew(sites(0).id.get, 1L, 3, "site-box1"),
      ShippingBox.createNew(sites(1).id.get, 1L, 2, "site-box2")
    )
    
    val fee1 = ShippingFee.createNew(boxes(0).id.get, CountryCode.JPN, JapanPrefecture.東京都.code)
    val fee2 = ShippingFee.createNew(boxes(1).id.get, CountryCode.JPN, JapanPrefecture.東京都.code)
    
    val feeHis1 = ShippingFeeHistory.createNew(
      fee1.id.get, taxes(1).id.get, BigDecimal(123), date("9999-12-31")
    )
    val feeHis2 = ShippingFeeHistory.createNew(
      fee2.id.get, taxes(1).id.get, BigDecimal(234), date("9999-12-31")
    )

    val transporters = Vector(Transporter.createNew, Transporter.createNew)
    val transporterNames = Vector(
      TransporterName.createNew(
        transporters(0).id.get, LocaleInfo.Ja, "トマト運輸"
      ),
      TransporterName.createNew(
        transporters(1).id.get, LocaleInfo.Ja, "ヤダワ急便"
      )
    )

    Master(sites, items, itemNames, itemPriceHistories, boxes, transporters, transporterNames)
  }

  def createTransaction(
    master: Master, user: StoreUser, now: Long = System.currentTimeMillis, offset: Int = 0
  )(implicit conn: Connection): Tran = {
    ShoppingCartItem.removeForUser(user.id.get)

    val shoppingCartItems = Vector(
      ShoppingCartItem.addItem(user.id.get, master.sites(0).id.get, master.items(0).id.get.id, 3),
      ShoppingCartItem.addItem(user.id.get, master.sites(1).id.get, master.items(1).id.get.id, 5),
      ShoppingCartItem.addItem(user.id.get, master.sites(0).id.get, master.items(2).id.get.id, 7)
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
    
    val shippingTotal1 = ShippingFeeHistory.feeBySiteAndItemClass(
      CountryCode.JPN, JapanPrefecture.東京都.code,
      ShippingFeeEntries().add(master.sites(0), 1L, 3).add(master.sites(1), 1L, 5),
      now
    )
    val shippingDate1 = ShippingDate(
      Map(
        master.sites(0).id.get -> ShippingDateEntry(master.sites(0).id.get, date("2013-02-03")),
        master.sites(1).id.get -> ShippingDateEntry(master.sites(1).id.get, date("2013-02-03"))
      )
    )

    val cartTotal1 = ShoppingCartItem.listItemsForUser(LocaleInfo.Ja, user.id.get)
    val tranId = (new TransactionPersister).persist(
      Transaction(user.id.get, CurrencyInfo.Jpy, cartTotal1, Some(addr1), shippingTotal1, shippingDate1, now)
    )
    val tranList = TransactionLogHeader.list()
    val tranSiteList = TransactionLogSite.list()

    Tran(
      shoppingCartItems,
      shippingDate1,
      tranList(0),
      tranSiteList
    )
  }
}


