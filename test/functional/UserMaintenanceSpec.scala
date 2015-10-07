package functional

import helpers.UrlHelper._
import helpers.Helper
import play.api.test._
import play.api.test.Helpers._
import play.api.Play.current
import java.sql.Connection

import helpers.Helper._

import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.i18n.{Lang, Messages}
import play.api.db.DB
import play.api.test.TestServer
import play.api.test.FakeApplication
import java.sql.Date.{valueOf => date}
import org.openqa.selenium.By
import models.{StoreUser, OrderNotification, UserRole, Site, LocaleInfo}

class UserMaintenanceSpec extends Specification {
  "User maintenance" should {
    "Show current user's info." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)

        browser.goTo(
          "http://localhost:3333" + 
          controllers.routes.UserMaintenance.modifyUserStart(user.id.get).url +
          "&lang=" + lang.code
        )

        browser.title() === Messages("commonTitle") + " " + Messages("modifyUserTitle")
        browser.find("#userId").getAttribute("value") === user.id.get.toString
        browser.find("#userName").getAttribute("value") === user.userName
        browser.find("#firstName").getAttribute("value") === user.firstName
        browser.find("#lastName").getAttribute("value") === user.lastName
        browser.find("#companyName").getAttribute("value") === user.companyName.getOrElse("")
        browser.find("#email").getAttribute("value") === user.email
        browser.find("#password_main").getAttribute("value") === ""
        browser.find("#password_confirm").getAttribute("value") === ""
        browser.webDriver.findElement(By.id("sendNoticeMail")).isSelected === false
        browser.find("#sendNoticeMail").click()

        browser.fill("#userName").`with`("userName2")
        browser.fill("#firstName").`with`("firstName2")
        browser.fill("#lastName").`with`("lastName2")
        browser.fill("#companyName").`with`("companyName2")
        browser.fill("#email").`with`("email2@abc.com")
        browser.fill("#password_main").`with`("12345678")
        browser.fill("#password_confirm").`with`("12345678")
        browser.find("#modifyUser").click()

        browser.title() === Messages("commonTitle") + " " + Messages("editUserTitle")
        val user2 = StoreUser(user.id.get)
        user2.userName === "userName2"
        user2.firstName === "firstName2"
        user2.lastName === "lastName2"
        user2.companyName === Some("companyName2")
        user2.email === "email2@abc.com"
        OrderNotification.getByUserId(user.id.get).isDefined === true

        browser.goTo(
          "http://localhost:3333" + 
          controllers.routes.UserMaintenance.modifyUserStart(user.id.get).url +
          "&lang=" + lang.code
        )

        browser.title() === Messages("commonTitle") + " " + Messages("modifyUserTitle")
        browser.find("#userId").getAttribute("value") === user2.id.get.toString
        browser.find("#userName").getAttribute("value") === user2.userName
        browser.find("#firstName").getAttribute("value") === user2.firstName
        browser.find("#lastName").getAttribute("value") === user2.lastName
        browser.find("#companyName").getAttribute("value") === user2.companyName.getOrElse("")
        browser.find("#email").getAttribute("value") === user2.email
        browser.find("#password_main").getAttribute("value") === ""
        browser.find("#password_confirm").getAttribute("value") === ""
        browser.webDriver.findElement(By.id("sendNoticeMail")).isSelected === true

        browser.find("#sendNoticeMail").click()
        browser.fill("#password_main").`with`("12345678")
        browser.fill("#password_confirm").`with`("12345678")
        browser.find("#modifyUser").click()

        browser.title() === Messages("commonTitle") + " " + Messages("editUserTitle")
        OrderNotification.getByUserId(user.id.get).isDefined === false
      }}
    }

    "Super user see all registed employee count." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)

        val site1 = Site.createNew(LocaleInfo.Ja, "Store01")
        val site2 = Site.createNew(LocaleInfo.Ja, "Store02")

        // Employee, not registered
        val user3 = StoreUser.create(
          userName = site1.id.get + "-111111", // Employee (n-mmmm)
          firstName = "", // unregistered
          middleName = None,
          lastName = "lastName",
          email = "null@ruimo.com",
          passwordHash = 0,
          salt = 0,
          userRole = UserRole.NORMAL, // Normal user
          companyName = None
        )

        // Employee, registered
        val user4 = StoreUser.create(
          userName = site1.id.get + "-222222", // Employee (n-mmmm)
          firstName = "firstName", // registered
          middleName = None,
          lastName = "lastName",
          email = "null@ruimo.com",
          passwordHash = 0,
          salt = 0,
          userRole = UserRole.NORMAL, // Normal user
          companyName = None
        )

        // Employee, registered
        val user9 = StoreUser.create(
          userName = site2.id.get + "-77777777", // In employee format (n-mmmm), but site owner is not employee.
          firstName = "firstName", // registered
          middleName = None,
          lastName = "lastName",
          email = "null@ruimo.com",
          passwordHash = 0,
          salt = 0,
          userRole = UserRole.NORMAL,
          companyName = None
        )

        // Employee, unregistered
        val user10 = StoreUser.create(
          userName = site2.id.get + "-99999999", // In employee format (n-mmmm), but site owner is not employee.
          firstName = "", // unregistered
          middleName = None,
          lastName = "lastName",
          email = "null@ruimo.com",
          passwordHash = 0,
          salt = 0,
          userRole = UserRole.NORMAL,
          companyName = None
        )

        // Employee, unregistered
        val user11 = StoreUser.create(
          userName = site2.id.get + "-12345678", // In employee format (n-mmmm), but site owner is not employee.
          firstName = "", // unregistered
          middleName = None,
          lastName = "lastName",
          email = "null@ruimo.com",
          passwordHash = 0,
          salt = 0,
          userRole = UserRole.NORMAL,
          companyName = None
        )

        browser.goTo(
          "http://localhost:3333" + 
          controllers.routes.UserMaintenance.showRegisteredEmployeeCount().url.addParm("lang", lang.code)
        )

        browser.title() === Messages("commonTitle") + " " + Messages("showRegisteredEmployeeCount")

        browser.find(".site.body").getTexts.size === 2

        browser.find(".site.body", 0).getText === site1.name
        browser.find(".allCount.body", 0).getText === "2"
        browser.find(".registeredCount.body", 0).getText === "1"

        browser.find(".site.body", 1).getText === site2.name
        browser.find(".allCount.body", 1).getText === "3"
        browser.find(".registeredCount.body", 1).getText === "1"

        val (ownerUser, ownerSiteUser) = Helper.createStoreOwner(name = "StoreOwner01", siteId = site1.id.get)
        Helper.logoff(browser)
        Helper.login(browser, "StoreOwner01", "password")

        browser.goTo(
          "http://localhost:3333" + 
          controllers.routes.UserMaintenance.showRegisteredEmployeeCount().url.addParm("lang", lang.code)
        )

        browser.title() === Messages("commonTitle") + " " + Messages("showRegisteredEmployeeCount")
        browser.find(".site.body").getTexts.size === 1
        browser.find(".site.body", 0).getText === site1.name
        browser.find(".allCount.body", 0).getText === "2"
        browser.find(".registeredCount.body", 0).getText === "1"

        val (ownerUser2, ownerSiteUser2) = Helper.createStoreOwner(name = "StoreOwner02", siteId = site2.id.get)
        Helper.logoff(browser)
        Helper.login(browser, "StoreOwner02", "password")

        browser.goTo(
          "http://localhost:3333" + 
          controllers.routes.UserMaintenance.showRegisteredEmployeeCount().url.addParm("lang", lang.code)
        )

        browser.title() === Messages("commonTitle") + " " + Messages("showRegisteredEmployeeCount")
        browser.find(".site.body").getTexts.size === 1
        browser.find(".site.body", 0).getText === site2.name
        browser.find(".allCount.body", 0).getText === "3"
        browser.find(".registeredCount.body", 0).getText === "1"
      }}
    }
  }
}
