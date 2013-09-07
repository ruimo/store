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

class TaxSpec extends Specification {
  "Tax" should {
    "Create new tax." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        val tax1 = Tax.createNew()
        val tax2 = Tax.createNew()

        val list = Tax.list
        list.size === 2
        list(0) === tax1
        list(1) === tax2
      }
    }
  }
}

