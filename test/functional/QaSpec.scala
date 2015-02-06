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
import java.util.concurrent.TimeUnit
import models.{StoreUser, Address, UserAddress, CountryCode, JapanPrefecture}

class QaSpec extends Specification {
  val conf = inMemoryDatabase() ++ disableMailer

  "QA" should {
    "All field should be blank if no one is logged in." in {
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        if (NeedLogin.needAuthenticationEntirely) {
          1 === 1
        }
        else {
          implicit val lang = Lang("ja")

          browser.goTo(
            "http://localhost:3333" + controllers.routes.Qa.index() + "?lang=" + lang.code
          )
          browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
          browser.title === Messages("qaTitle")

          browser.find("#qaType_field .help-block").getText === Messages("constraint.required")
          browser.find("#comment").getText === ""
          browser.find("#companyName").getText === ""
          browser.find("#firstName").getText === ""
          browser.find("#lastName").getText === ""
          browser.find("#tel").getText === ""
          browser.find("#email").getText === ""
        }}
      }
    }

    "If some one is logged in, some fields should be filled." in {
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        val user: StoreUser = loginWithTestUser(browser)

        implicit val lang = Lang("ja")

        browser.goTo(
          "http://localhost:3333" + controllers.routes.Qa.index() + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("qaTitle")

        browser.find("#comment").getAttribute("value") === ""
        browser.find("#firstName").getAttribute("value") === "Admin"
        browser.find("#lastName").getAttribute("value") === "Manager"
        browser.find("#tel").getAttribute("value") === ""
        browser.find("#email").getAttribute("value") === "admin@abc.com"
        browser.find("#companyName").getAttribute("value") === "Company1"
      }}
    }

    "If address is available telephone number should be filled." in {
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        val user: StoreUser = loginWithTestUser(browser)
        val addr1 = Address.createNew(
          countryCode = CountryCode.JPN,
          firstName = "firstName1",
          lastName = "lastName1",
          zip1 = "zip1",
          zip2 = "zip2",
          prefecture = JapanPrefecture.東京都,
          address1 = "address1-1",
          address2 = "address1-2",
          tel1 = "tel1-1",
          comment = "comment1"
        )
        UserAddress.createNew(user.id.get, addr1.id.get)

        implicit val lang = Lang("ja")

        browser.goTo(
          "http://localhost:3333" + controllers.routes.Qa.index() + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("qaTitle")

        browser.find("#comment").getAttribute("value") === ""
        browser.find("#firstName").getAttribute("value") === "Admin"
        browser.find("#lastName").getAttribute("value") === "Manager"
        browser.find("#tel").getAttribute("value") === "tel1-1"
        browser.find("#email").getAttribute("value") === "admin@abc.com"
        browser.find("#companyName").getAttribute("value") === "Company1"
      }}
    }

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

        if (NeedLogin.needAuthenticationEntirely) {
          browser.fill("#companyName").`with`("")
          browser.fill("#firstName").`with`("")
          browser.fill("#lastName").`with`("")
          browser.fill("#tel").`with`("")
          browser.fill("#email").`with`("")
        }

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
