package models

import org.specs2.mutable._

import anorm._
import anorm.SqlParser
import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current
import anorm.Id

class AddressSpec extends Specification {
  "Address" should {
    "Can create record." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => {
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

          addr1 === Address.byId(addr1.id.get)
        }}
      }
    }
  }

  "ShippingAddressHistory" should {
    "Hold up to HistoryMaxCount" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn =>
          val user = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL, Some("companyName")
          )

          for (i <- 0 until ShippingAddressHistory.HistoryMaxCount) {
            ShippingAddressHistory.createNew(
              user.id.get, Address(
                None,
                CountryCode.JPN,
                "firstName" + i,
                "middleName" + i,
                "lastName" + i,
                "firstNameKana" + i,
                "lastNameKana" + i,
                "zip1", "zip2", "zip3",
                JapanPrefecture.北海道,
                "address1",
                "address2",
                "address3",
                "address4",
                "address5",
                "tel1",
                "tel2",
                "tel3",
                "comment",
                "email" + i
              ), i.toLong
            )
          }

          val list1 = ShippingAddressHistory.list(user.id.get)
          list1.size === ShippingAddressHistory.HistoryMaxCount

          ShippingAddressHistory.createNew(
            user.id.get, Address(
              None,
              CountryCode.JPN,
              "firstName" + ShippingAddressHistory.HistoryMaxCount,
              "middleName",
              "lastName",
              "firstNameKana",
              "lastNameKana",
              "zip1", "zip2", "zip3",
              JapanPrefecture.北海道,
              "address1",
              "address2",
              "address3",
              "address4",
              "address5",
              "tel1",
              "tel2",
              "tel3",
              "comment",
              "email"
            ), ShippingAddressHistory.HistoryMaxCount
          )

          // Still having only max count.
          val list2 = ShippingAddressHistory.list(user.id.get)
          list2.size === ShippingAddressHistory.HistoryMaxCount

          // Oldest record should be removed.
          for (i <- 0 until ShippingAddressHistory.HistoryMaxCount) {
            list2(i).updatedTime === ShippingAddressHistory.HistoryMaxCount - i
            Address.byId(list2(i).addressId).firstName === "firstName" + (ShippingAddressHistory.HistoryMaxCount - i)
          }
          1 === 1
        }
      }
    }

    "Existing record should updated if all of the fields are the same." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn =>
          val user = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL, Some("companyName")
          )
          
          ShippingAddressHistory.createNew(
            user.id.get, Address(
              None,
              CountryCode.JPN,
              "firstName",
              "middleName",
              "lastName",
              "firstNameKana",
              "lastNameKana",
              "zip1", "zip2", "zip3",
              JapanPrefecture.北海道,
              "address1",
              "address2",
              "address3",
              "address4",
              "address5",
              "tel1",
              "tel2",
              "tel3",
              "comment",
              "email"
            ), 1L
          )

          ShippingAddressHistory.createNew(
            user.id.get, Address(
              None,
              CountryCode.JPN,
              "firstName",
              "middleName",
              "lastName",
              "firstNameKana",
              "lastNameKana",
              "zip1", "zip2", "zip3",
              JapanPrefecture.北海道,
              "address1",
              "address2",
              "address3",
              "address4",
              "address5",
              "tel1",
              "tel2",
              "tel3",
              "comment",
              "email"
            ), 2L
          )

          val list = ShippingAddressHistory.list(user.id.get)
          list.size === 1
          list(0).updatedTime === 2L
        }
      }
    }
  }
}
