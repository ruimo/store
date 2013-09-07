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

class SiteSpec extends Specification {
  "Site" should {
    "Can create new site." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
        val site2 = Site.createNew(LocaleInfo.En, "Shop2")

        val list = Site.listByName()
        list.size === 2
        list(0).name === "Shop2"
        list(1).name === "商店1"
      }      
    }
  }
}
