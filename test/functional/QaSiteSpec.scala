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
          "http://localhost:3333" + controllers.routes.Qa.qaSiteStart(site.id.get).url.addParm("lang", lang.code)
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#companyName").getAttribute("value") === user.companyName.get
        browser.find("#name").getAttribute("value") === user.fullName
        browser.find("#tel").getAttribute("value") === ""
        browser.find("#email").getAttribute("value") === user.email
        browser.find("#inquiryBody").getText === ""
      }}
    }
  }
}

