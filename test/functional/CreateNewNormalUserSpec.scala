package functional

import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable._
import play.api.i18n.{Lang, Messages}
import models.{UserRole, StoreUser}
import play.api.Play
import play.api.Play.current
import play.api.db.DB
import helpers.Helper._

class CreateNewNormalUserSpec extends Specification {
  "CreateNewNormalUser" should {
    "Can create new user" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        browser.goTo("http://localhost:3333" +
                     controllers.routes.UserMaintenance.startCreateNewNormalUser.url + "?lang=" + lang.code)
        
        browser.title === Messages("createNormalUserTitle")
        browser.fill("#userName").`with`("username")
        browser.fill("#firstName").`with`("firstname")
        browser.fill("#lastName").`with`("lastname")
        browser.fill("#companyName").`with`("companyname")
        browser.fill("#email").`with`("ruimo@ruimo.com")
        browser.fill("#password_main").`with`("12345678")
        browser.fill("#lastName").`with`("lastName")
        browser.fill("#email").`with`("ruimo")
        browser.fill("#password_main").`with`("password")
        browser.fill("#password_confirm").`with`("password")

        browser.submit("#registerNormalUser")
        // Waiting next normal user to create.
        browser.title === Messages("createNormalUserTitle")

        browser.$(".globalErrorMessage").getText === Messages("inputError")
        browser.$("#email_field dd.error").getText === Messages("error.email")
      }}
    }

    "Confirmation password does not match." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        browser.goTo("http://localhost:3333" +
                     controllers.routes.UserMaintenance.startCreateNewNormalUser.url + "?lang=" + lang.code)

        browser.title === Messages("createNormalUserTitle")
        browser.fill("#userName").`with`("username")
        browser.fill("#firstName").`with`("firstname")
        browser.fill("#lastName").`with`("lastname")
        browser.fill("#companyName").`with`("companyname")
        browser.fill("#email").`with`("ruimo@ruimo.com")
        browser.fill("#password_main").`with`("12345678")
        browser.fill("#password_confirm").`with`("12345679")
        browser.submit("#registerNormalUser")

        // Waiting next normal user to create.
        browser.title === Messages("createNormalUserTitle")

        browser.$(".globalErrorMessage").getText === Messages("inputError")
        browser.$("#password_confirm_field dd.error").getText === Messages("confirmPasswordDoesNotMatch")
      }
    }
  }
}
