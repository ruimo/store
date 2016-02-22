package functional

import anorm._
import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable._
import play.api.i18n.{Lang, Messages}
import models.{UserRole, StoreUser}
import play.api.Play
import play.api.Play.current
import play.api.db.DB
import helpers.Helper._
import constraints.FormConstraints.passwordMinLength
import java.util.concurrent.TimeUnit

class CreateNewSuperUserSpec extends Specification {
  "CreateNewSuperUser" should {
    "Can create new user" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        browser.goTo("http://localhost:3333" +
                     controllers.routes.UserMaintenance.startCreateNewSuperUser.url + "?lang=" + lang.code)
        
        browser.title === Messages("commonTitle", Messages("createSuperUserTitle"))
        browser.fill("#userName").`with`("username")
        browser.fill("#firstName").`with`("firstname")
        browser.fill("#lastName").`with`("lastname")
        browser.fill("#companyName").`with`("companyname")
        browser.fill("#email").`with`("ruimo@ruimo.com")
        browser.fill("#password_main").`with`("12345678")
        browser.fill("#password_confirm").`with`("12345678")
        browser.submit("#registerSuperUser")

        // Waiting next super user to create.
        browser.title === Messages("commonTitle", Messages("createSuperUserTitle"))
        val user2 = StoreUser.findByUserName("username").get

        user2.deleted === false
        user2.email === "ruimo@ruimo.com"
        user2.firstName === "firstname"
        user2.lastName === "lastname"
        user2.userName === "username"
        user2.companyName === Some("companyname")
        user2.userRole === UserRole.ADMIN
      }}
    }

    "Minimum length error." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        browser.goTo("http://localhost:3333" +
                     controllers.routes.UserMaintenance.startCreateNewSuperUser.url + "?lang=" + lang.code)

        browser.title === Messages("commonTitle",  Messages("createSuperUserTitle"))
        browser.fill("#userName").`with`("usern")
        browser.fill("#firstName").`with`("")
        browser.fill("#lastName").`with`("")
        browser.fill("#email").`with`("")
        browser.fill("#password_main").`with`("")
        browser.fill("#password_confirm").`with`("12345678")
        browser.fill("#companyName").`with`("")

        browser.submit("#registerSuperUser")
        // Waiting next super user to create.
        browser.title === Messages("commonTitle", Messages("createSuperUserTitle"))

        browser.$(".globalErrorMessage").getText === Messages("inputError")
        browser.$("#userName_field dd.error").getText === Messages("error.minLength", 6)
        browser.$("#companyName_field dd.error").getText === Messages("error.required")
        browser.$("#firstName_field dd.error").getText === Messages("error.required")
        browser.$("#email_field dd.error").getText === Messages("error.email")
        browser.$("#password_main_field dd.error").getText === Messages("error.minLength", passwordMinLength())
      }
    }

    "Invalid email error." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        browser.goTo("http://localhost:3333" +
                     controllers.routes.UserMaintenance.startCreateNewSuperUser.url + "?lang=" + lang.code)

        browser.title === Messages("commonTitle", Messages("createSuperUserTitle"))
        browser.fill("#userName").`with`("userName")
        browser.fill("#firstName").`with`("firstName")
        browser.fill("#lastName").`with`("lastName")
        browser.fill("#email").`with`("ruimo")
        browser.fill("#password_main").`with`("password")
        browser.fill("#password_confirm").`with`("password")

        browser.submit("#registerSuperUser")
        // Waiting next super user to create.
        browser.title === Messages("commonTitle", Messages("createSuperUserTitle"))

        browser.$(".globalErrorMessage").getText === Messages("inputError")
        browser.$("#email_field dd.error").getText === Messages("error.email")
      }
    }

    "Confirmation password does not match." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        browser.goTo("http://localhost:3333" +
                     controllers.routes.UserMaintenance.startCreateNewSuperUser.url + "?lang=" + lang.code)

        browser.title === Messages("commonTitle", Messages("createSuperUserTitle"))
        browser.find("#supplementalEmails_0_field").size === 0
        browser.fill("#userName").`with`("username")
        browser.fill("#firstName").`with`("firstname")
        browser.fill("#lastName").`with`("lastname")
        browser.fill("#companyName").`with`("companyname")
        browser.fill("#email").`with`("ruimo@ruimo.com")
        browser.fill("#password_main").`with`("12345678")
        browser.fill("#password_confirm").`with`("12345679")
        browser.submit("#registerSuperUser")

        // Waiting next super user to create.
        browser.title === Messages("commonTitle", Messages("createSuperUserTitle"))

        browser.$(".globalErrorMessage").getText === Messages("inputError")
        browser.$("#password_confirm_field dd.error").getText === Messages("confirmPasswordDoesNotMatch")
      }
    }

    "Supplemental email fields should be shown." in {
      // User name should be 6 digit string.
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() + ("maxCountOfSupplementalEmail" -> 3))
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)

        browser.goTo("http://localhost:3333" +
                     controllers.routes.UserMaintenance.startCreateNewSuperUser.url + "?lang=" + lang.code)

        browser.find("#supplementalEmails_0_field").size === 1
        browser.find("#supplementalEmails_1_field").size === 1
        browser.find("#supplementalEmails_2_field").size === 1
        browser.find("#supplementalEmails_3_field").size === 0

        browser.fill("#firstName").`with`("firstname")
        browser.fill("#lastName").`with`("lastname")
        browser.fill("#companyName").`with`("site01")
        browser.fill("#email").`with`("ruimo@ruimo.com")
        browser.fill("#userName").`with`("123456")
        browser.fill("#password_main").`with`("password")
        browser.fill("#password_confirm").`with`("password")

        browser.fill("#supplementalEmails_0").`with`("null@ruimo.com")
        browser.fill("#supplementalEmails_1").`with`("aaa")

        browser.submit("#registerSuperUser")
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.$(".globalErrorMessage").getText === Messages("inputError")
        browser.find("#supplementalEmails_1_field dd.error").getText === Messages("error.email")

        browser.fill("#password_main").`with`("password")
        browser.fill("#password_confirm").`with`("password")
        browser.fill("#supplementalEmails_0").`with`("null@ruimo.com")
        browser.fill("#supplementalEmails_1").`with`("foo@ruimo.com")

        browser.submit("#registerSuperUser")
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".message").getText === Messages("userIsCreated")

        val recs = SQL(
          """
          select * from supplemental_user_email s
          inner join store_user u on s.store_user_id = u.store_user_id
          where u.user_name = {userName}
          order by email
          """
        ).on(
          'userName -> "123456"
        ).as(
          SqlParser.str("supplemental_user_email.email") *
        )
        recs.size === 2
        recs(0) === "foo@ruimo.com"
        recs(1) === "null@ruimo.com"
      }}
    }
  }
}
