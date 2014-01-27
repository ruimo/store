package functional

import play.api.test._
import play.api.test.Helpers._
import play.api.Play.current

import functional.Helper._

import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.test.TestServer
import play.api.test.FakeApplication
import play.api.db.DB
import play.api.i18n.{Messages, Lang}
import models._
import org.openqa.selenium.By
import java.util.concurrent.TimeUnit
import play.api.test.TestServer
import play.api.test.FakeApplication

class ShippingMaintenanceSpec extends Specification {
  "Shipping fee maintenance" should {
    "Validation error in creating shipping box" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
        val site2 = Site.createNew(LocaleInfo.Ja, "商店2")
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ShippingBoxMaintenance.startCreateShippingBox().url + "?lang=" + lang.code
        )

        browser.title === Messages("createNewShippingBoxTitle")
        browser.find("#createNewShippingBoxForm").find("input[type='submit']").click
        
        browser.await().atMost(5, TimeUnit.SECONDS).until(".globalErrorMessage").areDisplayed()
        browser.find(".globalErrorMessage").getText === Messages("inputError")
        browser.find("#itemClass_field").find("dd.error").getText === Messages("error.number")
        browser.find("#boxSize_field").find("dd.error").getText === Messages("error.number")
        browser.find("#boxName_field").find("dd.error").getText === Messages("error.required")

        browser.fill("#itemClass").`with`("a")
        browser.fill("#boxSize").`with`("a")
        browser.find("#itemClass_field").find("dd.error").getText === Messages("error.number")
        browser.find("#boxSize_field").find("dd.error").getText === Messages("error.number")
      }}      
    }

    "Can create shipping box" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
        val site2 = Site.createNew(LocaleInfo.Ja, "商店2")
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ShippingBoxMaintenance.startCreateShippingBox().url + "?lang=" + lang.code
        )

        browser.title === Messages("createNewShippingBoxTitle")
        browser.webDriver
          .findElement(By.id("siteId"))
          .findElement(By.cssSelector("option[value=\"" + site1.id.get + "\"]")).getText === "商店1"
        browser.webDriver
          .findElement(By.id("siteId"))
          .findElement(By.cssSelector("option[value=\"" + site2.id.get + "\"]")).getText === "商店2"
        browser.find("#siteId").find("option[value=\"" + site2.id.get + "\"]").click()

        browser.fill("#itemClass").`with`("1")
        browser.fill("#boxSize").`with`("2")
        browser.fill("#boxName").`with`("BoxName")
        browser.find("#createNewShippingBoxForm").find("input[type='submit']").click

        browser.await().atMost(5, TimeUnit.SECONDS).until(".message").areDisplayed()
        browser.find(".message").getText === Messages("shippingBoxIsCreated")
        
        browser.title === Messages("createNewShippingBoxTitle")
        DB.withConnection { implicit conn =>
          val list = ShippingBox.list(site2.id.get)
          list.size === 1
          doWith(list(0)) { rec =>
            rec.siteId === site2.id.get
            rec.itemClass === 1
            rec.boxSize === 2
            rec.boxName === "BoxName"
          }
        }
      }}      
    }

    "Edit without records" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ShippingBoxMaintenance.editShippingBox().url + "?lang=" + lang.code
        )
        browser.title === Messages("editShippingBoxTitle")
        browser.find(".norecord").getText === Messages("no.records.found")
      }}
    }

    "Edit with some records" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
        val site2 = Site.createNew(LocaleInfo.Ja, "商店2")
        val box1 = ShippingBox.createNew(site1.id.get, 1, 2, "box1")
        val box2 = ShippingBox.createNew(site1.id.get, 3, 4, "box2")
        val box3 = ShippingBox.createNew(site2.id.get, 5, 6, "box3")
        
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ShippingBoxMaintenance.editShippingBox().url + "?lang=" + lang.code
        )
        browser.title === Messages("editShippingBoxTitle")
        browser.find(".shippingBoxTableBodyId", 0).getText === box1.id.get.toString
        browser.find(".shippingBoxTableBodySite", 0).getText === "商店1"
        browser.find(".shippingBoxTableBodyItemClass", 0).getText === "1"
        browser.find(".shippingBoxTableBodyBoxSize", 0).getText === "2"
        browser.find(".shippingBoxTableBodyBoxName", 0).getText === "box1"

        browser.find(".shippingBoxTableBodyId", 1).getText === box2.id.get.toString
        browser.find(".shippingBoxTableBodySite", 1).getText === "商店1"
        browser.find(".shippingBoxTableBodyItemClass", 1).getText === "3"
        browser.find(".shippingBoxTableBodyBoxSize", 1).getText === "4"
        browser.find(".shippingBoxTableBodyBoxName", 1).getText === "box2"

        browser.find(".shippingBoxTableBodyId", 2).getText === box3.id.get.toString
        browser.find(".shippingBoxTableBodySite", 2).getText === "商店2"
        browser.find(".shippingBoxTableBodyItemClass", 2).getText === "5"
        browser.find(".shippingBoxTableBodyBoxSize", 2).getText === "6"
        browser.find(".shippingBoxTableBodyBoxName", 2).getText === "box3"
      }}
    }

    "Edit one box record with validation error" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
        val site2 = Site.createNew(LocaleInfo.Ja, "商店2")
        val box1 = ShippingBox.createNew(site1.id.get, 1, 2, "box1")
        
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ShippingBoxMaintenance.startChangeShippingBox(box1.id.get).url + "&lang=" + lang.code
        )

        browser.title === Messages("changeShippingBoxTitle")
        browser.webDriver
          .findElement(By.id("siteId"))
          .findElement(By.cssSelector("option[value=\"" + site1.id.get + "\"]")).getText === "商店1"

        browser.webDriver
          .findElement(By.id("siteId"))
          .findElement(By.cssSelector("option[value=\"" + site2.id.get + "\"]")).getText === "商店2"

        browser.fill("#itemClass").`with`("")
        browser.fill("#boxSize").`with`("")
        browser.fill("#boxName").`with`("")

        browser.find("#changeShippingBoxForm").find("input[type='submit']").click

        browser.await().atMost(5, TimeUnit.SECONDS).until("dd.error").areDisplayed()
        browser.find("#itemClass_field").find("dd.error").getText === Messages("error.number")
        browser.find("#boxSize_field").find("dd.error").getText === Messages("error.number")
        browser.find("#boxName_field").find("dd.error").getText === Messages("error.required")

        browser.fill("#itemClass").`with`("100")
        browser.fill("#boxSize").`with`("200")
        browser.fill("#boxName").`with`("boxName2")

        browser.find("#changeShippingBoxForm").find("input[type='submit']").click
        browser.await().atMost(10, TimeUnit.SECONDS).until(".title").areDisplayed

        DB.withConnection { implicit conn =>
          val box = ShippingBox(box1.id.get)
          box.itemClass === 100
          box.boxSize === 200
          box.boxName === "boxName2"
        }
      }}
    }

    "Fee maintenance without records" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
        val site2 = Site.createNew(LocaleInfo.Ja, "商店2")
        val box1 = ShippingBox.createNew(site1.id.get, 1, 2, "box1")
        
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ShippingFeeMaintenance.startFeeMaintenanceNow(box1.id.get).url + "&lang=" + lang.code
        )

        browser.title === Messages("shippingFeeMaintenanceTitle")
        browser.find("table.shippingFeeHeader").find(".body").find(".site").getText === "商店1"
        browser.find("table.shippingFeeHeader").find(".body").find(".boxName").getText === "box1"

        browser.find(".shippingFeeList").find(".body").getTexts.size === 0
      }}
    }

    "Fee maintenance with records" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
        val site2 = Site.createNew(LocaleInfo.Ja, "商店2")
        val box1 = ShippingBox.createNew(site1.id.get, 1, 2, "box1")
        val fee1 = ShippingFee.createNew(box1.id.get, CountryCode.JPN, JapanPrefecture.北海道.code)
        val fee2 = ShippingFee.createNew(box1.id.get, CountryCode.JPN, JapanPrefecture.東京都.code)
        
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ShippingFeeMaintenance.startFeeMaintenanceNow(box1.id.get).url + "&lang=" + lang.code
        )

        browser.title === Messages("shippingFeeMaintenanceTitle")
        doWith(browser.find("table.shippingFeeHeader").find(".body")) { e =>
          e.find(".site").getText === "商店1"
          e.find(".boxName").getText === "box1"
        }

        doWith(browser.find(".shippingFeeList").find(".body", 0)) { e =>
          e.find(".country").getText === Messages("country.JPN")
          e.find(".prefecture").getText === JapanPrefecture.北海道.toString
          e.find(".shippingFee").getText === "-"
        }

        doWith(browser.find(".shippingFeeList").find(".body", 1)) { e =>
          e.find(".country").getText === Messages("country.JPN")
          e.find(".prefecture").getText === JapanPrefecture.東京都.toString
          e.find(".shippingFee").getText === "-"
        }
      }}
    }

    "Remove fee record" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
        val site2 = Site.createNew(LocaleInfo.Ja, "商店2")
        val box1 = ShippingBox.createNew(site1.id.get, 1, 2, "box1")
        val fee1 = ShippingFee.createNew(box1.id.get, CountryCode.JPN, JapanPrefecture.北海道.code)
        val fee2 = ShippingFee.createNew(box1.id.get, CountryCode.JPN, JapanPrefecture.東京都.code)
        
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ShippingFeeMaintenance.startFeeMaintenanceNow(box1.id.get).url + "&lang=" + lang.code
        )

        browser.title === Messages("shippingFeeMaintenanceTitle")
        

      }}
    }
  }
}
