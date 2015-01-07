package functional

import play.api.test.Helpers._
import play.api.Play.current
import helpers.Helper._
import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.i18n.{Lang, Messages}
import play.api.db.DB
import helpers.Helper.disableMailer
import controllers.NeedLogin

class QaSpec extends Specification {
  val conf = inMemoryDatabase() ++ disableMailer

  "QA" should {
    "Show error when nothing is entered" in {
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        if (NeedLogin.needAuthenticationEntirely) {
          loginWithTestUser(browser)
        }

        browser.goTo(
          "http://localhost:3333" + controllers.routes.Qa.index() + "?lang=" + lang.code
        )
        browser.title === Messages("qaTitle")
        browser.find("#submitQa").click()

        browser.find(".globalErrorMessage").getText === Messages("inputError")
        browser.find("#qaType_field").find(".help-inline").getText === Messages("error.required")
        browser.find("#comment_field").find(".help-inline").getText === Messages("error.required")
        browser.find("#companyName_field").find(".help-inline").getText === Messages("error.required")
        browser.find("#firstName_field").find(".help-inline").getText === Messages("error.required")
        browser.find("#lastName_field").find(".help-inline").getText === Messages("error.required")
        browser.find("#tel_field").find(".help-inline").getText ===
          Messages("error.required") + ", " + Messages("error.number")
        browser.find("#email_field").find(".help-inline").getText === Messages("error.required")
      }}
    }

    "Enter invalid tel should result in error" in {
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        if (NeedLogin.needAuthenticationEntirely) {
          loginWithTestUser(browser)
        }
        browser.goTo(
          "http://localhost:3333" + controllers.routes.Qa.index() + "?lang=" + lang.code
        )
        browser.fill("#tel").`with`("A")
        browser.find("#submitQa").click()
        browser.find("#tel_field").find(".help-inline").getText === Messages("error.number")
      }}
    }
  }
}
