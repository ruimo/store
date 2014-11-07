package functional

import org.joda.time.format.DateTimeFormat
import java.util.concurrent.TimeUnit
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
import com.ruimo.scoins.Scoping._
import org.joda.time.format.DateTimeFormat

class PurchasedCouponSpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  case class Tran(
    now: Long,
    tranHeader: TransactionLogHeader,
    tranSiteHeader: Seq[TransactionLogSite],
    transporter1: Transporter,
    transporter2: Transporter,
    transporterName1: TransporterName,
    transporterName2: TransporterName,
    address: Address,
    coupon: Coupon
  )

  "Purchased coupon" should {
    "Show purchased coupon list" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        createNormalUser(
          browser, "user01", "password01", "user01@mail.xxx", "firstName01", "lastName01", "company01"
        )
        createNormalUser(
          browser, "user02", "password02", "user02@mail.xxx", "firstName02", "lastName02", "company02"
        )

        val user1 = StoreUser.findByUserName("user01").get
        val user2 = StoreUser.findByUserName("user02").get

        val tran01 = createTransaction(lang, user1, 1)
        val tran02 = createTransaction(lang, user2, 10)

        logoff(browser)
        login(browser, "user01", "password01")
        browser.goTo(
          "http://localhost:3333" + controllers.routes.CouponHistory.showPurchasedCouponList() + "?lang=" + lang.code
        )

        doWith(browser.find("#purchasedCouponHistory .body")) { body =>
          body.size === 1
          body.find(".tranId").getText === tran01.tranHeader.id.get.toString
          body.find(".siteName").getText === "商店1"
          body.find(".tranDate").getText === DateTimeFormat.forPattern(
            Messages("published.date.format")
          ).print(tran01.tranHeader.transactionTime)
          body.find(".itemName").getText === "植木1"
        }

        browser.find(".itemName a").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".date").find("span", 1).getText() === 
          DateTimeFormat.forPattern(Messages("published.date.format")).print(tran01.tranHeader.transactionTime)
        browser.find(".siteName").getText() === Messages("coupon.user.company.name", "company01")
        browser.find(".name").getText() === "firstName01 lastName01 様"
        
      }}
    }
  }

  def createTransaction(
    lang: Lang, user: StoreUser, startIdx: Int
  )(implicit conn: Connection): Tran = {
    val tax = Tax.createNew
    val taxHis = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

    val site1 = Site.createNew(Ja, "商店" + startIdx)
    val site2 = Site.createNew(Ja, "商店" + (startIdx + 1))
    
    val cat1 = Category.createNew(
      Map(Ja -> "植木", En -> "Plant")
    )
    
    val item1 = Item.createNew(cat1)
    val item2 = Item.createNew(cat1)
    val item3 = Item.createNew(cat1)
    
    val coupon = Coupon.createNew()
    Coupon.updateAsCoupon(item1.id.get)

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
      trans1.id.get, Ja, "トマト運輸" + startIdx
    )
    val transName2 = TransporterName.createNew(
      trans2.id.get, Ja, "ヤダワ急便" + startIdx
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
      Transaction(user.id.get, CurrencyInfo.Jpy, cartTotal1, Some(addr1), shippingTotal1, shippingDate1, now)
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
      addr1,
      coupon
    )
  }
}
