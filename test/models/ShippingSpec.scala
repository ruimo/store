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
import java.util.Locale
import java.sql.Date.{valueOf => date}

class ShippingSpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  "Shipping" should {
    "Can create shipping fee record." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          TestHelper.removePreloadedRecords()

          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val itemClass1 = 1L

          val box = ShippingBox.createNew(
            site1.id.get, itemClass1, 10, "小箱"
          )
          val shipping = ShippingFee.createNew(
            box.id.get, CountryCode.JPN, JapanPrefecture.東京都.code
          )

          ShippingFee(box.id.get, CountryCode.JPN, JapanPrefecture.東京都.code) === shipping
        }
      }
    }

    "Can create shipping fee history." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          TestHelper.removePreloadedRecords()

          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val itemClass1 = 1L

          val box = ShippingBox.createNew(
            site1.id.get, itemClass1, 10, "小箱"
          )
          val shipping = ShippingFee.createNew(
            box.id.get, CountryCode.JPN, JapanPrefecture.東京都.code
          )
          val history1 = ShippingFeeHistory.createNew(
            shipping.id.get, BigDecimal(1234), date("9999-12-31")
          )

          val list = ShippingFeeHistory.list(shipping.id.get)
          list.size === 1
          list(0) === history1

          ShippingFeeHistory.at(shipping.id.get).fee === BigDecimal(1234)
          
          val history2 = ShippingFeeHistory.createNew(
            shipping.id.get, BigDecimal(2345), date("2013-12-31")
          )

          val list2 = ShippingFeeHistory.list(shipping.id.get)
          list2.size === 2
          list2(0) === history2
          list2(1) === history1

          ShippingFeeHistory.at(shipping.id.get, date("2013-12-30")).fee === BigDecimal(2345)
          ShippingFeeHistory.at(shipping.id.get, date("2013-12-31")).fee === BigDecimal(1234)
        }
      }
    }

    "Can list by country and location." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          TestHelper.removePreloadedRecords()

          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val itemClass1 = 1L

          val box = ShippingBox.createNew(
            site1.id.get, itemClass1, 10, "小箱"
          )
          val shipping1 = ShippingFee.createNew(
            box.id.get, CountryCode.JPN, JapanPrefecture.東京都.code
          )
          val history1 = ShippingFeeHistory.createNew(
            shipping1.id.get, BigDecimal(1234), date("9999-12-31")
          )
          val history2 = ShippingFeeHistory.createNew(
            shipping1.id.get, BigDecimal(2345), date("2013-12-31")
          )

          val shipping2 = ShippingFee.createNew(
            box.id.get, CountryCode.JPN, JapanPrefecture.埼玉県.code
          )
          val history3 = ShippingFeeHistory.createNew(
            shipping2.id.get, BigDecimal(9999), date("9999-12-31")
          )
          val history4 = ShippingFeeHistory.createNew(
            shipping2.id.get, BigDecimal(8888), date("2013-12-31")
          )

          val list = ShippingFeeHistory.listByLocation(CountryCode.JPN, JapanPrefecture.東京都.code)
          list.size === 2
          list(0) === (shipping1, history2)
          list(1) === (shipping1, history1)

          val list2 = ShippingFeeHistory.listByLocation(CountryCode.JPN, JapanPrefecture.埼玉県.code)
          list2.size === 2
          list2(0) === (shipping2, history4)
          list2(1) === (shipping2, history3)

          ShippingFeeHistory.listByLocation(CountryCode.JPN, JapanPrefecture.三重県.code).size === 0
        }
      }
    }
  }
}

