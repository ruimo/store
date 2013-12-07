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
import models.{TransporterName, Transporter, LocaleInfo, Site}
import org.openqa.selenium.By
import java.util.concurrent.TimeUnit

class TransporterMaintenanceSpec extends Specification {
  "Transporter maitenance" should {
    "Validation error will shown" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        browser.goTo(
          "http://localhost:3333" + controllers.routes.TransporterMaintenance.startCreateNewTransporter().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded();

        browser.find("#createNewTransporterForm").find("input[type='submit']").click

        browser.find(".globalErrorMessage").getText() === Messages("inputError")
        browser.find("#transporterName_field").find("dd.error").getText() === Messages("error.required")
      }}
    }

    "Can create new transporter." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        browser.goTo(
          "http://localhost:3333" + controllers.routes.TransporterMaintenance.startCreateNewTransporter().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded();

        browser.title === Messages("createNewTransporterTitle")
        browser.click("select[id='langId'] option[value='" + LocaleInfo.Ja.id + "']")
        browser.fill("#transporterName").`with`("Transporter01")
        browser.find("#createNewTransporterForm").find("input[type='submit']").click

        browser.await().atMost(5, TimeUnit.SECONDS).until(".message").containsText(Messages("transporterIsCreated"))

        DB.withConnection { implicit conn =>
          val list = Transporter.listWithName
          list.size === 1
          val name = list.head._2.get
          name.localeId === LocaleInfo.Ja.id
          name.transporterName === "Transporter01"
        }
      }}
    }

    "Can list transporters." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        browser.goTo(
          "http://localhost:3333" + controllers.routes.TransporterMaintenance.startCreateNewTransporter().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded();

        browser.webDriver
          .findElement(By.id("langId"))
          .findElement(By.cssSelector("option[value=\"" + LocaleInfo.Ja.id + "\"]")).click()
        browser.fill("#transporterName").`with`("Transporter01")
        browser.find("#createNewTransporterForm").find("input[type='submit']").click

        browser.await().atMost(5, TimeUnit.SECONDS).until(".message").containsText(Messages("transporterIsCreated"))

        browser.goTo(
          "http://localhost:3333" + controllers.routes.TransporterMaintenance.startCreateNewTransporter().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded();

        browser.webDriver
          .findElement(By.id("langId"))
          .findElement(By.cssSelector("option[value=\"" + LocaleInfo.En.id + "\"]")).click()
        browser.fill("#transporterName").`with`("Transporter02")

        browser.find("#createNewTransporterForm").find("input[type='submit']").click

        browser.await().atMost(5, TimeUnit.SECONDS).until(".message").containsText(Messages("transporterIsCreated"))

        var list: Seq[(Transporter, Option[TransporterName])] = null
        DB.withConnection { implicit conn =>
          list = Transporter.listWithName
          list.size === 2
          val name0 = list(0)._2.get
          name0.localeId === LocaleInfo.Ja.id
          name0.transporterName === "Transporter01"
          list(1)._2 === None
        }

        browser.goTo(
          "http://localhost:3333" + controllers.routes.TransporterMaintenance.editTransporter().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded();

        browser.title === Messages("editTransporterTitle")
        browser.find(".transporterTableId", 0).find("a").getText() === list(0)._1.id.get.toString
        browser.find(".transporterTableId", 1).find("a").getText() === list(1)._1.id.get.toString

        browser.find(".transporterTableName", 0).getText() === list(0)._2.get.transporterName
        browser.find(".transporterTableName", 1).getText() === "-"
      }}
    }

    "Can change transporter name." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        DB.withConnection { implicit conn =>
          implicit val lang = Lang("ja")
          val user = loginWithTestUser(browser)

          browser.goTo(
            "http://localhost:3333" + controllers.routes.TransporterMaintenance.startCreateNewTransporter().url
            + "?lang=" + lang.code
          )
          browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded();

          browser.click("select[id='langId'] option[value='" + LocaleInfo.Ja.id + "']")
          browser.fill("#transporterName").`with`("Transporter01")
          browser.find("#createNewTransporterForm").find("input[type='submit']").click
          browser.await().atMost(5, TimeUnit.SECONDS).until(".message").containsText(Messages("transporterIsCreated"))
          
          val list = Transporter.listWithName
          val trans = list.head
          
          browser.goTo(
            "http://localhost:3333" + controllers.routes.TransporterMaintenance.startChangeTransporter(trans._1.id.get).url
            + "&lang=" + lang.code
          )
          browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded();

          browser.title === Messages("changeTransporterTitle")
          browser.find(".langName").getText() === Messages("lang." + LocaleInfo.Ja.lang)
          browser.find("#transporterNames_0__transporterName").getValue() === "Transporter01"

          browser.fill("#transporterNames_0__transporterName").`with`("Transporter02")
          browser.find("#changeTransporterName").click()

          browser.title === Messages("changeTransporterTitle")
          browser.await().atMost(5, TimeUnit.SECONDS).until(".message").containsText(Messages("transporterIsUpdated"))
          
          browser.find(".langName").getText() === Messages("lang." + LocaleInfo.Ja.lang)
          browser.find("#transporterNames_0__transporterName").getValue() === "Transporter02"
        }
      }}
    }

    "Validation error when changing transporter name." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        DB.withConnection { implicit conn =>
          implicit val lang = Lang("ja")
          val user = loginWithTestUser(browser)

          browser.goTo(
            "http://localhost:3333" + controllers.routes.TransporterMaintenance.startCreateNewTransporter().url
            + "?lang=" + lang.code
          )
          browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded();

          browser.click("select[id='langId'] option[value='" + LocaleInfo.Ja.id + "']")
          browser.fill("#transporterName").`with`("Transporter01")
          browser.find("#createNewTransporterForm").find("input[type='submit']").click

          browser.await().atMost(5, TimeUnit.SECONDS).until(".message").containsText(Messages("transporterIsCreated"))
          
          val list = Transporter.listWithName
          val trans = list.head
          
          browser.goTo(
            "http://localhost:3333" + controllers.routes.TransporterMaintenance.startChangeTransporter(trans._1.id.get).url
            + "&lang=" + lang.code
          )
          browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded();

          browser.title === Messages("changeTransporterTitle")
          browser.find(".langName").getText() === Messages("lang." + LocaleInfo.Ja.lang)
          browser.find("#transporterNames_0__transporterName").getValue() === "Transporter01"

          browser.fill("#transporterNames_0__transporterName").`with`("")
          browser.find("#changeTransporterName").click()

          browser.title === Messages("changeTransporterTitle")
          browser.find("#transporterNames_0__transporterName_field").find(".error").getText() === Messages("error.required")
        }
      }}
    }

    "Can add transporter name." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        DB.withConnection { implicit conn =>
          implicit val lang = Lang("ja")
          val user = loginWithTestUser(browser)

          browser.goTo(
            "http://localhost:3333" + controllers.routes.TransporterMaintenance.startCreateNewTransporter().url
            + "?lang=" + lang.code
          )
          browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded();

          browser.click("select[id='langId'] option[value='" + LocaleInfo.Ja.id + "']")
          browser.fill("#transporterName").`with`("Transporter01")
          browser.find("#createNewTransporterForm").find("input[type='submit']").click

          browser.await().atMost(5, TimeUnit.SECONDS).until(".message").containsText(Messages("transporterIsCreated"))
       
          val list = Transporter.listWithName
          val trans = list.head
       
          browser.goTo(
            "http://localhost:3333" + controllers.routes.TransporterMaintenance.startChangeTransporter(trans._1.id.get).url
            + "&lang=" + lang.code
          )
          browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded();

          browser.title === Messages("changeTransporterTitle")
          browser.find(".langName").getText() === Messages("lang." + LocaleInfo.Ja.lang)
          browser.find("#transporterNames_0__transporterName").getValue() === "Transporter01"

          browser.fill("#transporterName").`with`("Transporter02")
          browser.click("select[id='localeId'] option[value='" + LocaleInfo.En.id + "']")
          browser.webDriver
            .findElement(By.id("addTransporterName")).click()

          browser.title === Messages("changeTransporterTitle")
//          browser.await().atMost(5, TimeUnit.SECONDS).until(".message").containsText(Messages("transporterIsUpdated"))

          browser.find(".langName", 0).getText() === Messages("lang." + LocaleInfo.Ja.lang)
          browser.find("#transporterNames_0__transporterName").getValue() === "Transporter01"
          browser.find(".langName", 1).getText() === Messages("lang." + LocaleInfo.En.lang)
          browser.find("#transporterNames_1__transporterName").getValue() === "Transporter02"
        }
      }}
    }

    "Can delete transporter name." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        DB.withConnection { implicit conn =>
          implicit val lang = Lang("ja")
          val user = loginWithTestUser(browser)

          browser.goTo(
            "http://localhost:3333" + controllers.routes.TransporterMaintenance.startCreateNewTransporter().url
            + "?lang=" + lang.code
          )
          browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded();

          browser.click("select[id='langId'] option[value='" + LocaleInfo.Ja.id + "']")
          browser.fill("#transporterName").`with`("Transporter01")
          browser.find("#createNewTransporterForm").find("input[type='submit']").click

          browser.await().atMost(5, TimeUnit.SECONDS).until(".message").containsText(Messages("transporterIsCreated"))
       
          val list = Transporter.listWithName
          val trans = list.head
       
          browser.goTo(
            "http://localhost:3333" + controllers.routes.TransporterMaintenance.startChangeTransporter(trans._1.id.get).url
            + "&lang=" + lang.code
          )
          browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded();

          browser.title === Messages("changeTransporterTitle")
          browser.find(".langName").getText() === Messages("lang." + LocaleInfo.Ja.lang)
          browser.find("#transporterNames_0__transporterName").getValue() === "Transporter01"

          browser.fill("#transporterName").`with`("Transporter02")
          browser.click("select[id='localeId'] option[value='" + LocaleInfo.En.id + "']")
          browser.webDriver
            .findElement(By.id("addTransporterName")).click()
       
          browser.title === Messages("changeTransporterTitle")
//          browser.await().atMost(5, TimeUnit.SECONDS).until(".message").containsText(Messages("transporterIsUpdated"))

          browser.find(".langName", 0).getText() === Messages("lang." + LocaleInfo.Ja.lang)
          browser.find("#transporterNames_0__transporterName").getValue() === "Transporter01"
          browser.find(".langName", 1).getText() === Messages("lang." + LocaleInfo.En.lang)
          browser.find("#transporterNames_1__transporterName").getValue() === "Transporter02"

          browser.find(".removeTransporterName", 0).click();
          browser.title === Messages("changeTransporterTitle")

          browser.await().atMost(5, TimeUnit.SECONDS).until(".message").containsText(Messages("transporterIsUpdated"))
       
          browser.find(".langName", 0).getText() === Messages("lang." + LocaleInfo.En.lang)
          browser.find("#transporterNames_0__transporterName").getValue() === "Transporter02"
        }
      }}
    }
  }
}
