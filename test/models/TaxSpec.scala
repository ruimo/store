package models

import org.specs2.mutable._

import anorm._
import anorm.SqlParser
import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current
import java.util.Locale
import play.api.i18n.Lang
import java.sql.Date.{valueOf => date}

class TaxSpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  "Tax" should {
    "Create new tax." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()
        DB.withConnection { implicit conn => {
          val tax1 = Tax.createNew
          val tax2 = Tax.createNew

          val list = Tax.list
          list.size === 2
          list(0) === tax1
          list(1) === tax2
        }}
      }
    }

    "Create name." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()
        DB.withConnection { implicit conn => {
          val tax1 = Tax.createNew
          val tax2 = Tax.createNew

          val taxName1 = TaxName.createNew(tax1, LocaleInfo.Ja, "外税")
          val taxName2 = TaxName.createNew(tax2, LocaleInfo.Ja, "内税")

          val list = Tax.tableForDropDown(lang = Lang("ja"), conn = implicitly)
          list.size === 2
          list(0)._1 === tax2.id.get.toString
          list(0)._2 === taxName2.taxName
          list(1)._1 === tax1.id.get.toString
          list(1)._2 === taxName1.taxName
        }}
      }
    }

    "apply." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()
        DB.withConnection { implicit conn => {
          val tax1 = Tax.createNew
          val tax2 = Tax.createNew

          tax1 === Tax(tax1.id.get)
          tax2 === Tax(tax2.id.get)
        }}
      }
    }

    "at." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()
        DB.withConnection { implicit conn =>
          val tax1 = Tax.createNew
          val his1 = TaxHistory.createNew(tax1, TaxType.INNER_TAX, BigDecimal("5"), date("2013-01-02"))
          val his2 = TaxHistory.createNew(tax1, TaxType.INNER_TAX, BigDecimal("10"), date("9999-12-31"))
          
          TaxHistory.at(tax1.id.get, date("2013-01-01")) === his1                         
          TaxHistory.at(tax1.id.get, date("2013-01-02")) === his2
        }
      }
    }

    "Outer tax amount is calculated." in {
      val his = TaxHistory(None, 0, TaxType.OUTER_TAX, BigDecimal(5), 0)
      his.taxAmount(BigDecimal(100)) === BigDecimal(5)
      his.taxAmount(BigDecimal(99)) === BigDecimal(4)
      his.taxAmount(BigDecimal(80)) === BigDecimal(4)
      his.taxAmount(BigDecimal(79)) === BigDecimal(3)
    }

    "Innter tax amount is calculated." in {
      val his = TaxHistory(None, 0, TaxType.INNER_TAX, BigDecimal(5), 0)
      his.taxAmount(BigDecimal(100)) === BigDecimal(4)
      his.taxAmount(BigDecimal(84)) === BigDecimal(4)
      his.taxAmount(BigDecimal(83)) === BigDecimal(3)
    }

    "Non tax amount is calculated." in {
      val his = TaxHistory(None, 0, TaxType.NON_TAX, BigDecimal(5), 0)
      his.taxAmount(BigDecimal(100)) === BigDecimal(0)
      his.taxAmount(BigDecimal(84)) === BigDecimal(0)
      his.taxAmount(BigDecimal(83)) === BigDecimal(0)
    }
  }
}

