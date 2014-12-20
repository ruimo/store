package models

import org.specs2.mutable._

import anorm._
import anorm.SqlParser
import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current

class ResetPasswordSpec extends Specification {
  "ResetPassword" should {
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

          val rec = ResetPassword.createNew(
            storeUserId = user.id.get,
            now = 12345L,
            token = 45678L
          )
          val readRec = ResetPassword(rec.id.get)

          readRec.storeUserId === user.id.get
          readRec.resetTime === 12345L
          readRec.token === 45678L
        }}
      }      
    }

    "Can remove records" in {
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

          val rec = ResetPassword.createNew(
            storeUserId = user.id.get,
            now = 12345L,
            token = 45678L
          )

          rec === ResetPassword.get(rec.id.get).get

          ResetPassword.removeByStoreUserId(user.id.get) === 1L
          ResetPassword.get(rec.id.get) === None
        }}
      }
    }

    "Can determine valid record" in {
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

          val rec = ResetPassword.createNew(
            storeUserId = user.id.get,
            now = 12345L,
            token = 45678L
          )

          // token does not match
          ResetPassword.isValid(user.id.get, 45679L, 12344L) === false

          // userid does not match
          ResetPassword.isValid(user.id.get + 1, 45678L, 12344L) === false

          // record is too old
          ResetPassword.isValid(user.id.get, 45678L, 12345L) === false

          // valid record
          ResetPassword.isValid(user.id.get, 45678L, 12344L) === true
        }}
      }
    }
  }
}
