package models

import org.specs2.mutable._

import anorm._
import anorm.SqlParser
import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current
import java.sql.Date.{valueOf => date}

class TransactionPersisterSpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  "TransactionPersister" should {
    "Can persist one item transaction" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()
        DB.withConnection { implicit conn =>
          val user = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )
          val site = Site.createNew(LocaleInfo.Ja, "商店1")
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
            itemPrice, tax, CurrencyInfo.Jpy, BigDecimal(999), BigDecimal(900), date("9999-12-31")
          )
          val shoppingCartItem = ShoppingCartItem.addItem(
            user.id.get, site.id.get, item.id.get.id, 2
          )
          val cartTotal = ShoppingCartItem.listItemsForUser(LocaleInfo.Ja, user.id.get)
          val address = Address.createNew(
            countryCode = CountryCode.JPN,
            firstName = "First Name",
            lastName = "Last Name"
          )
          val itemClass = 1L
          val now = System.currentTimeMillis
          val box = ShippingBox.createNew(site.id.get, itemClass, 3, "box1")
          val fee = ShippingFee.createNew(
            box.id.get, CountryCode.JPN, JapanPrefecture.東京都.code
          )
          val feeHistory = ShippingFeeHistory.createNew(
            fee.id.get, tax.id.get, BigDecimal(123), date("9999-12-31")
          )
          val shippingTotal = ShippingFeeHistory.feeBySiteAndItemClass(
            CountryCode.JPN, JapanPrefecture.東京都.code,
            ShippingFeeEntries().add(site, itemClass, 2),
            now
          )

          val shippingDate = ShippingDate(Map(site.id.get -> ShippingDateEntry(site.id.get, date("2013-02-03"))))

          (new TransactionPersister).persist(
            Transaction(user.id.get, CurrencyInfo.Jpy, cartTotal, address, shippingTotal, shippingDate, now)
          )

          val header = TransactionLogHeader.list()
          header.size === 1
          header(0).userId === user.id.get
          header(0).transactionTime === now
          header(0).currencyId === CurrencyInfo.Jpy.id
          header(0).totalAmount === BigDecimal(999 * 2 + 123)
          header(0).taxAmount === BigDecimal((999 * 2 + 123) * 5 / 105)
          header(0).transactionType === TransactionType.NORMAL

          val siteLog = TransactionLogSite.list()
          siteLog.size === 1
          siteLog(0).transactionId === header(0).id.get
          siteLog(0).siteId === site.id.get
          siteLog(0).totalAmount === BigDecimal(999 * 2 + 123)
          siteLog(0).taxAmount === BigDecimal((999 * 2 + 123) * 5 / 105)

          val shippingLog = TransactionLogShipping.list()
          shippingLog.size === 1
          shippingLog(0).transactionSiteId === siteLog(0).id.get
          shippingLog(0).amount === BigDecimal(123)
          shippingLog(0).addressId === address.id.get
          shippingLog(0).itemClass === itemClass
          shippingLog(0).boxSize === 3
          shippingLog(0).taxId === tax.id.get
          shippingLog(0).shippingDate === date("2013-02-03").getTime

          val taxLog = TransactionLogTax.list()
          taxLog.size === 1
          taxLog(0).transactionSiteId === siteLog(0).id.get
          taxLog(0).taxId === tax.id.get
          taxLog(0).taxType === TaxType.INNER_TAX
          taxLog(0).rate === BigDecimal(5)
          taxLog(0).targetAmount === BigDecimal(999 * 2 + 123)
          taxLog(0).amount === BigDecimal((999 * 2 + 123) * 5 / 105)

        }
      }      
    }

    "Can persist coupon item transaction" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()
        DB.withConnection { implicit conn =>
          val user = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )
          val site = Site.createNew(LocaleInfo.Ja, "商店1")
          val cat = Category.createNew(Map(LocaleInfo.Ja -> "植木"))
          val tax = Tax.createNew
          val taxHistory = TaxHistory.createNew(
            tax, TaxType.INNER_TAX, BigDecimal(5), date("9999-12-31")
          )
          val item = Item.createNew(cat)
          val coupon = Coupon.createNew()
          CouponItem.create(item.id.get, coupon.id.get)

          val siteItem = SiteItem.createNew(site, item)
          val itemName = ItemName.createNew(item, Map(LocaleInfo.Ja -> "かえで"))
          val itemDesc = ItemDescription.createNew(item, site, "かえで説明")
          val itemPrice = ItemPrice.createNew(item, site)
          val itemPriceHistory = ItemPriceHistory.createNew(
            itemPrice, tax, CurrencyInfo.Jpy, BigDecimal(999), BigDecimal(900), date("9999-12-31")
          )
          val shoppingCartItem = ShoppingCartItem.addItem(
            user.id.get, site.id.get, item.id.get.id, 2
          )
          val cartTotal = ShoppingCartItem.listItemsForUser(LocaleInfo.Ja, user.id.get)
          val address = Address.createNew(
            countryCode = CountryCode.JPN,
            firstName = "First Name",
            lastName = "Last Name"
          )
          val itemClass = 1L
          val now = System.currentTimeMillis
          val box = ShippingBox.createNew(site.id.get, itemClass, 3, "box1")
          val fee = ShippingFee.createNew(
            box.id.get, CountryCode.JPN, JapanPrefecture.東京都.code
          )
          val feeHistory = ShippingFeeHistory.createNew(
            fee.id.get, tax.id.get, BigDecimal(123), date("9999-12-31")
          )
          val shippingTotal = ShippingFeeHistory.feeBySiteAndItemClass(
            CountryCode.JPN, JapanPrefecture.東京都.code,
            ShippingFeeEntries().add(site, itemClass, 2),
            now
          )

          val shippingDate = ShippingDate(Map(site.id.get -> ShippingDateEntry(site.id.get, date("2013-02-03"))))

          (new TransactionPersister).persist(
            Transaction(user.id.get, CurrencyInfo.Jpy, cartTotal, address, shippingTotal, shippingDate, now)
          )

          val header = TransactionLogHeader.list()
          header.size === 1
          header(0).userId === user.id.get
          header(0).transactionTime === now
          header(0).currencyId === CurrencyInfo.Jpy.id
          header(0).totalAmount === BigDecimal(999 * 2 + 123)
          header(0).taxAmount === BigDecimal((999 * 2 + 123) * 5 / 105)
          header(0).transactionType === TransactionType.NORMAL

          val siteLog = TransactionLogSite.list()
          siteLog.size === 1
          siteLog(0).transactionId === header(0).id.get
          siteLog(0).siteId === site.id.get
          siteLog(0).totalAmount === BigDecimal(999 * 2 + 123)
          siteLog(0).taxAmount === BigDecimal((999 * 2 + 123) * 5 / 105)

          val itemLog = TransactionLogItem.list()
          itemLog.size === 1
          itemLog(0).transactionSiteId === siteLog(0).id.get
          itemLog(0).itemId === item.id.get.id
          itemLog(0).itemPriceHistoryId === itemPriceHistory.id.get
          itemLog(0).quantity === 2
          itemLog(0).amount === BigDecimal(999) * 2
          itemLog(0).costPrice === BigDecimal(900)
          itemLog(0).taxId === tax.id.get

          val couponLog = TransactionLogCoupon.list(user.id.get)
          couponLog.size === 1
          couponLog(0).tranHeaderId === header(0).id.get
          couponLog(0).site === site
          couponLog(0).time === now
          couponLog(0).couponId === coupon.id.get

          val shippingLog = TransactionLogShipping.list()
          shippingLog.size === 1
          shippingLog(0).transactionSiteId === siteLog(0).id.get
          shippingLog(0).amount === BigDecimal(123)
          shippingLog(0).addressId === address.id.get
          shippingLog(0).itemClass === itemClass
          shippingLog(0).boxSize === 3
          shippingLog(0).taxId === tax.id.get
          shippingLog(0).shippingDate === date("2013-02-03").getTime

          val taxLog = TransactionLogTax.list()
          taxLog.size === 1
          taxLog(0).transactionSiteId === siteLog(0).id.get
          taxLog(0).taxId === tax.id.get
          taxLog(0).taxType === TaxType.INNER_TAX
          taxLog(0).rate === BigDecimal(5)
          taxLog(0).targetAmount === BigDecimal(999 * 2 + 123)
          taxLog(0).amount === BigDecimal((999 * 2 + 123) * 5 / 105)

        }
      }      
    }
  }
}
