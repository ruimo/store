package functional

import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable._
import play.api.i18n.{Lang, Messages}
import models.{UserRole, StoreUser}
import play.api.db.DB
import play.api.Play.current
import java.util.concurrent.TimeUnit
import functional.Helper._

class LoginSpec extends Specification {
  "Login" should {
    "Login screen is shown if not logged in." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser =>
        val user = createTestUser()
        implicit val lang = Lang("ja")
        browser.goTo("http://localhost:3333" + controllers.routes.Admin.index.url + "?lang=" + lang.code)
        browser.title === Messages("loginTitle")
      }
    }

    "Login empty error." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser =>
        val user = createTestUser()
        implicit val lang = Lang("ja")
        browser.goTo("http://localhost:3333" + controllers.routes.Admin.index.url + "?lang=" + lang.code)
        browser.title === Messages("loginTitle")

        browser.$("#doLoginButton").click()
        browser.title === Messages("loginTitle")

        browser.$(".globalErrorMessage").getText === Messages("inputError")
        browser.$("#userName_field dd.error").getText === Messages("error.required")
        browser.$("#password_field dd.error").getText === Messages("error.required")
      }
    }

    "Login success." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser =>
        val user = createTestUser()
        implicit val lang = Lang("ja")
        browser.goTo("http://localhost:3333" + controllers.routes.Admin.index.url + "?lang=" + lang.code)
        browser.title === Messages("loginTitle")

        browser.fill("#userName").`with`("administrator")
        browser.fill("#password").`with`("password")
        browser.click("#doLoginButton")
        browser.title === Messages("adminTitle")
      }
    }
  }
}
