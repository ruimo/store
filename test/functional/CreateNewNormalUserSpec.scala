package functional

import anorm._
import helpers.PasswordHash
import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable._
import play.api.i18n.{Lang, Messages}
import models.{UserRole, StoreUser}
import play.api.Play
import play.api.Play.current
import play.api.db.DB
import helpers.Helper._
import java.util.concurrent.TimeUnit

class CreateNewNormalUserSpec extends Specification {
  "CreateNewNormalUser" should {
    "Can create record" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)

        browser.goTo("http://localhost:3333" +
                     controllers.routes.UserMaintenance.startCreateNewNormalUser.url + "?lang=" + lang.code)
        
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("commonTitle", Messages("createNormalUserTitle"))
        browser.fill("#userName").`with`("01234567")
        browser.fill("#firstName").`with`("firstname")
        browser.fill("#lastName").`with`("lastname")
        browser.fill("#companyName").`with`("site01")
        browser.fill("#email").`with`("ruimo@ruimo.com")
        browser.fill("#password_main").`with`("12345678")
        browser.fill("#lastName").`with`("lastName")
        browser.fill("#password_main").`with`("password")
        browser.fill("#password_confirm").`with`("password")

        browser.submit("#registerNormalUser")
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        // Waiting next normal user to create.
        browser.title === Messages("commonTitle", Messages("createNormalUserTitle"))
        browser.find(".message").getText === Messages("userIsCreated")
        
        val newUser = StoreUser.findByUserName("01234567").get
        newUser.userName === "01234567"
        newUser.firstName === "firstname"
        newUser.middleName === None
        newUser.lastName === "lastName"
        newUser.email === "ruimo@ruimo.com"
        newUser.passwordHash === PasswordHash.generate("password", newUser.salt)
        newUser.deleted === false
        newUser.userRole === UserRole.NORMAL
        newUser.companyName === Some("site01")
      }}
    }

    "Email error" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        browser.goTo("http://localhost:3333" +
                     controllers.routes.UserMaintenance.startCreateNewNormalUser.url + "?lang=" + lang.code)
        
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("commonTitle", Messages("createNormalUserTitle"))
        browser.fill("#userName").`with`("01234567")
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
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        // Waiting next normal user to create.
        browser.title === Messages("commonTitle", Messages("createNormalUserTitle"))

        browser.$(".globalErrorMessage").getText === Messages("inputError")
        browser.$("#email_field dd.error").getText === Messages("error.email")
      }}
    }

    "Confirmation password does not match." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        browser.goTo("http://localhost:3333" +
                     controllers.routes.UserMaintenance.startCreateNewNormalUser.url + "?lang=" + lang.code)

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("commonTitle", Messages("createNormalUserTitle"))
        browser.fill("#userName").`with`("01234567")
        browser.fill("#firstName").`with`("firstname")
        browser.fill("#lastName").`with`("lastname")
        browser.fill("#companyName").`with`("companyname")
        browser.fill("#email").`with`("ruimo@ruimo.com")
        browser.fill("#password_main").`with`("12345678")
        browser.fill("#password_confirm").`with`("12345679")
        browser.submit("#registerNormalUser")

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        // Waiting next normal user to create.
        browser.title === Messages("commonTitle", Messages("createNormalUserTitle"))

        browser.$(".globalErrorMessage").getText === Messages("inputError")
        browser.$("#password_confirm_field dd.error").getText === Messages("confirmPasswordDoesNotMatch")
      }
    }

    "If normalUserNamePattern is set, user name should match the specified pattern." in {
      // User name should be 6 digit string.
      val app = FakeApplication(
        additionalConfiguration = 
          inMemoryDatabase() + ("normalUserNamePattern" -> "[0-9]{6}") + ("maxCountOfSupplementalEmail" -> 0)
      )
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)

        browser.goTo("http://localhost:3333" +
                     controllers.routes.UserMaintenance.startCreateNewNormalUser.url + "?lang=" + lang.code)
        
        browser.find("#supplementalEmails_0_field").size === 0

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("commonTitle", Messages("createNormalUserTitle"))
        browser.fill("#userName").`with`("abcdef")
        browser.fill("#firstName").`with`("firstname")
        browser.fill("#lastName").`with`("lastname")
        browser.fill("#companyName").`with`("site01")
        browser.fill("#email").`with`("ruimo@ruimo.com")
        browser.fill("#password_main").`with`("12345678")
        browser.fill("#lastName").`with`("lastName")
        browser.fill("#password_main").`with`("password")
        browser.fill("#password_confirm").`with`("password")

        browser.submit("#registerNormalUser")
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        // User name is not in pattern.
        browser.title === Messages("commonTitle", Messages("createNormalUserTitle"))
        browser.find("#userName_field dd.error").getText === Messages("normalUserNamePatternError")

        browser.fill("#userName").`with`("12345")
        browser.fill("#password_main").`with`("password")
        browser.fill("#password_confirm").`with`("password")
        browser.submit("#registerNormalUser")
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("commonTitle", Messages("createNormalUserTitle"))
        browser.find("#userName_field dd.error").getText === Messages("normalUserNamePatternError")

        browser.fill("#userName").`with`("1234567")
        browser.fill("#password_main").`with`("password")
        browser.fill("#password_confirm").`with`("password")
        browser.submit("#registerNormalUser")
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("commonTitle", Messages("createNormalUserTitle"))
        browser.find("#userName_field dd.error").getText === Messages("normalUserNamePatternError")

        browser.fill("#userName").`with`("123456")
        browser.fill("#password_main").`with`("password")
        browser.fill("#password_confirm").`with`("password")
        browser.submit("#registerNormalUser")
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("commonTitle", Messages("createNormalUserTitle"))
        browser.find(".message").getText === Messages("userIsCreated")

        val newUser = StoreUser.findByUserName("123456").get
        newUser.userName === "123456"
        newUser.firstName === "firstname"
        newUser.middleName === None
        newUser.lastName === "lastName"
        newUser.email === "ruimo@ruimo.com"
        newUser.passwordHash === PasswordHash.generate("password", newUser.salt)
        newUser.deleted === false
        newUser.userRole === UserRole.NORMAL
        newUser.companyName === Some("site01")
      }}
    }

    "Supplemental email fields should be shown." in {
      // User name should be 6 digit string.
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() + ("maxCountOfSupplementalEmail" -> 3))
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)

        browser.goTo("http://localhost:3333" +
                     controllers.routes.UserMaintenance.startCreateNewNormalUser.url + "?lang=" + lang.code)

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

        browser.submit("#registerNormalUser")
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.$(".globalErrorMessage").getText === Messages("inputError")
        browser.find("#supplementalEmails_1_field dd.error").getText === Messages("error.email")

        browser.fill("#password_main").`with`("password")
        browser.fill("#password_confirm").`with`("password")
        browser.fill("#supplementalEmails_0").`with`("null@ruimo.com")
        browser.fill("#supplementalEmails_1").`with`("foo@ruimo.com")

        browser.submit("#registerNormalUser")
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
