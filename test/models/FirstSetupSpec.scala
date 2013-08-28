package models

import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable._
import helpers.TokenGenerator

class FirstSetupSpec extends Specification {
  "FirstSetup" should {
    "Salt and hash is created by create() method." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        val user = FirstSetup("userName", "firstName", Some("middleName"), "lastName", "email", "password").save(
          new TokenGenerator {
            def next = 0x1234567890123456L
          }
        )

        StoreUser.find(user.id.get) === user
      }
    }
  }
}
