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

        DB.withConnection { implicit conn => {
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.En, "Shop2")

          val list = Site.listByName()
          list.size === 2
          list(0).name === "Shop2"
          list(1).name === "商店1"
        }}
      }      
    }

    "Can create dropdown items." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => {
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.En, "Shop2")
          
          val list = Site.tableForDropDown
          list.size === 2
          list(0)._1 === site2.id.get.toString
          list(0)._2 === site2.name

          list(1)._1 === site1.id.get.toString
          list(1)._2 === site1.name
        }}        
      }      
    }

    "Can retrieve record by id." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => {
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")

          site1 === Site(site1.id.get)
        }}
      }
    }
  }
}
