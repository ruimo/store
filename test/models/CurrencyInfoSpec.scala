package models

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

class CurrencyInfoSpec extends Specification {
  "CurrencyInfo" should {
    "Japan and English locale" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        CurrencyInfo.Jpy === CurrencyInfo(1L, "JPY")
        CurrencyInfo.Usd === CurrencyInfo(2L, "USD")
      }
    }
  }
}
