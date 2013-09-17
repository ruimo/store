package functional

import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable._
import play.api.i18n.{Lang, Messages}
import models.{UserRole, StoreUser}
import play.api.db.DB
import play.api.Play.current

class LoginSpec extends Specification {
  // password == password
  def createTestUser() = DB.withConnection { implicit conn => {
    StoreUser.create(
      "administrator", "Admin", None, "Manager", "admin@abc.com",
      4151208325021896473L, -1106301469931443100L, UserRole.ADMIN
    )
  }}

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

        browser.submit("#loginButton")
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
        browser.submit("#loginButton")
        browser.title === Messages("adminTitle")
      }
    }
  }
}
