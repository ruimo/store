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

class TransactionSpec extends Specification {
  "Transaction" should {
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

          val header = TransactionHeader.createNew(
            user1.id.get, currency1.id,
            BigDecimal(234), BigDecimal(345),
            TransactionType.NORMAL
          )

          val list = TransactionHeader.list()
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

          val header = TransactionHeader.createNew(
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

          val tranSite = TransactionSite.createNew(
            header.id.get, site1.id.get, BigDecimal(234), BigDecimal(345)
          )

          val shipping = TransactionShipping.createNew(
            tranSite.id.get, BigDecimal(9876), addr1.id.get
          )

          val list = TransactionShipping.list()
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

          val header = TransactionHeader.createNew(
            user1.id.get, currency1.id,
            BigDecimal(234), BigDecimal(345),
            TransactionType.NORMAL
          )
          
          val tranSite = TransactionSite.createNew(
            header.id.get, site1.id.get, BigDecimal(234), BigDecimal(345)
          )

          val tax = TransactionTax.createNew(
            tranSite.id.get,
            1234L, 2345L, TaxType.INNER_TAX,
            BigDecimal(5), BigDecimal(333), BigDecimal(222)
          )

          val list = TransactionTax.list()
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

          val header = TransactionHeader.createNew(
            user1.id.get, currency1.id,
            BigDecimal(234), BigDecimal(345),
            TransactionType.NORMAL
          )

          val tranSite = TransactionSite.createNew(
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

          val shipping = TransactionShipping.createNew(
            tranSite.id.get, BigDecimal(9876), addr1.id.get
          )

          val item = TransactionItem.createNew(
            header.id.get, 1234L, shipping.id.get, 234L, BigDecimal(456)
          )

          val list = TransactionItem.list()
          list.size === 1
          list(0) === item
        }
      }
    }
  }
}

