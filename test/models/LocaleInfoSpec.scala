package models

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current
import anorm.Id
import java.util.Locale

class ApplicationSpec extends Specification {
  "LocaleInfo" should {
    "Japaness locale should exists." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          LocaleInfo(1L).get === new Locale("ja")
        }
      }
    }
  }
}
