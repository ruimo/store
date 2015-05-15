package functional

import play.api.test.Helpers._
import play.api.Play.current
import helpers.Helper._
import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.i18n.{Lang, Messages}
import play.api.db.DB
import helpers.Helper.disableMailer

class UserEntrySpec extends Specification {
  val conf = inMemoryDatabase() ++ disableMailer

  "User entry" should {
    "Show error when nothing is entered" in {
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        browser.goTo(
          "http://localhost:3333" + controllers.routes.UserEntry.index() + "?lang=" + lang.code
        )
        browser.title === Messages("userEntryTitle")
        browser.find("#submitUserEntry").click()

        browser.find(".globalErrorMessage").getText === Messages("inputError")
        browser.find("#companyName_field").find(".help-inline").getText === Messages("error.required")
        browser.find("#zipPanel").find(".zipError").getText === Messages("zipError")
        browser.find("#address1_field").find(".help-inline").getText === Messages("error.required")
        browser.find("#address2_field").find(".help-inline").getText === Messages("error.required")
        browser.find("#tel_field").find(".help-inline").getText === Messages("error.number")
        browser.find("#firstName_field").find(".help-inline").getText === Messages("error.required")
        browser.find("#lastName_field").find(".help-inline").getText === Messages("error.required")
        browser.find("#email_field").find(".help-inline").getText === Messages("error.required")
      }}
    }

    "Enter invalid zip should result in error" in {
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        browser.goTo(
          "http://localhost:3333" + controllers.routes.UserEntry.index() + "?lang=" + lang.code
        )
        browser.fill("#zipPanel input[name=zip1]").`with`("12")
        browser.fill("#zipPanel input[name=zip2]").`with`("123")
        browser.find("#submitUserEntry").click()
        browser.find("#zipPanel").find(".zipError").getText === Messages("zipError")

        browser.fill("#zipPanel input[name=zip1]").`with`("123")
        browser.fill("#zipPanel input[name=zip2]").`with`("123")
        browser.find("#submitUserEntry").click()
        browser.find("#zipPanel").find(".zipError").getText === Messages("zipError")

        browser.fill("#zipPanel input[name=zip1]").`with`("12")
        browser.fill("#zipPanel input[name=zip2]").`with`("1234")
        browser.find("#submitUserEntry").click()
        browser.find("#zipPanel").find(".zipError").getText === Messages("zipError")

        browser.fill("#zipPanel input[name=zip1]").`with`("")
        browser.fill("#zipPanel input[name=zip2]").`with`("1234")
        browser.find("#submitUserEntry").click()
        browser.find("#zipPanel").find(".zipError").getText === Messages("zipError")

        browser.fill("#zipPanel input[name=zip1]").`with`("123")
        browser.fill("#zipPanel input[name=zip2]").`with`("")
        browser.find("#submitUserEntry").click()
        browser.find("#zipPanel").find(".zipError").getText === Messages("zipError")

        browser.fill("#zipPanel input[name=zip1]").`with`("12A")
        browser.fill("#zipPanel input[name=zip2]").`with`("123B")
        browser.find("#submitUserEntry").click()
        browser.find("#zipPanel").find(".zipError").getText === Messages("zipError")
      }}
    }

    "Enter invalid tel should result in error" in {
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        browser.goTo(
          "http://localhost:3333" + controllers.routes.UserEntry.index() + "?lang=" + lang.code
        )
        browser.fill("#tel").`with`("A")
        browser.find("#submitUserEntry").click()
        browser.find("#tel_field").find(".help-inline").getText === Messages("error.number")
      }}
    }

    "Enter invalid fax should result in error" in {
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        browser.goTo(
          "http://localhost:3333" + controllers.routes.UserEntry.index() + "?lang=" + lang.code
        )
        browser.fill("#fax").`with`("A")
        browser.find("#submitUserEntry").click()
        browser.find("#fax_field").find(".help-inline").getText === Messages("error.number")
      }}
    }
  }
}
