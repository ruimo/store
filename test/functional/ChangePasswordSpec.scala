package functional

import play.api.test.Helpers._
import play.api.Play.current
import helpers.Helper._
import models._
import org.joda.time.format.DateTimeFormat
import play.api.db.DB
import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.i18n.{Lang, Messages}
import java.sql.Date.{valueOf => date}
import LocaleInfo._
import java.sql.Connection
import scala.collection.JavaConversions._
import com.ruimo.scoins.Scoping._

class ChangePasswordSpec extends Specification {
  "Change password" should {
    "Be able to do validation." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val adminUser = loginWithTestUser(browser)

        browser.goTo(
          "http://localhost:3333" + controllers.routes.UserEntry.changePasswordStart() + "?lang=" + lang.code
        )
        
        // Empty
        browser.find("#doResetPasswordButton").click()
        browser.find(".globalErrorMessage").getText === Messages("inputError")
        browser.find("#currentPassword_field .error").getText === Messages("error.required")

        val passwordMinLength = NeedLogin.passwordMinLength
        browser.find("#newPassword_main_field .error").getText === Messages("error.minLength", passwordMinLength)
        browser.find("#newPassword_confirm_field .error").getText === Messages("error.minLength", passwordMinLength)

        // Current password is wrong.
        browser.fill("#currentPassword_field input[type='password']").`with`("password0")
        browser.fill("#newPassword_main_field input[type='password']").`with`("password2")
        browser.fill("#newPassword_confirm_field input[type='password']").`with`("password2")
        browser.find("#doResetPasswordButton").click()
        browser.find("#currentPassword_field .error").getText === Messages("currentPasswordNotMatch")

        // Confirmation password does not match.
        browser.fill("#currentPassword_field input[type='password']").`with`("password")
        browser.fill("#newPassword_main_field input[type='password']").`with`("password2")
        browser.fill("#newPassword_confirm_field input[type='password']").`with`("password3")
        browser.find("#doResetPasswordButton").click()
        browser.find("#newPassword_confirm_field .error").getText === Messages("confirmPasswordDoesNotMatch")

        val newPassword = "gH'(1hgf6"
        browser.fill("#currentPassword_field input[type='password']").`with`("password")
        browser.fill("#newPassword_main_field input[type='password']").`with`(newPassword)
        browser.fill("#newPassword_confirm_field input[type='password']").`with`(newPassword)
        browser.find("#doResetPasswordButton").click()

        browser.find(".message").getText === Messages("passwordIsUpdated")
        
        // Check if new password is saved.
        StoreUser(adminUser.id.get).passwordMatch(newPassword) === true
      }}
    }
  }
}
