package functional

import SeleniumHelpers.FirefoxJa
import java.util.concurrent.TimeUnit
import anorm._
import play.api.test.Helpers._
import play.api.Play.current
import helpers.Helper._
import models._
import play.api.db.DB
import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.i18n.{Lang, Messages}
import LocaleInfo._
import java.sql.Connection
import scala.collection.immutable
import com.ruimo.scoins.Scoping._

class UserProfileMaintenanceSpec extends Specification {
  "User profile maintenaces" should {
    "Not logged in user cannot change profile." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val adminUser = loginWithTestUser(browser)
        logoff(browser)

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ProfileMaintenance.index() + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("commonTitle", Messages("login"))
      }}
    }

    "Logged in user can change profile." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      SeleniumHelpers.running(TestServer(3333, app), FirefoxJa) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val adminUser = loginWithTestUser(browser)
        val user = createNormalUser(
          browser,
          "user001", "password0987", "null@ruimo.com", "firstname", "lastname", "company001"
        )
        logoff(browser)
        login(browser, "user001", "password0987")

        browser.goTo(
          "http://localhost:3333" + controllers.routes.ProfileMaintenance.changeProfile() + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("commonTitle", Messages("changeUserProfileTitle"))
        browser.find("#firstName").getAttribute("value") === "firstname"
        browser.find("#lastName").getAttribute("value") === "lastname"
        browser.find("#email").getAttribute("value") === "null@ruimo.com"

        browser.fill("#firstName").`with`("firstName2")
        browser.fill("#lastName").`with`("lastName2")
        browser.fill("#email").`with`("null2@ruimo.com")
        // Wrong password.
        browser.fill("#password").`with`("password9876")
        browser.find("#updateButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#password_field.error dd.error").getText === Messages("currentPasswordNotMatch")
        browser.fill("#password").`with`("password0987")
        browser.find("#updateButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".message").getText === Messages("userInfoIsUpdated")

        val newUser = StoreUser.findByUserName("user001").get
        newUser.firstName === "firstName2"
        newUser.lastName === "lastName2"
        newUser.middleName === None
        newUser.email === "null2@ruimo.com"
        newUser.stretchCount === StoreUser.PasswordHashStretchCount()
      }}
    }
  }
}
