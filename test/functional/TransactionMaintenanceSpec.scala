package functional

import helpers.UrlHelper
import helpers.UrlHelper._
import anorm._
import play.api.db._
import play.api.test._
import play.api.test.Helpers._
import play.api.Play.current

import helpers.Helper._

import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.i18n.{Lang, Messages}
import play.api.db.DB
import models._
import java.sql.Date.{valueOf => date}
import LocaleInfo._
import java.sql.Connection
import org.openqa.selenium.By
import org.joda.time.format.DateTimeFormat
import org.joda.time.LocalTime
import java.util.concurrent.TimeUnit
import controllers.TransactionMaintenance
import java.io.{StringReader, BufferedReader}
import helpers.Helper.disableMailer
import com.ruimo.scoins.Scoping._

class TransactionMaintenanceSpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime
  val conf = inMemoryDatabase() ++ disableMailer

  case class Tran(
    tranHeader: TransactionLogHeader,
    tranSiteHeader: Seq[TransactionLogSite],
    transporter1: Transporter,
    transporter2: Transporter,
    transporterName1: TransporterName,
    transporterName2: TransporterName
  )

  "Transaction maitenance" should {
    "Validation error will shown" in {
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val tran = createTransaction(lang, user)
        browser.goTo(
          "http://localhost:3333" + 
          controllers.routes.TransactionMaintenance.index(orderBySpec = "transaction_site_id").url.addParm("lang", lang.code)
        )
        browser.await().atMost(5, TimeUnit.SECONDS).until(".site").areDisplayed()
        browser.title === Messages("transactionMaintenanceTitle")
        browser.find(".site").getText === "商店1"
        browser.find(".shippingDate").getText === "2013年02月03日"
        browser.find(".transactionAmount").getText === "2,400円"
        browser.find(".transactionShipping").getText === "123円"
        browser.find(".buyer").find(".companyName").getText === user.companyName.get
        browser.find(".buyer").find(".name").getText === user.firstName + " " + user.lastName
        browser.find(".buyer").find(".email").getText === user.email
        browser.find(".shippingTableBody").find(".zip").getText === "zip1 - zip2"
        browser.find(".shippingTableBody").find(".prefecture").getText === JapanPrefecture.東京都.toString
        browser.find(".shippingTableBody").find(".address1").getText === "address1-1"
        browser.find(".shippingTableBody").find(".address2").getText === "address1-2"
        browser.find(".shippingTableBody").find(".tel1").getText === "tel1-1"
        browser.find(".shippingTableBody").find(".comment").getText === "comment1"
        val tranSiteId = tran.tranSiteHeader(0).id.get
        browser.webDriver
          .findElement(By.id("status" + tranSiteId))
          .findElement(By.cssSelector("option[value=\"0\"]")).isSelected === true

        browser
          .find("#transporter" + tranSiteId)
          .find("option[value=\"" + tran.transporter1.id.get + "\"]").getText === tran.transporterName1.transporterName
        browser
          .find("#transporter" + tranSiteId)
          .find("option[value=\"" + tran.transporter2.id.get + "\"]").getText === tran.transporterName2.transporterName

        browser
          .find("#transporter" + tranSiteId)
          .find("option[value=\"" + tran.transporter2.id.get + "\"]").click()

        // Input error(slip code is not filled).
        browser.await().atMost(5, TimeUnit.SECONDS).until(
          "#changeShippingInfoButton" + tranSiteId
        ).areDisplayed()
        browser.find("#changeShippingInfoButton" + tranSiteId).click()

        browser.await().atMost(5, TimeUnit.SECONDS).until(
          "#changeShippingInfoButton" + tranSiteId
        ).areDisplayed()
        browser.find("#slipCode" + tranSiteId + "_field").find(".error").getText === Messages("error.required")

        browser.fill("#slipCode" + tranSiteId).`with`("12345678")
        browser.find(
          "#changeShippingInfoButton" + tranSiteId
        ).click()

        browser.title === Messages("transactionMaintenanceTitle")
        browser.webDriver
          .findElement(By.id("status" + tranSiteId)).findElement(By.cssSelector("option[value=\"1\"]")).isSelected === true

        doWith(browser.find("#shippingStatusTable" + tranSiteId)) { tbl =>
          tbl.find(".transporter").getText === tran.transporterName2.transporterName
          tbl.find(".slipCode").getText === "12345678"
        }

        // Cancel
        browser.webDriver
          .findElement(By.id("status" + tranSiteId)).findElement(By.cssSelector("option[value=\"2\"]")).click()
        browser.find("#changeShippingStatusButton" + tranSiteId).click();
        browser.await().atMost(5, TimeUnit.SECONDS).until("#status" + tranSiteId).areDisplayed()

        browser.webDriver
          .findElement(By.id("status" + tranSiteId)).findElement(By.cssSelector("option[value=\"2\"]")).isSelected === true
      }}
    }

    "Transaction cancelation" in {
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val tran = createTransaction(lang, user)
        browser.goTo(
          "http://localhost:3333" + 
          controllers.routes.TransactionMaintenance.index(orderBySpec = "transaction_site_id").url.addParm("lang", lang.code)
        )
        browser.await().atMost(5, TimeUnit.SECONDS).until(".site").areDisplayed()

        // Cancel transaction.
        browser.find(".cancelShippingButton").click()

        // Dialog should be shown.
        browser.await().atMost(65, TimeUnit.SECONDS).until(".ui-dialog-buttonset").areDisplayed()
        browser.find(".ui-dialog-buttonset").find("button", 0).click()

        browser.find(".shippingStatusTable").find(".transporter").getText === "-"
        browser.find(".shippingStatusTable").find(".slipCode").getText === "-"
      }}
    }

    "Abort transaction cancelation" in {
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val tran = createTransaction(lang, user)
        browser.goTo(
          "http://localhost:3333" + 
          controllers.routes.TransactionMaintenance.index(orderBySpec = "transaction_site_id").url.addParm("lang", lang.code)
        )
        browser.await().atMost(5, TimeUnit.SECONDS).until(".site").areDisplayed()

        // Cancel transaction.
        browser.find(".cancelShippingButton").click()

        // Dialog should be shown.
        browser.await().atMost(5, TimeUnit.SECONDS).until(".ui-dialog-buttonset").areDisplayed()
        browser.find(".ui-dialog-buttonset").find("button", 1).click()

        // Aborted. Transporter/slip code form are still shown.
        find("#transporterId_field") must be_!=(null)
        find("#slipCode_field") must be_!=(null)
      }}
    }

    "Download csv" in {
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        implicit val loginSession = LoginSession(user, None, System.currentTimeMillis() + 10000)
        val tran = createTransaction(lang, user)
        val csv1 = TransactionMaintenance.createCsv(tran.tranHeader.id.get, tran.tranSiteHeader(0).id.get)
        val csv2 = TransactionMaintenance.createCsv(tran.tranHeader.id.get, tran.tranSiteHeader(1).id.get)
        val csvLines1 = new BufferedReader(new StringReader(csv1))
        val csvLines2 = new BufferedReader(new StringReader(csv2))
        csvLines1.readLine === "%s,%s,%s,%s,%s,%s,%s,%s".format(
          Messages("csv.tran.detail.id"),
          Messages("csv.tran.detail.date"),
          Messages("csv.tran.detail.shippingDate"),
          Messages("csv.tran.detail.type"),
          Messages("csv.tran.detail.itemName"),
          Messages("csv.tran.detail.quantity"),
          Messages("csv.tran.detail.amount"),
          Messages("csv.tran.detail.costPrice")
        )
        csvLines2.readLine === "%s,%s,%s,%s,%s,%s,%s,%s".format(
          Messages("csv.tran.detail.id"),
          Messages("csv.tran.detail.date"),
          Messages("csv.tran.detail.shippingDate"),
          Messages("csv.tran.detail.type"),
          Messages("csv.tran.detail.itemName"),
          Messages("csv.tran.detail.quantity"),
          Messages("csv.tran.detail.amount"),
          Messages("csv.tran.detail.costPrice")
        )

        val lines1 = readFully(csvLines1).sorted
        lines1.size === 3

        lines1(0) === (
          "%1$d,".format(tran.tranHeader.id.get) +
          Messages("csv.tran.detail.shippingDate.format").format(tran.tranHeader.transactionTime) + "," +
          "2013/02/03," +
          Messages("csv.tran.detail.type.item") + "," +
          "植木1," +
          "3," +
          "100.00," +
          "90.00"
        )
        lines1(1) === (
          "%1$d,".format(tran.tranHeader.id.get) +
          Messages("csv.tran.detail.shippingDate.format").format(tran.tranHeader.transactionTime) + "," +
          "2013/02/03," +
          Messages("csv.tran.detail.type.item") + "," +
          "植木3," +
          "7," +
          "300.00," +
          "290.00"
        )
        lines1(2) === (
          "%1$d,".format(tran.tranHeader.id.get) +
          Messages("csv.tran.detail.shippingDate.format").format(tran.tranHeader.transactionTime) + "," +
          "2013/02/03," +
          Messages("csv.tran.detail.type.shipping") + "," +
          "site-box1," +
          "1," +
          "123.00," +
          "0"
        )

        val lines2 = readFully(csvLines2).sorted
        lines2.size === 2

        lines2(0) === (
          "%1$d,".format(tran.tranHeader.id.get) +
          Messages("csv.tran.detail.shippingDate.format").format(tran.tranHeader.transactionTime) + "," +
          "2013/02/03," +
          Messages("csv.tran.detail.type.item") + "," +
          "植木2," +
          "5," +
          "200.00," +
          "190.00"
        )
        lines2(1) === (
          "%1$d,".format(tran.tranHeader.id.get) +
          Messages("csv.tran.detail.shippingDate.format").format(tran.tranHeader.transactionTime) + "," +
          "2013/02/03," +
          Messages("csv.tran.detail.type.shipping") + "," +
          "site-box2," +
          "2," +
          "234.00," +
          "0"
        )
      }
    }}
  }

  def createTransaction(lang: Lang, user: StoreUser, count: Int = 1)(implicit conn: Connection): Tran = {
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
      itemPrice1, tax, CurrencyInfo.Jpy, BigDecimal("100"), None, BigDecimal("90"), date("9999-12-31")
    )
    val itemPriceHis2 = ItemPriceHistory.createNew(
      itemPrice2, tax, CurrencyInfo.Jpy, BigDecimal("200"), None, BigDecimal("190"), date("9999-12-31")
    )
    val itemPriceHis3 = ItemPriceHistory.createNew(
      itemPrice3, tax, CurrencyInfo.Jpy, BigDecimal("300"), None, BigDecimal("290"), date("9999-12-31")
    )
    
    val shoppingCartItem1 = ShoppingCartItem.addItem(user.id.get, site1.id.get, item1.id.get.id, 3)
    val shoppingCartItem2 = ShoppingCartItem.addItem(user.id.get, site2.id.get, item2.id.get.id, 5)
    val shoppingCartItem3 = ShoppingCartItem.addItem(user.id.get, site1.id.get, item3.id.get.id, 7)
    
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
      trans1.id.get, LocaleInfo.Ja, "トマト運輸"
    )
    val transName2 = TransporterName.createNew(
      trans2.id.get, LocaleInfo.Ja, "ヤダワ急便"
    )
    
    val box1 = ShippingBox.createNew(site1.id.get, 1L, 3, "site-box1")
    val box2 = ShippingBox.createNew(site2.id.get, 1L, 2, "site-box2")
    
    val fee1 = ShippingFee.createNew(box1.id.get, CountryCode.JPN, JapanPrefecture.東京都.code)
    val fee2 = ShippingFee.createNew(box2.id.get, CountryCode.JPN, JapanPrefecture.東京都.code)
    
    val feeHis1 = ShippingFeeHistory.createNew(
      fee1.id.get, tax.id.get, BigDecimal(123), None, date("9999-12-31")
    )
    val feeHis2 = ShippingFeeHistory.createNew(
      fee2.id.get, tax.id.get, BigDecimal(234), None, date("9999-12-31")
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
        site2.id.get -> ShippingDateEntry(site2.id.get, date("2013-02-03"))
      )
    )

    val cartTotal1 = ShoppingCartItem.listItemsForUser(LocaleInfo.Ja, user.id.get)
    (1 to count) foreach {
      i => (new TransactionPersister).persist(
        Transaction(user.id.get, CurrencyInfo.Jpy, cartTotal1, Some(addr1), shippingTotal1, shippingDate1, now)
      )
    }

    val tranList = TransactionLogHeader.list()
    val tranSiteList = TransactionLogSite.list()

    Tran(
      tranList(0),
      tranSiteList,
      transporter1 = trans1,
      transporter2 = trans2,
      transporterName1 = transName1,
      transporterName2 = transName2
    )
  }
}
