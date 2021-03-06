package models

import org.specs2.mutable._

import com.ruimo.scoins.Scoping._
import anorm._
import anorm.SqlParser
import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current
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
          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )
          
          implicit val login = LoginSession(user1, None, 0L)
          val list = Site.tableForDropDown
          list.size === 2
          list(0)._1 === site2.id.get.toString
          list(0)._2 === site2.name

          list(1)._1 === site1.id.get.toString
          list(1)._2 === site1.name
        }}        
      }      
    }

    "Can create dropdown items for site owner." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => {
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.En, "Shop2")
          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )
          val siteUser = SiteUser.createNew(user1.id.get, site1.id.get)
          
          implicit val login = LoginSession(user1, Some(siteUser), 0L)
          val list = Site.tableForDropDown
          list.size === 1
          list(0)._1 === site1.id.get.toString
          list(0)._2 === site1.name
        }}        
      }      
    }

    "Can create dropdown by item." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => {
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.En, "Shop2")
          val site3 = Site.createNew(LocaleInfo.Ja, "商店3")
          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )
          val cat1 = Category.createNew(
            Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
          )
          val item1 = Item.createNew(cat1)
          SiteItem.createNew(site1, item1)
          SiteItem.createNew(site2, item1)
          
          implicit val login = LoginSession(user1, None, 0L)
          val list = Site.tableForDropDown(item1.id.get.id)
          list.size === 2
          list(0)._1 === site2.id.get.toString
          list(0)._2 === site2.name

          list(1)._1 === site1.id.get.toString
          list(1)._2 === site1.name
        }}        
      }      
    }

    "Can create dropdown by item for site owner." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => {
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.En, "Shop2")
          val site3 = Site.createNew(LocaleInfo.Ja, "商店3")
          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )
          val siteUser = SiteUser.createNew(user1.id.get, site1.id.get)
          val cat1 = Category.createNew(
            Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
          )
          val item1 = Item.createNew(cat1)
          SiteItem.createNew(site1, item1)
          SiteItem.createNew(site2, item1)
          
          implicit val login = LoginSession(user1, Some(siteUser), 0L)
          val list = Site.tableForDropDown(item1.id.get.id)
          list.size === 1
          list(0)._1 === site1.id.get.toString
          list(0)._2 === site1.name
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

    "Deleted record should be omitted from results." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => {
          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )
          implicit val login = LoginSession(user1, None, 0L)
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val cat1 = Category.createNew(
            Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
          )
          val item1 = Item.createNew(cat1)
          SiteItem.createNew(site1, item1)

          Site.listByName().size === 1
          Site.tableForDropDown.size === 1
          Site.tableForDropDown(item1.id.get.id).size === 1
          Site.listAsMap.size === 1
          Site.get(site1.id.get) === Some(site1)
          Site(site1.id.get) === site1

          Site.delete(site1.id.get)

          Site.listByName().size === 0
          Site.tableForDropDown.size === 0
          Site.tableForDropDown(item1.id.get.id).size === 0
          Site.listAsMap.size === 0
          Site.get(site1.id.get) === None
          Site(site1.id.get) must throwA[RuntimeException]
        }}
      }
    }

    "Can update record" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => {
          val site = Site.createNew(LocaleInfo.Ja, "商店1")
          doWith(Site.listByName()) { list =>
            list.size === 1
            list(0).localeId === LocaleInfo.Ja.id
            list(0).name === "商店1"
          }

          Site.update(site.id.get, LocaleInfo.En, "Shop1")
          doWith(Site.listByName()) { list =>
            list.size === 1
            list(0).localeId === LocaleInfo.En.id
            list(0).name === "Shop1"
          }
        }}
      }      
    }
  }
}
