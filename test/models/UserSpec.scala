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
          "userName", "firstName", "lastName", "email",
          1L, 2L, UserRole.ADMIN
        )
        StoreUser.count === 1
      }
    }
  }
}
