package models

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current
import anorm.Id

class UserSpec extends Specification {
  "User" should {
    "User count should be zero when table is empty" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        StoreUser.count === 0
      }
    }

    "User count should reflect the number of records in the table" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        StoreUser.create(
          "userName", "firstName", Some("middleName"), "lastName", "email",
          1L, 2L, UserRole.ADMIN
        )
        StoreUser.count === 1
      }
    }

    "User can be queried by username" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        val user1 = StoreUser.create(
          "userName", "firstName", Some("middleName"), "lastName", "email",
          1L, 2L, UserRole.ADMIN
        )

        val user2 = StoreUser.create(
          "userName2", "firstName2", None, "lastName2", "email2",
          1L, 2L, UserRole.ADMIN
        )

        StoreUser.findByUserName("userName").get === user1
        StoreUser.findByUserName("userName2").get === user2
      }
    }
  }
}

