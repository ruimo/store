package models

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current

class CurrencyInfoSpec extends Specification {
  "CurrencyInfo" should {
    "Japan and English locale" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        CurrencyInfo.Jpy === CurrencyInfo(1L, "JPY")
        CurrencyInfo.Usd === CurrencyInfo(2L, "USD")
      }
    }

    "Dropdown items" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          val list = CurrencyInfo.tableForDropDown
          list(0)._1 == CurrencyInfo.Jpy.id
          list(0)._2 == CurrencyInfo.Jpy.currencyCode
          list(1)._1 == CurrencyInfo.Usd.id
          list(1)._2 == CurrencyInfo.Usd.currencyCode
        }}
      }
    }
  }
}
