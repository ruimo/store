package functional

import play.api.test._
import play.api.test.Helpers._
import play.api.Play.current

import helpers.Helper._

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
import com.ruimo.scoins.Scoping._

class ShippingMaintenanceSpec extends Specification {
  "Shipping fee maintenance" should {
    "Should occur validation error in creating shipping box" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
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
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
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

        // Creating with the same site and item class will cause duplicated error.
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ShippingBoxMaintenance.startCreateShippingBox().url + "?lang=" + lang.code
        )
        
        browser.title === Messages("createNewShippingBoxTitle")
        browser.find("#siteId").find("option[value=\"" + site2.id.get + "\"]").click()

        browser.fill("#itemClass").`with`("1")
        browser.fill("#boxSize").`with`("3")
        browser.fill("#boxName").`with`("BoxName2")
        browser.find("#createNewShippingBoxForm").find("input[type='submit']").click

        browser.find(".globalErrorMessage").getText === Messages("inputError")
        browser.find("#itemClass_field").find("dd.error").getText === Messages("duplicatedItemClass")
      }}      
    }

    "Can edit without records" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ShippingBoxMaintenance.editShippingBox().url + "?lang=" + lang.code
        )
        browser.title === Messages("editShippingBoxTitle")
        browser.find(".norecord").getText === Messages("no.records.found")
      }}
    }

    "Can edit with some records" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
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

    "Can edit one box record with validation error" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
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

    "Can maintenance fee without records" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
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

    "Can maintenance fee with records" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
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

    "Can remove fee record" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
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
        browser.find(".shippingFeeList").find(".body", 0).find(".delete").find("button").click

        // Dialog should be shown.
        browser.await().atMost(10, TimeUnit.SECONDS).until(".ui-dialog-buttonset").areDisplayed()
        // Cancel
        browser.find(".ui-dialog-buttonset").find("button", 1).click()
        browser.find(".shippingFeeList").find(".body").getTexts.size === 2

        browser.find(".shippingFeeList").find(".body", 0).find(".delete").find("button").click
        browser.await().atMost(10, TimeUnit.SECONDS).until(".ui-dialog-buttonset").areDisplayed()
        // do removal
        browser.find(".ui-dialog-buttonset").find("button", 0).click()
        browser.find(".shippingFeeList").find(".body").getTexts.size === 1
      }}
    }

    "Can create fee record" in {
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
        browser.await().atMost(5, TimeUnit.SECONDS).until("#createShippingFeeEntryButton").areDisplayed()
        browser.find("#createShippingFeeEntryButton").click()
        
        // No prefectures are checked.
        browser.find("input:not(:checked)[type='checkbox']").getTexts.size === JapanPrefecture.all.length

        // Check Tokyo and Kanagawa.
        browser.find("input[type='checkbox'][value='" + JapanPrefecture.東京都.code + "']").click()
        browser.find("input[type='checkbox'][value='" + JapanPrefecture.神奈川県.code + "']").click()
        browser.find("#createShippingFeeForm").find("input[type='submit']").click
        browser.title === Messages("shippingFeeMaintenanceTitle")

        doWith(browser.find(".shippingFeeList").find(".body", 0)) { e =>
          e.find(".country").getText === Messages("country.JPN")
          e.find(".prefecture").getText === JapanPrefecture.東京都.toString
          e.find(".shippingFee").getText === "-"
        }

        doWith(browser.find(".shippingFeeList").find(".body", 1)) { e =>
          e.find(".country").getText === Messages("country.JPN")
          e.find(".prefecture").getText === JapanPrefecture.神奈川県.toString
          e.find(".shippingFee").getText === "-"
        }
      }}
    }

    "Show validation error when adding fee" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
        val site2 = Site.createNew(LocaleInfo.Ja, "商店2")
        val box1 = ShippingBox.createNew(site1.id.get, 1, 2, "box1")
        val fee1 = ShippingFee.createNew(box1.id.get, CountryCode.JPN, JapanPrefecture.北海道.code)
        val fee2 = ShippingFee.createNew(box1.id.get, CountryCode.JPN, JapanPrefecture.東京都.code)
        val tax1 = Tax.createNew
        val taxName1 = TaxName.createNew(tax1, LocaleInfo.Ja, "tax01")
        val tax2 = Tax.createNew
        val taxName2 = TaxName.createNew(tax2, LocaleInfo.Ja, "tax02")
        
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ShippingFeeMaintenance.startFeeMaintenanceNow(box1.id.get).url + "&lang=" + lang.code
        )

        browser.title === Messages("shippingFeeMaintenanceTitle")
        // Edit fee for tokyo.
        browser.find(".shippingFeeList").find(".body", 0).find(".edit").find("a").click

        browser.title === Messages("shippingFeeHistoryMaintenanceTitle")
        doWith(browser.find(".shippingFeeHistory").find(".body")) { rec =>
          rec.find(".boxName").getText === "box1"
          rec.find(".country").getText === "日本"
          rec.find(".prefecture").getText === "北海道"
        }
        
        browser.find("#taxId").find("option", 0).getText === "tax01"
        browser.find("#taxId").find("option", 1).getText === "tax02"

        browser.find("#addShippingFeeHistoryButton").click()

        browser.await().atMost(5, TimeUnit.SECONDS).until("#fee_field").areDisplayed()
        browser.find("#fee_field").find(".error").getText === Messages("error.number")
        browser.find("#validUntil_field").find(".error").getText === Messages("error.date")
        browser.find("#costFee_field").find(".error").getTexts.size === 0

        browser.fill("#costFee").`with`("-1")
        browser.find("#addShippingFeeHistoryButton").click()

        browser.await().atMost(5, TimeUnit.SECONDS).until("#fee_field").areDisplayed()
        browser.find("#costFee_field").find(".error").getText === Messages("error.min", 0)
      }}
    }

    "Can add, edit, delete fee" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
        val site2 = Site.createNew(LocaleInfo.Ja, "商店2")
        val box1 = ShippingBox.createNew(site1.id.get, 1, 2, "box1")
        val fee1 = ShippingFee.createNew(box1.id.get, CountryCode.JPN, JapanPrefecture.北海道.code)
        val fee2 = ShippingFee.createNew(box1.id.get, CountryCode.JPN, JapanPrefecture.東京都.code)
        val tax1 = Tax.createNew
        val taxName1 = TaxName.createNew(tax1, LocaleInfo.Ja, "tax01")
        val tax2 = Tax.createNew
        val taxName2 = TaxName.createNew(tax2, LocaleInfo.Ja, "tax02")
        
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ShippingFeeMaintenance.startFeeMaintenanceNow(box1.id.get).url + "&lang=" + lang.code
        )

        browser.title === Messages("shippingFeeMaintenanceTitle")
        // Edit fee for tokyo.
        browser.find(".shippingFeeList").find(".body", 0).find(".edit").find("a").click

        browser.title === Messages("shippingFeeHistoryMaintenanceTitle")
        doWith(browser.find(".shippingFeeHistory").find(".body")) { rec =>
          rec.find(".boxName").getText === "box1"
          rec.find(".country").getText === "日本"
          rec.find(".prefecture").getText === "北海道"
        }
        
        // without cost fee.
        browser.find("#taxId").find("option", 1).click()
        browser.fill("#fee").`with`("123")
        browser.fill("#validUntil").`with`("2015-01-23 11:22:33")

        browser.find("#addShippingFeeHistoryButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).until(".title").areDisplayed()

        browser.title === Messages("shippingFeeHistoryMaintenanceTitle")
        browser.webDriver.findElement(By.id("histories_0_taxId")).findElement(
          By.cssSelector("option[value='" + tax2.id.get + "']")
        ).isSelected === true
        browser.find("#histories_0_fee").getAttribute("value") === "123.00"
        browser.find("#histories_0_costFee").getAttribute("value") === ""
        browser.find("#histories_0_validUntil").getAttribute("value") === "2015-01-23 11:22:33"

        // Remove history.
        browser.find(".removeHistoryButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).until(".title").areDisplayed()
        browser.find("#histories_0_fee").getTexts.size === 0

        // with cost fee.
        browser.find("#taxId").find("option", 1).click()
        browser.fill("#fee").`with`("123")
        browser.fill("#costFee").`with`("100")
        browser.fill("#validUntil").`with`("2015-01-23 11:22:33")

        browser.find("#addShippingFeeHistoryButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).until(".title").areDisplayed()

        browser.title === Messages("shippingFeeHistoryMaintenanceTitle")
        browser.webDriver.findElement(By.id("histories_0_taxId")).findElement(
          By.cssSelector("option[value='" + tax2.id.get + "']")
        ).isSelected === true
        browser.find("#histories_0_fee").getAttribute("value") === "123.00"
        browser.find("#histories_0_costFee").getAttribute("value") === "100.00"
        browser.find("#histories_0_validUntil").getAttribute("value") === "2015-01-23 11:22:33"

        // Can change history.
        browser.find("#histories_0_taxId").find("option[value='" + tax1.id.get + "']").click()
        browser.fill("#histories_0_fee").`with`("234")
        browser.fill("#histories_0_costFee").`with`("")
        browser.fill("#histories_0_validUntil").`with`("2016-01-23 22:33:44")
        browser.find("#updateShippingFeeHistoryButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).until(".title").areDisplayed()
        browser.title === Messages("shippingFeeHistoryMaintenanceTitle")
        
        browser.webDriver.findElement(By.id("histories_0_taxId")).findElement(
          By.cssSelector("option[value='" + tax1.id.get + "']")
        ).isSelected === true
        browser.find("#histories_0_fee").getAttribute("value") === "234.00"
        browser.find("#histories_0_costFee").getAttribute("value") === ""
        browser.find("#histories_0_validUntil").getAttribute("value") === "2016-01-23 22:33:44"

        browser.fill("#histories_0_costFee").`with`("100")
        browser.find("#updateShippingFeeHistoryButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).until(".title").areDisplayed()
        browser.title === Messages("shippingFeeHistoryMaintenanceTitle")
        browser.find("#histories_0_costFee").getAttribute("value") === "100.00"

        // Check fee history.
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ShippingFeeMaintenance.startFeeMaintenanceNow(box1.id.get).url + "&lang=" + lang.code
        )
        browser.title === Messages("shippingFeeMaintenanceTitle")
        doWith(browser.find(".shippingFeeList").find(".body", 0)) { e =>
          e.find(".country").getText === Messages("country.JPN")
          e.find(".prefecture").getText === JapanPrefecture.北海道.toString
          e.find(".shippingFee").getText === "234円"
        }

        // Delete fee history.
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ShippingFeeMaintenance.editHistory(fee1.id.get).url + "&lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).until(".title").areDisplayed()
        browser.title === Messages("shippingFeeHistoryMaintenanceTitle")
        browser.find("button.removeHistoryButton").getTexts.size === 1
        browser.find("button.removeHistoryButton").click()

        browser.await().atMost(5, TimeUnit.SECONDS).until(".title").areDisplayed()
        browser.title === Messages("shippingFeeHistoryMaintenanceTitle")
        browser.find("button.removeHistoryButton").getTexts.size === 0
      }}
    }
  }
}
