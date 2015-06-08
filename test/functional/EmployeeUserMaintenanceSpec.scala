package functional

import helpers.Formatter
import helpers.UrlHelper
import helpers.UrlHelper._
import helpers.PasswordHash
import constraints.FormConstraints
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
import models.{StoreUser, OrderNotification, Site, SiteUser, LocaleInfo, UserRole, Employee}
import LocaleInfo._
import com.ruimo.scoins.Scoping._
import SeleniumHelpers.htmlUnit
import SeleniumHelpers.FirefoxJa

class EmployeeUserMaintenanceSpec extends Specification {
  val disableEmployeeMaintenance = Map("siteOwnerCanEditEmployee" -> false)
  val enableEmployeeMaintenance = Map("siteOwnerCanEditEmployee" -> true)

  def createNormalUser(userName: String = "administrator"): StoreUser = DB.withConnection { implicit conn =>
    StoreUser.create(
      userName, "Admin", None, "Manager", "admin@abc.com",
      4151208325021896473L, -1106301469931443100L, UserRole.NORMAL, Some("Company1")
    )
  }

  def login(browser: TestBrowser, userName: String = "administrator") {
    browser.goTo("http://localhost:3333" + controllers.routes.Admin.index.url)
    browser.fill("#userName").`with`(userName)
    browser.fill("#password").`with`("password")
    browser.click("#doLoginButton")
  }

  "Employee user" should {
    "Employee editing is disabled." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ disableEmployeeMaintenance)

      SeleniumHelpers.running(TestServer(3333, app), htmlUnit) { browser => DB.withConnection { implicit conn =>
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
      SeleniumHelpers.running(TestServer(3333, app), htmlUnit) { browser => DB.withConnection { implicit conn =>
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

        // Check validation error.
        browser.find("#registerEmployee").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#userName_field .error").getText === Formatter.validationErrorString(
          FormConstraints.normalUserNameConstraint(), ""
        )
        browser.find("#password_main_field .error").getText === 
          Messages("error.minLength", FormConstraints.passwordMinLength())

        // Confirm password does not match.
        browser.fill("#userName").`with`("12345678")
        browser.fill("#password_main").`with`("abcdefgh")
        browser.fill("#password_confirm").`with`("abcdefgh1")
        browser.find("#registerEmployee").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#password_confirm_field .error").getText === Messages("confirmPasswordDoesNotMatch")

        browser.fill("#userName").`with`("12345678")
        browser.fill("#password_main").`with`("abcdefgh")
        browser.fill("#password_confirm").`with`("abcdefgh")
        browser.find("#registerEmployee").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title() === Messages("createEmployeeTitle")
        browser.find(".message").getText === Messages("userIsCreated")

        // store_user table should be updated.
        doWith(StoreUser.findByUserName(site.id.get + "-12345678").get) { user =>
          user.firstName === ""
          user.passwordHash === PasswordHash.generate("abcdefgh", user.salt)
          user.companyName === Some(site.name)

          // employee table should be updated.
          doWith(Employee.getBelonging(user.id.get).get) { emp =>
            emp.userId === user.id.get
            emp.siteId === site.id.get
          }
        }
      }}
    }

    "User name pattern error." in {
      val app = FakeApplication(
        additionalConfiguration = inMemoryDatabase() ++ enableEmployeeMaintenance + ("normalUserNamePattern" -> "[0-9]{6}")
      )
      SeleniumHelpers.running(TestServer(3333, app), FirefoxJa) { browser => DB.withConnection { implicit conn =>
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

        browser.fill("#userName").`with`("abcdef")
        browser.fill("#password_main").`with`("abcdefgh")
        browser.fill("#password_confirm").`with`("abcdefgh")
        browser.find("#registerEmployee").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title() === Messages("createEmployeeTitle")
        browser.find("#userName_field dd.error").getText === Messages("normalUserNamePatternError")

        browser.fill("#userName").`with`("12345")
        browser.fill("#password_main").`with`("abcdefgh")
        browser.fill("#password_confirm").`with`("abcdefgh")
        browser.find("#registerEmployee").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title() === Messages("createEmployeeTitle")
        browser.find("#userName_field dd.error").getText === Messages("normalUserNamePatternError")

        browser.fill("#userName").`with`("1234567")
        browser.fill("#password_main").`with`("abcdefgh")
        browser.fill("#password_confirm").`with`("abcdefgh")
        browser.find("#registerEmployee").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title() === Messages("createEmployeeTitle")
        browser.find("#userName_field dd.error").getText === Messages("normalUserNamePatternError")

        browser.fill("#userName").`with`("123456")
        browser.fill("#password_main").`with`("abcdefgh")
        browser.fill("#password_confirm").`with`("abcdefgh")
        browser.find("#registerEmployee").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title() === Messages("createEmployeeTitle")
        browser.find(".message").getText === Messages("userIsCreated")

        // store_user table should be updated.
        doWith(StoreUser.findByUserName(site.id.get + "-123456").get) { user =>
          user.firstName === ""
          user.passwordHash === PasswordHash.generate("abcdefgh", user.salt)
          user.companyName === Some(site.name)

          // employee table should be updated.
          doWith(Employee.getBelonging(user.id.get).get) { emp =>
            emp.userId === user.id.get
            emp.siteId === site.id.get
          }
        }
      }}
    }

    // Since employee maintenance is disabled, redirected to top
    "Login with super user. Since super user cannot edit employee, page is redirected to top." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ disableEmployeeMaintenance)
      SeleniumHelpers.running(TestServer(3333, app), htmlUnit) { browser => DB.withConnection { implicit conn =>
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

    // Since employee maintenance is disabled, redirected to top
    "Login with super user. Since super user cannot edit employee, page is redirected to top." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ disableEmployeeMaintenance)
      SeleniumHelpers.running(TestServer(3333, app), htmlUnit) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val site01 = Site.createNew(Ja, "店舗1")
        val superUser = loginWithTestUser(browser)
        val user01 = createNormalUser("user01")
        val employee01 = createNormalUser(site01.id.get + "-employee")
        val employee02 = createNormalUser((site01.id.get + 1) + "-employee")
        val siteOwner = SiteUser.createNew(user01.id.get, site01.id.get)
        logoff(browser)
        login(browser, "user01")

        browser.goTo(
          "http://localhost:3333" + 
          controllers.routes.UserMaintenance.editUser().url.addParm("lang", lang.code)
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title() === Messages("company.name")

        browser.goTo(
          "http://localhost:3333" + 
          controllers.routes.UserMaintenance.modifyUserStart(employee01.id.get).url.addParm("lang", lang.code)
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title() === Messages("company.name")
      }}
    }

    "Edit employee will show only employees of the site of currently logined store owner." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ enableEmployeeMaintenance)
      running(TestServer(3333, app), HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val site01 = Site.createNew(Ja, "店舗1")
        val superUser = loginWithTestUser(browser)
        val user01 = createNormalUser("user01")
        val employee01 = createNormalUser(site01.id.get + "-employee")
        val employee02 = createNormalUser((site01.id.get + 1) + "-employee")
        val siteOwner = SiteUser.createNew(user01.id.get, site01.id.get)
        logoff(browser)
        login(browser, "user01")

        browser.goTo(
          "http://localhost:3333" + 
          controllers.routes.UserMaintenance.editUser().url.addParm("lang", lang.code)
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".userTable .userTableBody").getTexts.size === 1
        browser.find(".userTable .userTableBody .id a").getText === employee01.id.get.toString
        browser.find(".userTable .userTableBody .name").getText === employee01.userName
        browser.find(".userTable .userTableBody .id a").click()

        browser.title() === Messages("modifyUserTitle")
        browser.find("#userId").getAttribute("value") === employee01.id.get.toString
        browser.find("#userName").getAttribute("value") === employee01.userName
        browser.find("#firstName").getAttribute("value") === employee01.firstName
        browser.find("#lastName").getAttribute("value") === employee01.lastName
        browser.find("#companyName").getAttribute("value") === employee01.companyName.get
        browser.find("#email").getAttribute("value") === employee01.email
        browser.find("#sendNoticeMail_field input[type='checkbox']").getTexts().size === 0

        browser.fill("#userName").`with`("")
        browser.fill("#firstName").`with`("")
        browser.fill("#lastName").`with`("")
        browser.fill("#companyName").`with`("")
        browser.fill("#email").`with`("")
        browser.find("#modifyUser").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".globalErrorMessage").getText === Messages("inputError")
        browser.find("#userName_field .error").getText === 
          Messages("error.minLength", FormConstraints.userNameMinLength)
        browser.find("#firstName_field .error").getText === Messages("error.required")
        browser.find("#lastName_field .error").getText === Messages("error.required")
        browser.find("#companyName_field .error").getText === Messages("error.required")
        browser.find("#email_field .error", 0).getText === Messages("error.email")
        browser.find("#email_field .error", 1).getText === Messages("error.required")
        browser.find("#password_main_field .error").getText ===
          Messages("error.minLength", FormConstraints.passwordMinLength())

        browser.fill("#userName").`with`(employee01.userName + "new")
        browser.fill("#firstName").`with`("firstName2")
        browser.fill("#lastName").`with`("lastName2")
        browser.fill("#companyName").`with`("companyName2")
        browser.fill("#email").`with`("xxx@xxx.xxx")
        browser.fill("#password_main").`with`("password2")
        browser.find("#modifyUser").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#password_confirm_field .error").getText === Messages("confirmPasswordDoesNotMatch")
        browser.fill("#password_main").`with`("password2")
        browser.fill("#password_confirm").`with`("password2")
        browser.find("#modifyUser").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title() === Messages("editUserTitle")
        browser.find(".message").getText === Messages("userIsUpdated")

        doWith(StoreUser(employee01.id.get)) { newUser =>
          newUser.userName === employee01.userName + "new"
          newUser.firstName === "firstName2"
          newUser.lastName === "lastName2"
          newUser.companyName === Some("companyName2")
          newUser.email === "xxx@xxx.xxx"
          newUser.passwordHash === PasswordHash.generate("password2", newUser.salt)
        }

        browser.find("button[data-user-id='" + employee01.id.get + "']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).until(".ui-dialog-buttonset").areDisplayed()
        browser.find(".ui-dialog-buttonset .ui-button", 1).click() // click No
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title() === Messages("editUserTitle")

        browser.goTo(
          "http://localhost:3333" + 
          controllers.routes.UserMaintenance.editUser().url.addParm("lang", lang.code)
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find("button[data-user-id='" + employee01.id.get + "']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).until(".ui-dialog-buttonset").areDisplayed()
        browser.find(".ui-dialog-buttonset .ui-button", 0).click() // click Yes
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title() === Messages("editUserTitle")

        browser.find(".userTable .userTableBody").getTexts.size === 0
      }}
    }
  }
}

