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
import play.api.i18n.Lang

class TaxSpec extends Specification {
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
  }
}

