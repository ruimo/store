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
          val tax = Tax.createNew
          val taxHistory = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

          val box = ShippingBox.createNew(
            site1.id.get, itemClass1, 10, "小箱"
          )
          val shipping = ShippingFee.createNew(
            box.id.get, CountryCode.JPN, JapanPrefecture.東京都.code
          )
          val history1 = ShippingFeeHistory.createNew(
            shipping.id.get, tax.id.get, BigDecimal(1234), date("9999-12-31")
          )

          val list = ShippingFeeHistory.list(shipping.id.get)
          list.size === 1
          list(0) === history1

          ShippingFeeHistory.at(shipping.id.get).fee === BigDecimal(1234)
          
          val history2 = ShippingFeeHistory.createNew(
            shipping.id.get, tax.id.get, BigDecimal(2345), date("2013-12-31")
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

          val tax = Tax.createNew
          val taxHistory = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

          val box = ShippingBox.createNew(
            site1.id.get, itemClass1, 10, "小箱"
          )
          val shipping1 = ShippingFee.createNew(
            box.id.get, CountryCode.JPN, JapanPrefecture.東京都.code
          )
          val history1 = ShippingFeeHistory.createNew(
            shipping1.id.get, tax.id.get, BigDecimal(1234), date("9999-12-31")
          )
          val history2 = ShippingFeeHistory.createNew(
            shipping1.id.get, tax.id.get, BigDecimal(2345), date("2013-12-31")
          )

          val shipping2 = ShippingFee.createNew(
            box.id.get, CountryCode.JPN, JapanPrefecture.埼玉県.code
          )
          val history3 = ShippingFeeHistory.createNew(
            shipping2.id.get, tax.id.get, BigDecimal(9999), date("9999-12-31")
          )
          val history4 = ShippingFeeHistory.createNew(
            shipping2.id.get, tax.id.get, BigDecimal(8888), date("2013-12-31")
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

    "Can list by country and location." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          TestHelper.removePreloadedRecords()

          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.Ja, "商店2")
          val itemClass1 = 1L
          val itemClass2 = 2L

          val tax = Tax.createNew
          val taxHistory = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

          val box1_1 = ShippingBox.createNew(site1.id.get, itemClass1, 10, "小箱1")
          val box1_2 = ShippingBox.createNew(site1.id.get, itemClass2, 5, "中箱1")

          val box2_1 = ShippingBox.createNew(site2.id.get, itemClass1, 7, "小箱2")
          val box2_2 = ShippingBox.createNew(site2.id.get, itemClass2, 3, "中箱2")

          val shipping1_1_tokyo = ShippingFee.createNew(box1_1.id.get, CountryCode.JPN, JapanPrefecture.東京都.code)
          val shipping1_1_saitama = ShippingFee.createNew(box1_1.id.get, CountryCode.JPN, JapanPrefecture.埼玉県.code)
          val shipping1_2_tokyo = ShippingFee.createNew(box1_2.id.get, CountryCode.JPN, JapanPrefecture.東京都.code)
          val shipping1_2_saitama = ShippingFee.createNew(box1_2.id.get, CountryCode.JPN, JapanPrefecture.埼玉県.code)

          val shipping2_1_tokyo = ShippingFee.createNew(box2_1.id.get, CountryCode.JPN, JapanPrefecture.東京都.code)
          val shipping2_1_saitama = ShippingFee.createNew(box2_1.id.get, CountryCode.JPN, JapanPrefecture.埼玉県.code)
          val shipping2_2_tokyo = ShippingFee.createNew(box2_2.id.get, CountryCode.JPN, JapanPrefecture.東京都.code)
          val shipping2_2_saitama = ShippingFee.createNew(box2_2.id.get, CountryCode.JPN, JapanPrefecture.埼玉県.code)

          val history1_1_tokyo_1 = ShippingFeeHistory.createNew(
            shipping1_1_tokyo.id.get, tax.id.get, BigDecimal(1234), date("9999-12-31")
          )
          val history1_1_tokyo_2 = ShippingFeeHistory.createNew(
            shipping1_1_tokyo.id.get, tax.id.get, BigDecimal(1000), date("2013-12-31")
          )

          val history1_1_saitama_1 = ShippingFeeHistory.createNew(
            shipping1_1_saitama.id.get, tax.id.get, BigDecimal(12), date("9999-12-31")
          )
          val history1_1_saitama_2 = ShippingFeeHistory.createNew(
            shipping1_1_saitama.id.get, tax.id.get, BigDecimal(34), date("2013-12-31")
          )

          val history1_2_tokyo_1 = ShippingFeeHistory.createNew(
            shipping1_2_tokyo.id.get, tax.id.get, BigDecimal(234), date("9999-12-31")
          )
          val history1_2_tokyo_2 = ShippingFeeHistory.createNew(
            shipping1_2_tokyo.id.get, tax.id.get, BigDecimal(345), date("2013-12-31")
          )
          
          val history1_2_saitama_1 = ShippingFeeHistory.createNew(
            shipping1_2_saitama.id.get, tax.id.get, BigDecimal(112), date("9999-12-31")
          )
          val history1_2_saitama_2 = ShippingFeeHistory.createNew(
            shipping1_2_saitama.id.get, tax.id.get, BigDecimal(221), date("2013-12-31")
          )

          val history2_1_tokyo_1 = ShippingFeeHistory.createNew(
            shipping2_1_tokyo.id.get, tax.id.get, BigDecimal(222), date("9999-12-31")
          )
          val history2_1_tokyo_2 = ShippingFeeHistory.createNew(
            shipping2_1_tokyo.id.get, tax.id.get, BigDecimal(333), date("2013-12-31")
          )

          val history2_1_saitama_1 = ShippingFeeHistory.createNew(
            shipping2_1_saitama.id.get, tax.id.get, BigDecimal(999), date("9999-12-31")
          )
          val history2_1_saitama_2 = ShippingFeeHistory.createNew(
            shipping2_1_saitama.id.get, tax.id.get, BigDecimal(888), date("2013-12-31")
          )

          val history2_2_tokyo_1 = ShippingFeeHistory.createNew(
            shipping2_2_tokyo.id.get, tax.id.get, BigDecimal(9999), date("9999-12-31")
          )
          val history2_2_tokyo_2 = ShippingFeeHistory.createNew(
            shipping2_2_tokyo.id.get, tax.id.get, BigDecimal(8888), date("2013-12-31")
          )

          val history2_2_saitama_1 = ShippingFeeHistory.createNew(
            shipping2_2_saitama.id.get, tax.id.get, BigDecimal(7777), date("9999-12-31")
          )
          val history2_2_saitama_2 = ShippingFeeHistory.createNew(
            shipping2_2_saitama.id.get, tax.id.get, BigDecimal(6666), date("2013-12-31")
          )

          val map1 = ShippingFeeHistory.feeBySiteAndItemClass(
            CountryCode.JPN, JapanPrefecture.東京都.code, ShippingFeeEntries()
            .add(site1.id.get, itemClass1, 7)
            .add(site1.id.get, itemClass1, 8)
            .add(site1.id.get, itemClass1, 4),
            date("2013-12-30").getTime
          ).table

          map1.size === 1
          map1(site1).size === 1
          val info1 = map1(site1)(itemClass1)
          info1.shippingBox.boxName === "小箱1"
          info1.itemQuantity === 19
          info1.boxQuantity === 2
          info1.boxPrice === history1_1_tokyo_2.fee

          val map2 = ShippingFeeHistory.feeBySiteAndItemClass(
            CountryCode.JPN, JapanPrefecture.東京都.code, ShippingFeeEntries()
            .add(site1.id.get, itemClass1, 7)
            .add(site1.id.get, itemClass1, 8)
            .add(site1.id.get, itemClass1, 4)
            .add(site1.id.get, itemClass2, 4),
            date("2013-12-31").getTime
          ).table

          map2.size === 1
          map2(site1).size === 2
          val info2 = map2(site1)(itemClass2)
          info2.shippingBox.boxName === "中箱1"
          info2.itemQuantity === 4
          info2.boxQuantity === 1
          info2.boxPrice === history1_2_tokyo_1.fee

          val map3 = ShippingFeeHistory.feeBySiteAndItemClass(
            CountryCode.JPN, JapanPrefecture.埼玉県.code, ShippingFeeEntries()
            .add(site1.id.get, itemClass1, 7)
            .add(site1.id.get, itemClass1, 8)
            .add(site1.id.get, itemClass1, 4)
            .add(site1.id.get, itemClass2, 4)
            .add(site2.id.get, itemClass1, 20)
            .add(site2.id.get, itemClass1, 1)
            .add(site2.id.get, itemClass2, 6),
            date("2013-12-31").getTime
          ).table

          map3.size === 2
          map3(site1).size === 2
          val info3 = map3(site1)(itemClass1)
          info3.shippingBox.boxName === "小箱1"
          info3.itemQuantity === 19
          info3.boxQuantity === 2
          info3.boxPrice === history1_1_saitama_1.fee

          val info4 = map3(site1)(itemClass2)
          info4.shippingBox.boxName === "中箱1"
          info4.itemQuantity === 4
          info4.boxQuantity === 1
          info4.boxPrice === history1_2_saitama_1.fee

          map3(site2).size === 2
          val info5 = map3(site2)(itemClass1)
          info5.shippingBox.boxName === "小箱2"
          info5.itemQuantity === 21
          info5.boxQuantity === 3
          info5.boxPrice === history2_1_saitama_1.fee

          val info6 = map3(site2)(itemClass2)
          info6.shippingBox.boxName === "中箱2"
          info6.itemQuantity === 6
          info6.boxQuantity === 2
          info6.boxPrice === history2_2_saitama_1.fee
        }
      }
    }
  }
}

