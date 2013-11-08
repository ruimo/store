package functional

import play.api.test._
import play.api.test.Helpers._
import play.api.Play.current

import functional.Helper._

import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.i18n.{Lang, Messages}
import play.api.test.TestServer
import play.api.test.FakeApplication
import play.api.db.DB
import models.{LocaleInfo, Site}

class SiteMaintenanceSpec extends Specification {
  "Site maitenance" should {
    "Can create new site." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        browser.goTo(
          "http://localhost:3333" + controllers.routes.SiteMaintenance.startCreateNewSite().url + "?lang=" + lang.code
        )
        browser.title === Messages("createNewSiteTitle")
        browser.click("select[id='langId'] option[value='1']")
        browser.fill("#siteName").`with`("Store01")
        browser.click("input[type='submit']")

        browser.find(".message").getText() === Messages("siteIsCreated")

        DB.withConnection { implicit conn =>
          val map = Site.listAsMap
          map.size === 1
          map.head._2.localeId === LocaleInfo(1).id
          map.head._2.name === "Store01"
        }
      }}
    }
  }
}

