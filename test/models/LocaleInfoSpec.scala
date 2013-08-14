package models

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current
import anorm.Id
import java.util.Locale

class LocaleInfoSpec extends Specification {
  "LocaleInfo" should {
    "Japaness locale should exists." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        LocaleInfo(1L).toLocale === new Locale("ja")
      }
    }
  }
}
