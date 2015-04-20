package functional

import play.api.test._
import play.api.test.Helpers._
import play.api.Play.current
import java.sql.Connection
import java.util.concurrent.TimeUnit

import helpers.Helper._

import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.i18n.{Lang, Messages}
import play.api.db.DB
import play.api.test.TestServer
import play.api.test.FakeApplication
import java.sql.Date.{valueOf => date}
import org.openqa.selenium.By
import models.{StoreUser, OrderNotification, Site, SiteUser, LocaleInfo, UserRole}
import LocaleInfo._

class EmployeeUserMaintenanceSpec extends Specification {
  val disableEmployeeMaintenance = Map("siteOwnerCanEditEmployee" -> false)
  val enableEmployeeMaintenance = Map("siteOwnerCanEditEmployee" -> true)

  def createNormalUser(): StoreUser = DB.withConnection { implicit conn =>
    StoreUser.create(
      "administrator", "Admin", None, "Manager", "admin@abc.com",
      4151208325021896473L, -1106301469931443100L, UserRole.NORMAL, Some("Company1")
    )
  }

  def login(browser: TestBrowser) {
    browser.goTo("http://localhost:3333" + controllers.routes.Admin.index.url)
    browser.fill("#userName").`with`("administrator")
    browser.fill("#password").`with`("password")
    browser.click("#doLoginButton")
  }

  "Employee user" should {
    "Employee editing is disabled." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ disableEmployeeMaintenance)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = createNormalUser()
        val site = Site.createNew(Ja, "店舗1")
        val siteUser = SiteUser.createNew(user.id.get, site.id.get)
        login(browser)

        browser.goTo(
          "http://localhost:3333" + 
          controllers.routes.UserMaintenance.startCreateNewEmployeeUser().url +
          "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        
        // Since employee maintenance is disabled, redirected to top.
        browser.title() === Messages("company.name")
      }}
    }

    "Employee editing is enabled." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ enableEmployeeMaintenance)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = createNormalUser()
        val site = Site.createNew(Ja, "店舗1")
        val siteUser = SiteUser.createNew(user.id.get, site.id.get)
        login(browser)

        browser.goTo(
          "http://localhost:3333" + 
          controllers.routes.UserMaintenance.startCreateNewEmployeeUser().url +
          "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title() === Messages("createEmployeeTitle")
Thread.sleep(20000)
      }}
    }
    // Since employee maintenance is disabled, redirected to top
    "Login with super user. Since super user cannot edit employee, page is redirected to top." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ disableEmployeeMaintenance)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)

        browser.goTo(
          "http://localhost:3333" + 
          controllers.routes.UserMaintenance.startCreateNewEmployeeUser().url +
          "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title() === Messages("company.name")
      }}
    }
  }
}

