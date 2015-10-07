package functional

import org.openqa.selenium.By
import com.ruimo.scoins.Scoping._
import play.api.test._
import play.api.test.Helpers._
import play.api.Play.current
import java.util.concurrent.TimeUnit

import helpers.Helper._

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
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("commonTitle") + " " + Messages("createNewSiteTitle")
        browser.click("select[id='langId'] option[value='1']")
        browser.fill("#siteName").`with`("Store01")
        browser.find("#createNewSiteForm").find("input[type='submit']").click
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".message").getText() === Messages("siteIsCreated")

        DB.withConnection { implicit conn =>
          val map = Site.listAsMap
          map.size === 1
          map.head._2.localeId === LocaleInfo(1).id
          map.head._2.name === "Store01"
        }
      }}
    }

    "Can change site." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)

        val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
        val site2 = Site.createNew(LocaleInfo.En, "Shop2")

        browser.goTo(
          "http://localhost:3333" + controllers.routes.SiteMaintenance.editSite().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("commonTitle") + " " + Messages("editSiteTitle")
        browser.find(".siteTableBody").getTexts.size === 2
        doWith(browser.find(".siteTableBody", 0)) { e =>
          e.find(".id").getText === site2.id.get.toString
          e.find(".siteName").getText === site2.name
        }
        doWith(browser.find(".siteTableBody", 1)) { e =>
          e.find(".id").getText === site1.id.get.toString
          e.find(".siteName").getText === site1.name
        }

        browser.find(".siteTableBody", 0).find("a").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("commonTitle") + " " + Messages("changeSiteTitle")
        browser.find("#siteId").getAttribute("value") === site2.id.get.toString
        browser.webDriver.findElement(By.id("langId"))
          .findElement(By.cssSelector("option[value='" + LocaleInfo.En.id + "']")).isSelected === true
        browser.find("#siteName").getAttribute("value") === site2.name

        browser.fill("#siteName").`with`("")
        browser.find("#changeSite").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find(".globalErrorMessage").getText === Messages("inputError")
        browser.find("#siteName_field dd.error").getText === Messages("error.required")

        browser.fill("#siteName").`with`("SITE002")
        browser.webDriver.findElement(By.id("langId"))
          .findElement(By.cssSelector("option[value='" + LocaleInfo.Ja.id + "']")).click()
        browser.find("#changeSite").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("commonTitle") + " " + Messages("editSiteTitle")
        browser.find(".siteTableBody").getTexts.size === 2
        doWith(browser.find(".siteTableBody", 0)) { e =>
          e.find(".id").getText === site2.id.get.toString
          e.find(".siteName").getText === "SITE002"
        }
        browser.find(".siteTableBody", 0).find("a").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.webDriver.findElement(By.id("langId"))
          .findElement(By.cssSelector("option[value='" + LocaleInfo.Ja.id + "']")).isSelected === true
        browser.find("#siteName").getAttribute("value") === "SITE002"
      }}
    }
  }
}

