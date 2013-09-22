package functional

import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable._
import play.api.Play.current
import org.specs2.mock._
import play.api.mvc.Session
import controllers.{Admin, NeedLogin}

class NeedLoginSpec extends Specification {
  "NeedLogin" should {
    "Can get login session." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        val now = 234L

        val req = FakeRequest().withSession((Admin.LoginUserKey, "123;234"))
        Admin.loginSessionWithTime(req, now) === Some(Admin.LoginSession(123L, 234L))
      }
    }

    "Login session expired." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        val now = 234L

        val req = FakeRequest().withSession((Admin.LoginUserKey, "123;233"))
        Admin.loginSessionWithTime(req, now) === None
      }
    }
  }
}
