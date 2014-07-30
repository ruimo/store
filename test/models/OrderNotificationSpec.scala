package models

import org.specs2.mutable._

import anorm._
import anorm.SqlParser
import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current

class OrderNotificationSpec extends Specification {
  "OrderNotification" should {
    "Can create record" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => {
          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )

          val on1 = OrderNotification.createNew(user1.id.get)
          on1.storeUserId === user1.id.get

          val list = OrderNotification.list()
          list.size === 1
          list.head === on1
        }}
      }
    }

    "List site owner notification record" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          TestHelper.removePreloadedRecords()

          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.Ja, "商店2")

          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )
          val user2 = StoreUser.create(
            "userName2", "firstName2", Some("middleName2"), "lastName2", "email2",
            1L, 2L, UserRole.ADMIN, Some("companyName2")
          )
          val user3 = StoreUser.create(
            "userName3", "firstName3", Some("middleName3"), "lastName3", "email3",
            1L, 2L, UserRole.ADMIN, Some("companyName3")
          )
          val user4 = StoreUser.create(
            "userName4", "firstName4", Some("middleName4"), "lastName4", "email4",
            1L, 2L, UserRole.ADMIN, Some("companyName4")
          )

          val siteUser1 = SiteUser.createNew(user1.id.get, site1.id.get)
          val siteUser2 = SiteUser.createNew(user2.id.get, site1.id.get)
          val siteUser3 = SiteUser.createNew(user3.id.get, site2.id.get)

          val on1 = OrderNotification.createNew(user1.id.get)
          val on2 = OrderNotification.createNew(user3.id.get)

          val list1 = OrderNotification.listBySite(site1.id.get)          
          val list2 = OrderNotification.listBySite(site2.id.get)

          list1.size === 1
          list2.size === 1

          list1.head === user1
          list2.head === user3
        }}
      }
    }

    "List admin notification record" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          TestHelper.removePreloadedRecords()

          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")

          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )
          val user2 = StoreUser.create(
            "userName2", "firstName2", Some("middleName2"), "lastName2", "email2",
            1L, 2L, UserRole.ADMIN, Some("companyName2")
          )
          val user3 = StoreUser.create(
            "userName3", "firstName3", Some("middleName3"), "lastName3", "email3",
            1L, 2L, UserRole.ADMIN, Some("companyName3")
          )
          val user4 = StoreUser.create(
            "userName4", "firstName4", Some("middleName4"), "lastName4", "email4",
            1L, 2L, UserRole.NORMAL, Some("companyName4")
          )

          val siteUser1 = SiteUser.createNew(user1.id.get, site1.id.get)

          val on1 = OrderNotification.createNew(user1.id.get)
          val on2 = OrderNotification.createNew(user2.id.get)

          val list1 = OrderNotification.listAdmin

          list1.size === 1

          list1.head === user2
        }}
      }
    }
  }
}
