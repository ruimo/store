package functional

import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable._
import play.api.i18n.Messages

class FirstSetupSpec extends Specification {
  "FirstSetup" should {
    "First setup screen is shown if no user found." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser =>
        browser.goTo("http://localhost:3333/admin")
        browser.title === Messages("firstSetupTitle")
      }
    }

    "First setup create user." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser =>
        browser.goTo("http://localhost:3333/admin")
        browser.title === Messages("firstSetupTitle")
        browser.fill("#userName").`with`("username")
        browser.fill("#firstName").`with`("firstname")
        browser.fill("#lastName").`with`("lastname")
        browser.fill("#email").`with`("ruimo@ruimo.com")
        browser.fill("#password_main").`with`("12345678")
        browser.fill("#password_confirm").`with`("12345678")

        browser.submit("input[type='submit']")
        browser.title === Messages("loginTitle")
      }
    }
  }
}

