package models

import org.specs2.mutable._

import anorm._
import anorm.SqlParser
import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current
import anorm.Id

class UserAddressSpec extends Specification {
  "User address" should {
    "Can create new record" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          val user = StoreUser.create(
            userName = "uno",
            firstName = "",
            middleName = None,
            lastName = "",
            email = "",
            passwordHash = 0L,
            salt = 0L,
            userRole = UserRole.NORMAL,
            companyName = None
          )
          val address01 = Address.createNew(
            countryCode = CountryCode.JPN
          )
          val address02 = Address.createNew(
            countryCode = CountryCode.JPN
          )

          val ua1 = UserAddress.createNew(user.id.get, address01.id.get)
          val ua2 = UserAddress.createNew(user.id.get, address02.id.get)

          ua1.seq === 1
          ua2.seq === 2
        }}
      }
    }
  }
}

