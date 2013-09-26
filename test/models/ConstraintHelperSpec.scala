package models

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current

class ConstraintHelperSpec extends Specification {
  "ConstraintHelper" should {
    "be able to retrieve column size (8 for LOCALE.LANG) " in {
       running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()
        val x = ConstraintHelper.getColumnSize(null,"LOCALE","LANG")
        //println(x)
        x == 8
      }
    }
  }
}