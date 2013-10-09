package models

import org.specs2.mutable._

import anorm._
import anorm.{NotAssigned, Pk}
import anorm.SqlParser
import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current
import anorm.Id
import java.sql.Date.{valueOf => date}

class TransactionSpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  "TransactionLog" should {
    "Can persist header." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          TestHelper.removePreloadedRecords()
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN
          )
          val currency1 = CurrencyInfo.Jpy
          val now = 1234L

          val header = TransactionLogHeader.createNew(
            user1.id.get, currency1.id,
            BigDecimal(234), BigDecimal(345),
            TransactionType.NORMAL
          )

          val list = TransactionLogHeader.list()
          list.size === 1
          header === list(0)
        }
      }
    }

    "Can persist shipping." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          TestHelper.removePreloadedRecords()
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN
          )
          val currency1 = CurrencyInfo.Jpy
          val now = 1234L

          val header = TransactionLogHeader.createNew(
            user1.id.get, currency1.id,
            BigDecimal(234), BigDecimal(345),
            TransactionType.NORMAL
          )

          val addr1 = Address.createNew(
            countryCode = CountryCode.JPN,
            firstName = "FirstName",
            lastName = "LastName",
            zip1 = "123",
            prefecture = JapanPrefecture.東京都,
            address1 = "Address1",
            address2 = "Address2",
            tel1 = "12345678"
          )

          val tranSite = TransactionLogSite.createNew(
            header.id.get, site1.id.get, BigDecimal(234), BigDecimal(345)
          )

          val shipping = TransactionLogShipping.createNew(
            tranSite.id.get, BigDecimal(9876), addr1.id.get, 1L, 1, 1L
          )

          val list = TransactionLogShipping.list()
          list.size === 1
          list(0) === shipping
        }
      }
    }

    "Can persist tax." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          TestHelper.removePreloadedRecords()

          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN
          )
          val currency1 = CurrencyInfo.Jpy
          val now = 1234L

          val header = TransactionLogHeader.createNew(
            user1.id.get, currency1.id,
            BigDecimal(234), BigDecimal(345),
            TransactionType.NORMAL
          )
          
          val tranSite = TransactionLogSite.createNew(
            header.id.get, site1.id.get, BigDecimal(234), BigDecimal(345)
          )

          val tax = TransactionLogTax.createNew(
            tranSite.id.get,
            1234L, 2345L, TaxType.INNER_TAX,
            BigDecimal(5), BigDecimal(333), BigDecimal(222)
          )

          val list = TransactionLogTax.list()
          list.size === 1
          list(0) === tax
        }
      }
    }

    "Can persist item." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          TestHelper.removePreloadedRecords()

          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN
          )
          val currency1 = CurrencyInfo.Jpy
          val now = 1234L

          val header = TransactionLogHeader.createNew(
            user1.id.get, currency1.id,
            BigDecimal(234), BigDecimal(345),
            TransactionType.NORMAL
          )

          val tranSite = TransactionLogSite.createNew(
            header.id.get, site1.id.get, BigDecimal(234), BigDecimal(345)
          )

          val addr1 = Address.createNew(
            countryCode = CountryCode.JPN,
            firstName = "FirstName",
            lastName = "LastName",
            zip1 = "123",
            prefecture = JapanPrefecture.東京都,
            address1 = "Address1",
            address2 = "Address2",
            tel1 = "12345678"
          )

          val shipping = TransactionLogShipping.createNew(
            tranSite.id.get, BigDecimal(9876), addr1.id.get, 1L, 1, 1L
          )

          val item = TransactionLogItem.createNew(
            header.id.get, 1234L, shipping.id.get, 234L, BigDecimal(456)
          )

          val list = TransactionLogItem.list()
          list.size === 1
          list(0) === item
        }
      }
    }

    "Can persist whole transaction." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          val tax1 = Tax.createNew
          val tax2 = Tax.createNew
          val taxHistory1 = TaxHistory.createNew(tax1, TaxType.OUTER_TAX, BigDecimal("5"), date("9999-12-31"))
          val taxHistory2 = TaxHistory.createNew(tax2, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

          val user1 = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL
          )
          import models.LocaleInfo.{Ja}
          val site1 = Site.createNew(Ja, "商店1")
          val site2 = Site.createNew(Ja, "商店2")

          val cat1 = Category.createNew(Map(Ja -> "植木"))

          val item1 = Item.createNew(cat1)
          val item2 = Item.createNew(cat1)

          val name1 = ItemName.createNew(item1, Map(Ja -> "杉"))
          val name2 = ItemName.createNew(item2, Map(Ja -> "梅"))

          SiteItem.createNew(site1, item1)
          SiteItem.createNew(site2, item2)

          val desc1 = ItemDescription.createNew(item1, site1, "杉説明")
          val desc2 = ItemDescription.createNew(item2, site1, "梅説明")

          val price1 = ItemPrice.createNew(item1, site1)
          val price2 = ItemPrice.createNew(item2, site2)

          val ph1 = ItemPriceHistory.createNew(price1, tax1, CurrencyInfo.Jpy, BigDecimal(119), date("9999-12-31"))
          val ph2 = ItemPriceHistory.createNew(price2, tax1, CurrencyInfo.Jpy, BigDecimal(59), date("9999-12-31"))

          val cart1 = ShoppingCartItem.addItem(user1.id.get, site1.id.get, item1.id.get, 1)
          val cart2 = ShoppingCartItem.addItem(user1.id.get, site2.id.get, item2.id.get, 1)

          val itemClass1 = 1L

          val box1 = ShippingBox.createNew(site1.id.get, itemClass1, 10, "小箱")
          val box2 = ShippingBox.createNew(site2.id.get, itemClass1, 3, "小箱")
          val shipping1 = ShippingFee.createNew(box1.id.get, CountryCode.JPN, JapanPrefecture.東京都.code)
          val shipping2 = ShippingFee.createNew(box2.id.get, CountryCode.JPN, JapanPrefecture.東京都.code)
          val shipHis1 = ShippingFeeHistory.createNew(
            shipping1.id.get, tax1.id.get, BigDecimal(1234), date("9999-12-31")
          )
          val shipHis2 = ShippingFeeHistory.createNew(
            shipping2.id.get, tax1.id.get, BigDecimal(2345), date("9999-12-31")
          )

          val shippingTotal = ShippingFeeHistory.feeBySiteAndItemClass(
            CountryCode.JPN, JapanPrefecture.東京都.code,
            ShippingFeeEntries()
              .add(site1.id.get, itemClass1, 3)
              .add(site2.id.get, itemClass1, 5)
          )


        }
      }      
    }
  }
}

