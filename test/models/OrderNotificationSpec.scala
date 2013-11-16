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
  }
}
