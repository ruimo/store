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
import helpers.Helper.disableMailer
import helpers.{PasswordHash, TokenGenerator, RandomTokenGenerator}
import models.StoreUser
import models.UserRole
import models.ResetPassword
import controllers.NeedLogin

class ResetPasswordSpec extends Specification {
  val conf = inMemoryDatabase() ++ disableMailer

  "Reset password" should {
    "Can reset password" in {
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val salt = RandomTokenGenerator().next
        val hash = PasswordHash.generate("password", salt)

        val user = StoreUser.create(
          "userName", "firstName", Some("middleName"), "lastName", "email",
          hash, salt, UserRole.NORMAL, Some("companyName")
        )

        browser.goTo(
          "http://localhost:3333" + controllers.routes.UserEntry.resetPasswordStart() + "?lang=" + lang.code
        )

        browser.title === Messages("resetPassword")

        // Empty input
        browser.find("#doResetPasswordButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find(".globalErrorMessage").getText === Messages("inputError")
        browser.find("#userName_field dd.error").getText === Messages("error.required")

        // Wrong user name
        browser.fill("#userName").`with`("userName2")
        browser.find("#doResetPasswordButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find(".globalErrorMessage").getText === Messages("inputError")
        browser.find("#userName_field dd.error").getText === Messages("error.value")

        val now = System.currentTimeMillis
        browser.fill("#userName").`with`("userName")
        browser.find("#doResetPasswordButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        val now2 = System.currentTimeMillis
        browser.title === Messages("resetPasswordMailSent")
        val rec = ResetPassword.getByStoreUserId(user.id.get).get
        (now <= rec.resetTime && rec.resetTime <= now2) === true
        
        browser.goTo(
          "http://localhost:3333" + controllers.routes.UserEntry.resetPasswordConfirm(user.id.get, rec.token) + "&lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("resetPassword")
        browser.find("input[name='userId']").getAttribute("value") === user.id.get.toString
        browser.find("input[name='token']").getAttribute("value") === rec.token.toString

        browser.find("#doResetPasswordButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".globalErrorMessage").getText === Messages("inputError")
        browser.find("#password_main_field .error").getText === 
          Messages("error.minLength", constraints.FormConstraints.passwordMinLength.toString)
        browser.find("#password_confirm_field .error").getText === 
          Messages("error.minLength", constraints.FormConstraints.passwordMinLength.toString)

        browser.fill("#password_main").`with`("12345678")
        browser.find("#doResetPasswordButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find("#password_confirm_field .error").getText === 
          Messages("error.minLength", constraints.FormConstraints.passwordMinLength.toString)

        browser.fill("#password_main").`with`("12345678")
        browser.fill("#password_confirm").`with`("12345679")
        browser.find("#doResetPasswordButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find(".globalErrorMessage").getText === Messages("inputError")
        browser.find("#password_confirm_field .error").getText === Messages("confirmPasswordDoesNotMatch")

        browser.fill("#password_main").`with`("1q2w3e4r")
        browser.fill("#password_confirm").`with`("1q2w3e4r")
        browser.find("#doResetPasswordButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("passwordIsUpdated")
        val newUser = StoreUser(user.id.get)
        newUser.passwordHash === PasswordHash.generate("1q2w3e4r", newUser.salt)
      }}
    }
  }
}
