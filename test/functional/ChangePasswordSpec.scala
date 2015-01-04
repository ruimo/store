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
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val adminUser = loginWithTestUser(browser)

        browser.goTo(
          "http://localhost:3333" + controllers.routes.UserEntry.changePasswordStart() + "?lang=" + lang.code
        )
        
        browser.find("#doResetPasswordButton").click()
        browser.find(".globalErrorMessage").getText === Messages("inputError")
        browser.find("#currentPassword_field .error").getText === Messages("error.required")

Thread.sleep(20000)
        1 === 1
      }}
    }
  }
}
