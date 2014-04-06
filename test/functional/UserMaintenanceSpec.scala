package functional

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
import models.{StoreUser, OrderNotification}

class UserMaintenanceSpec extends Specification {
  "User maintenance should" should {
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

        browser.title() === Messages("modifyUserTitle")
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

        browser.title() === Messages("editUserTitle")
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

        browser.title() === Messages("modifyUserTitle")
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

        browser.title() === Messages("editUserTitle")
        OrderNotification.getByUserId(user.id.get).isDefined === false
      }}
    }
  }
}
