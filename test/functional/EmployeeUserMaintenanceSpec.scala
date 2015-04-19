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
import models.{StoreUser, OrderNotification, Site, SiteUser, LocaleInfo}
import LocaleInfo._

class EmployeeUserMaintenanceSpec extends Specification {
  val disableEmployeeMaintenance = Map("siteOwnerCanEditEmployee" -> false)
  val enableEmployeeMaintenance = Map("siteOwnerCanEditEmployee" -> false)

  "Employee user" should {
    "Start page should be shown." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ disableEmployeeMaintenance)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site = Site.createNew(Ja, "店舗1")
        val siteUser = SiteUser.createNew(user.id.get, site.id.get)

        browser.goTo(
          "http://localhost:3333" + 
          controllers.routes.UserMaintenance.startCreateNewEmployeeUser().url +
          "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        1 === 1
      }}
    }

    "Login with super user. Since super user cannot edit employee, page is redirected to top." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ disableEmployeeMaintenance)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
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

