package controllers

import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable._
import play.api.Play.current
import org.specs2.mock._
import play.api.mvc.Session
import controllers.{Admin, NeedLogin}
import models.{UserRole, StoreUser, TestHelper, LoginSession}
import play.api.db.DB

class NeedLoginSpec extends Specification {
  "NeedLogin" should {
    "Can get login session." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          TestHelper.removePreloadedRecords()
          val user1 = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL
          )
          
          val now = 234L
          val req = FakeRequest().withSession((Admin.LoginUserKey, user1.id.get + ";234"))
          val login = Admin.loginSessionWithTime(req, now).get
          login.storeUser.id.get === user1.id.get
          login.expireTime === 234L
        }
      }
    }

    "Login session expired." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          TestHelper.removePreloadedRecords()
          val user1 = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL
          )

          val now = 234L

          val req = FakeRequest().withSession((Admin.LoginUserKey, user1.id.get + ";233"))
          Admin.loginSessionWithTime(req, now) === None
        }
      }
    }
  }
}
