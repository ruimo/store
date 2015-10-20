package functional

import java.util.concurrent.TimeUnit
import play.api.test.Helpers._
import play.api.Play.current
import helpers.Helper._
import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.i18n.{Lang, Messages}
import play.api.db.DB
import helpers.Helper.disableMailer
import helpers.UrlHelper.fromString
import models._

class QaSiteSpec extends Specification {
  val conf = inMemoryDatabase() ++ disableMailer

  "QA site" should {
    "Show form with some fields are filled" in {
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site = Site.createNew(LocaleInfo.Ja, "商店111")

        browser.goTo(
          "http://localhost:3333" + controllers.routes.Qa.qaSiteStart(site.id.get, "/back").url.addParm("lang", lang.code)
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".qaSiteNameBody").getText === site.name
        browser.find("#companyName").getAttribute("value") === user.companyName.get
        browser.find("#name").getAttribute("value") === user.fullName
        browser.find("#tel").getAttribute("value") === ""
        browser.find("#email").getAttribute("value") === user.email
        browser.find("#inquiryBody").getText === ""
        browser.find("a.backLink").getAttribute("href") === "http://localhost:3333/back"
      }}
    }

    "Show form with tel field are filled" in {
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site = Site.createNew(LocaleInfo.Ja, "商店111")
        val addr = Address.createNew(
          countryCode = CountryCode.JPN,
          firstName = "firstName1",
          lastName = "lastName1",
          zip1 = "zip1",
          zip2 = "zip2",
          prefecture = JapanPrefecture.東京都,
          address1 = "address1-1",
          address2 = "address1-2",
          tel1 = "12345678",
          comment = "comment1"
        )
        UserAddress.createNew(user.id.get, addr.id.get)

        browser.goTo(
          "http://localhost:3333" + controllers.routes.Qa.qaSiteStart(site.id.get, "/back").url.addParm("lang", lang.code)
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#companyName").getAttribute("value") === user.companyName.get
        browser.find("#name").getAttribute("value") === user.fullName
        browser.find("#tel").getAttribute("value") === "12345678"
        browser.find("#email").getAttribute("value") === user.email
        browser.find("#inquiryBody").getText === ""
      }}
    }

    "Show validation error" in {
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site = Site.createNew(LocaleInfo.Ja, "商店111")

        browser.goTo(
          "http://localhost:3333" + controllers.routes.Qa.qaSiteStart(site.id.get, "/back").url.addParm("lang", lang.code)
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.fill("#companyName").`with`("")
        browser.fill("#name").`with`("")
        browser.fill("#tel").`with`("")
        browser.fill("#email").`with`("")
        browser.fill("#inquiryBody").`with`("")
        browser.find("#submitQaSite").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".globalErrorMessage").getText === Messages("inputError")
        browser.find("#companyName_field.error span.help-inline").getText === Messages("error.required")
        browser.find("#name_field.error span.help-inline").getText === Messages("error.required")
        browser.find("#tel_field.error span.help-inline").getText === Messages("error.required") + ", " + Messages("error.number")
        browser.find("#email_field.error span.help-inline").getText === Messages("error.required")
        browser.find("#inquiryBody_field.error span.help-inline").getText === Messages("error.required")
        
        browser.fill("#tel").`with`("ABC")
        browser.find("#submitQaSite").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#tel_field.error span.help-inline").getText === Messages("error.number")

        browser.fill("#companyName").`with`("companyName002")
        browser.fill("#name").`with`("name002")
        browser.fill("#tel").`with`("12345678")
        browser.fill("#email").`with`("name@xxx.xxx")
        browser.fill("#inquiryBody").`with`("inquiry body")
        browser.find("#submitQaSite").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("commonTitle", Messages("qaConfirmTitle"))
        browser.find(".qaSiteNameBody").getText === site.name
        browser.find(".companyName .body .value").getText === "companyName002"
        browser.find("input[name='companyName']").getAttribute("value") === "companyName002"
        browser.find(".name .body .value").getText === "name002"
        browser.find("input[name='name']").getAttribute("value") === "name002"
        browser.find(".tel .body .value").getText === "12345678"
        browser.find("input[name='tel']").getAttribute("value") === "12345678"
        browser.find(".email .body .value").getText === "name@xxx.xxx"
        browser.find("input[name='email']").getAttribute("value") === "name@xxx.xxx"
        browser.find(".inquiryBody .body .value").getText === "inquiry body"
        browser.find("input[name='inquiryBody']").getAttribute("value") === "inquiry body"
        browser.find("button[value='amend']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("commonTitle", Messages("qaTitle"))
        browser.find(".qaSiteNameBody").getText === site.name
        browser.find("#companyName").getAttribute("value") === "companyName002"
        browser.find("#name").getAttribute("value") === "name002"
        browser.find("#tel").getAttribute("value") === "12345678"
        browser.find("#email").getAttribute("value") === "name@xxx.xxx"
        browser.find("#inquiryBody").getText === "inquiry body"
        browser.find("a.backLink").getAttribute("href") === "http://localhost:3333/back"

        browser.fill("#companyName").`with`("")
        browser.fill("#name").`with`("")
        browser.fill("#tel").`with`("")
        browser.fill("#email").`with`("")
        browser.fill("#inquiryBody").`with`("")
        browser.find("#submitQaSite").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".globalErrorMessage").getText === Messages("inputError")
        browser.find("#companyName_field.error span.help-inline").getText === Messages("error.required")
        browser.find("#name_field.error span.help-inline").getText === Messages("error.required")
        browser.find("#tel_field.error span.help-inline").getText === Messages("error.required") + ", " + Messages("error.number")
        browser.find("#email_field.error span.help-inline").getText === Messages("error.required")
        browser.find("#inquiryBody_field.error span.help-inline").getText === Messages("error.required")

        browser.fill("#companyName").`with`("companyName003")
        browser.fill("#name").`with`("name003")
        browser.fill("#tel").`with`("11111111")
        browser.fill("#email").`with`("name003@xxx.xxx")
        browser.fill("#inquiryBody").`with`("inquiry body003")
        browser.find("#submitQaSite").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("commonTitle", Messages("qaConfirmTitle"))
        browser.find(".qaSiteNameBody").getText === site.name
        browser.find(".companyName .body .value").getText === "companyName003"
        browser.find("input[name='companyName']").getAttribute("value") === "companyName003"
        browser.find(".name .body .value").getText === "name003"
        browser.find("input[name='name']").getAttribute("value") === "name003"
        browser.find(".tel .body .value").getText === "11111111"
        browser.find("input[name='tel']").getAttribute("value") === "11111111"
        browser.find(".email .body .value").getText === "name003@xxx.xxx"
        browser.find("input[name='email']").getAttribute("value") === "name003@xxx.xxx"
        browser.find(".inquiryBody .body .value").getText === "inquiry body003"
        browser.find("input[name='inquiryBody']").getAttribute("value") === "inquiry body003"
        browser.find("button[value='submit']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("commonTitle", Messages("qaCompletedTitle"))

        browser.find(".qaSiteNameBody").getText === site.name
        browser.find(".companyName .body .value").getText === "companyName003"
        browser.find(".name .body .value").getText === "name003"
        browser.find(".tel .body .value").getText === "11111111"
        browser.find(".email .body .value").getText === "name003@xxx.xxx"
        browser.find(".inquiryBody .body .value").getText === "inquiry body003"
        browser.find("a.backLink").getAttribute("href") === "http://localhost:3333/back"
      }}
    }
  }
}
