package functional

import models._
import java.util.concurrent.TimeUnit
import play.api.test._
import play.api.test.Helpers._
import play.api.Play.current
import java.sql.Connection
import helpers.Helper._
import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.i18n.{Lang, Messages}
import play.api.db.DB
import play.api.test.TestServer
import play.api.test.FakeApplication
import java.sql.Date.{valueOf => date}

class CategoryMaintenanceSpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  "Category maintenance" should {
    "create new category" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        import models.LocaleInfo.{Ja, En}
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
      
        browser.goTo(
          "http://localhost:3333" + controllers.routes.CategoryMaintenance.editCategory(None).url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("editCategoryTitle")
        
        browser.find("#langSpec option").getTexts.size === LocaleInfo.registry.size
      }}
    }
  }
}
