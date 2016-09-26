package functional

import SeleniumHelpers.FirefoxJa
import play.api.http.Status
import org.openqa.selenium.By
import java.nio.file.{Paths, Files}
import org.openqa.selenium.JavascriptExecutor
import java.util.concurrent.TimeUnit
import helpers.UrlHelper
import helpers.UrlHelper._
import anorm._
import play.api.test.Helpers._
import play.api.Play.current
import helpers.Helper._
import models._
import org.joda.time.format.DateTimeFormat
import play.api.db.DB
import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.i18n.{Lang, Messages}
import java.sql.Connection
import com.ruimo.scoins.Scoping._

class NewsMaintenanceSpec extends Specification {
  val testDir = Files.createTempDirectory(null)
  lazy val withTempDir = Map(
    "news.picture.path" -> testDir.toFile.getAbsolutePath,
    "news.picture.fortest" -> true
  )

  "News maintenace" should {
    "Create news" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTempDir)
      running(TestServer(3333, app), FirefoxJa) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val adminUser = loginWithTestUser(browser)
        browser.goTo(
          "http://localhost:3333" + controllers.routes.NewsMaintenance.startCreateNews().url.addParm("lang", lang.code)
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find("input[type='submit']").click
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".globalErrorMessage").getText === Messages("inputError")
        browser.find("#title_field dd.error").getText === Messages("error.required")
        browser.find("#newsContents_field dd.error").getText === Messages("error.required")
        browser.find("#releaseDateTextBox_field dd.error").getText === Messages("error.date")

        browser.fill("#title").`with`("title01")
        browser.webDriver.asInstanceOf[JavascriptExecutor].executeScript("tinyMCE.activeEditor.setContent('Contents01');")
        browser.fill("#releaseDateTextBox").`with`("2016年01月02日")
        browser.find("input[type='submit']").click
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("commonTitle", Messages("createNewsTitle"))
        browser.find(".globalErrorMessage").size === 0
        browser.find(".message").getText === Messages("newsIsCreated")

        browser.goTo(
          "http://localhost:3333" + controllers.routes.NewsMaintenance.editNews().url.addParm("lang", lang.code)
        )
        browser.title === Messages("commonTitle", Messages("newsMaintenanceTitle"))
        browser.find(".newsTableBody .title").getText === "title01"
        browser.find(".newsTableBody .releaseTime").getText === "2016年01月02日"
        browser.find(".newsTableBody .id a").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("commonTitle", Messages("modifyNewsTitle"))
        browser.find("#title").getAttribute("value") === "title01"
        browser.webDriver.asInstanceOf[JavascriptExecutor].executeScript("return tinyMCE.activeEditor.getContent();") === "<p>Contents01</p>"
        browser.find("#releaseDateTextBox").getAttribute("value") === "2016年01月02日"

        browser.webDriver
          .findElement(By.id("newsPictureUpload0"))
          .sendKeys(Paths.get("testdata/kinseimaruIdx.jpg").toFile.getAbsolutePath)
        val now = System.currentTimeMillis
        browser.click("#newsPictureUploadSubmit0")

        val id = browser.find("#idValue").getAttribute("value").toLong
        testDir.resolve(id + "_0.jpg").toFile.exists === true
        downloadBytes(
          Some(now - 1000),
          "http://localhost:3333" + controllers.routes.NewsPictures.getPicture(id, 0).url
        )._1 === Status.OK

        downloadBytes(
          Some(now + 5000),
          "http://localhost:3333" + controllers.routes.NewsPictures.getPicture(id, 0).url
        )._1 === Status.NOT_MODIFIED

        // Delete file.
        browser.click("#newsPictureRemove0")

        testDir.resolve(id + "_0.jpg").toFile.exists === false

        browser.fill("#title").`with`("title02")
        browser.webDriver.asInstanceOf[JavascriptExecutor].executeScript("tinyMCE.activeEditor.setContent('Contents02');")
        browser.fill("#releaseDateTextBox").`with`("2016年02月02日")
        browser.find(".updateButton").click
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".message").getText === Messages("newsIsUpdated")
        browser.title === Messages("commonTitle", Messages("newsMaintenanceTitle"))
        browser.find(".newsTableBody .title").getText === "title02"
        browser.find(".newsTableBody .releaseTime").getText === "2016年02月02日"
        browser.find(".newsTableBody .id a").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("commonTitle", Messages("modifyNewsTitle"))
        browser.find("#title").getAttribute("value") === "title02"
        browser.webDriver.asInstanceOf[JavascriptExecutor].executeScript("return tinyMCE.activeEditor.getContent();") === "<p>Contents02</p>"
        browser.find("#releaseDateTextBox").getAttribute("value") === "2016年02月02日"

        browser.goTo(
          "http://localhost:3333" + controllers.routes.NewsMaintenance.editNews().url.addParm("lang", lang.code)
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".deleteButton").click()
        browser.await().atMost(10, TimeUnit.SECONDS).until(".no-button").isPresent
        browser.find(".no-button").click()
        browser.await().atMost(10, TimeUnit.SECONDS).until(".no-button").isPresent

        browser.goTo(
          "http://localhost:3333" + controllers.routes.NewsMaintenance.editNews().url.addParm("lang", lang.code)
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".deleteButton").click()
        browser.await().atMost(10, TimeUnit.SECONDS).until(".yes-button").isPresent
        browser.find(".yes-button").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.goTo(
          "http://localhost:3333" + controllers.routes.NewsMaintenance.editNews().url.addParm("lang", lang.code)
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find(".deleteButton").size === 0
      }}
    }
  }
}

