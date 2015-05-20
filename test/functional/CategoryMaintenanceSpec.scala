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
    "List nothing when there are no categories." in {
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
        browser.find(".categoryTableBody").getTexts.size === 0
      }}
    }

    "Can query all categories in order." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        import models.LocaleInfo.{Ja, En}
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
      
        browser.goTo(
          "http://localhost:3333" + controllers.routes.CategoryMaintenance.startCreateNewCategory().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("createNewCategoryTitle")

        browser.find("#langId option").getTexts.size === LocaleInfo.registry.size
        browser.find("#langId option[value='" + LocaleInfo.Ja.id + "']").click()
        browser.fill("#categoryName").`with`("カテゴリ001")
        browser.find("#createNewCategoryButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        11 to 2 by -1 foreach { i =>
          browser.find("#langId option[value='" + LocaleInfo.Ja.id + "']").click()
          browser.fill("#categoryName").`with`(f"カテゴリ$i%03d")
          browser.find("#createNewCategoryButton").click()
          browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        }

        browser.goTo(
          "http://localhost:3333"
          + controllers.routes.CategoryMaintenance.editCategory(None).url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("tr.categoryTableBody").getTexts.size === 10

        browser.find(".categoryTableName", 0).getText === "カテゴリ001"
        browser.find(".categoryTableName", 1).getText === "カテゴリ011"
        browser.find(".categoryTableName", 2).getText === "カテゴリ010"
        browser.find(".categoryTableName", 3).getText === "カテゴリ009"
        browser.find(".categoryTableName", 4).getText === "カテゴリ008"
        browser.find(".categoryTableName", 5).getText === "カテゴリ007"
        browser.find(".categoryTableName", 6).getText === "カテゴリ006"
        browser.find(".categoryTableName", 7).getText === "カテゴリ005"
        browser.find(".categoryTableName", 8).getText === "カテゴリ004"
        browser.find(".categoryTableName", 9).getText === "カテゴリ003"
        browser.find(".nextPageButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("tr.categoryTableBody").getTexts.size === 1
        browser.find(".categoryTableName").getText === "カテゴリ002"

        browser.find(".categoryTableHeaderId .orderColumn").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        // Reverse order by id
        browser.find(".categoryTableName", 0).getText === "カテゴリ002"
        browser.find(".categoryTableName", 1).getText === "カテゴリ003"
        browser.find(".categoryTableName", 2).getText === "カテゴリ004"
        browser.find(".categoryTableName", 3).getText === "カテゴリ005"
        browser.find(".categoryTableName", 4).getText === "カテゴリ006"
        browser.find(".categoryTableName", 5).getText === "カテゴリ007"
        browser.find(".categoryTableName", 6).getText === "カテゴリ008"
        browser.find(".categoryTableName", 7).getText === "カテゴリ009"
        browser.find(".categoryTableName", 8).getText === "カテゴリ010"
        browser.find(".categoryTableName", 9).getText === "カテゴリ011"
        browser.find(".nextPageButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find("tr.categoryTableBody").getTexts.size === 1
        browser.find(".categoryTableName").getText === "カテゴリ001"

        browser.find(".categoryTableHeaderName .orderColumn").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".categoryTableName", 0).getText === "カテゴリ001"
        browser.find(".categoryTableName", 1).getText === "カテゴリ002"
        browser.find(".categoryTableName", 2).getText === "カテゴリ003"
        browser.find(".categoryTableName", 3).getText === "カテゴリ004"
        browser.find(".categoryTableName", 4).getText === "カテゴリ005"
        browser.find(".categoryTableName", 5).getText === "カテゴリ006"
        browser.find(".categoryTableName", 6).getText === "カテゴリ007"
        browser.find(".categoryTableName", 7).getText === "カテゴリ008"
        browser.find(".categoryTableName", 8).getText === "カテゴリ009"
        browser.find(".categoryTableName", 9).getText === "カテゴリ010"

        browser.find(".nextPageButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("tr.categoryTableBody").getTexts.size === 1
        browser.find(".categoryTableName").getText === "カテゴリ011"
      }}
    }

    "Can change category name." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        import models.LocaleInfo.{Ja, En}
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)

        browser.goTo(
          "http://localhost:3333" + controllers.routes.CategoryMaintenance.startCreateNewCategory().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("createNewCategoryTitle")

        browser.find("#langId option").getTexts.size === LocaleInfo.registry.size
        browser.find("#langId option[value='" + LocaleInfo.Ja.id + "']").click()
        browser.fill("#categoryName").`with`("カテゴリ001")
        browser.find("#createNewCategoryButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.goTo(
          "http://localhost:3333"
          + controllers.routes.CategoryMaintenance.editCategory(None).url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".categoryTableName", 0).getText === "カテゴリ001"

        browser.find("#langSpec option[value='" + LocaleInfo.En.id + "']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find(".categoryTableName", 0).getText === "-"

        browser.find(".editCategoryNameLink", 0).click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("editCategoryNameTitle")
        browser.find(".langName").getText === Messages("lang.ja")
        browser.find("#categoryNames_0_name").getAttribute("value") === "カテゴリ001"

        browser.fill("#categoryNames_0_name").`with`("カテゴリ999")
        browser.find("#submitCategoryNameUpdate").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("editCategoryTitle")
        browser.find(".editCategoryNameLink", 0).click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        
        browser.title === Messages("editCategoryNameTitle")
        browser.find("#createCategoryNameForm #localeId option[value='" + LocaleInfo.En.id + "']").click()
        browser.fill("#createCategoryNameForm #name").`with`("category999")
        browser.find("#submitCategoryNameCreate").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("editCategoryNameTitle")
        if (browser.find(".langName", 0).getText == Messages("lang.ja")) {
          browser.find("#categoryNames_0_name").getAttribute("value") === "カテゴリ999"
          browser.find("#categoryNames_1_name").getAttribute("value") === "category999"
          browser.find(".updateCategoryName button", 0).click()
        }
        else {
          browser.find("#categoryNames_0_name").getAttribute("value") === "category999"
          browser.find("#categoryNames_1_name").getAttribute("value") === "カテゴリ999"
          browser.find(".updateCategoryName button", 1).click()
        }
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".langName").getTexts.size === 1
        browser.find(".langName").getText == Messages("lang.en")
      }}
    }
  }
}
