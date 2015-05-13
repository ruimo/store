package functional

import java.util.concurrent.TimeUnit
import helpers.Helper.disableMailer
import play.api.test.Helpers._
import play.api.Play.current
import helpers.Helper._
import models._
import org.joda.time.format.DateTimeFormat
import play.api.db.DB
import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.i18n.{Lang, Messages}
import java.sql.Date.{valueOf => date}
import LocaleInfo._
import java.sql.Connection
import com.ruimo.scoins.Scoping._

class PrizeSpec extends Specification {
  val conf = inMemoryDatabase() ++ disableMailer

  "Prize" should {
    "Can show information." in {
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val itemName = "Item01"

        browser.goTo(
          "http://localhost:3333" + controllers.routes.Prize.entry(itemName) + "&lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("prize")

        doWith(browser.find(".prizeInfo")) { e =>
          e.find(".itemName .body").getText === itemName
          e.find(".companyName .body").getText === user.companyName.get
          e.find(".name .body").getText === user.firstName + " " + user.lastName
          e.find(".email .body").getText === user.email
        }

        // Since no address record exists, address fields will be left blank
        doWith(browser.find("#confirmPrizeForm")) { e =>
          e.find("input[name='zip.zip1']").getAttribute("value") === ""
          e.find("input[name='zip.zip2']").getAttribute("value") === ""

          e.find("#address1").getAttribute("value") === ""
          e.find("#address2").getAttribute("value") === ""
          e.find("#address3").getAttribute("value") === ""
          e.find("input[name='address4']").getAttribute("value") === ""
          e.find("input[name='address5']").getAttribute("value") === ""

          e.find("#firstName").getAttribute("value") === user.firstName
          e.find("#lastName").getAttribute("value") === user.lastName

          e.find("#firstNameKana").getAttribute("value") === ""
          e.find("#lastNameKana").getAttribute("value") === ""

          e.find("#tel").getAttribute("value") === ""
          e.find("input[name='command']").getAttribute("value") === "confirm"

          browser.fill("#firstName").`with`("")
          browser.fill("#lastName").`with`("")

          e.find("input.submitButton").click()
        }

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find(".globalErrorMessage").getText === Messages("inputError")
        doWith(browser.find(".prizeInfo")) { e =>
          e.find(".itemName .body").getText === itemName
          e.find(".companyName .body").getText === user.companyName.get
          e.find(".name .body").getText === user.firstName + " " + user.lastName
          e.find(".email .body").getText === user.email
        }

        doWith(browser.find("#confirmPrizeForm")) { e =>
          e.find(".error span", 1).getText === Messages("zipError")
          e.find("#address1_field .error").getText === Messages("error.required")
          e.find("#address2_field .error").getText === Messages("error.required")
          e.find("#firstName_field .error").getText === Messages("error.required")
          e.find("#lastName_field .error").getText === Messages("error.required")
          e.find("#firstNameKana_field .error").getText === Messages("error.required")
          e.find("#lastNameKana_field .error").getText === Messages("error.required")
          e.find("#tel_field .error").getText === Messages("error.number")
        }
        
        browser.fill("input[name='zip.zip1']").`with`("AAA")
        browser.fill("input[name='zip.zip2']").`with`("0082")
        browser.fill("#address1").`with`("ADDRESS01")
        browser.fill("#address2").`with`("ADDRESS02")
        browser.fill("#firstName").`with`("FIRST_NAME")
        browser.fill("#lastName").`with`("LAST_NAME")
        browser.fill("#firstNameKana").`with`("FIRST_NAME_KANA")
        browser.fill("#lastNameKana").`with`("LAST_NAME_KANA")
        browser.fill("#tel").`with`("AAA")
        browser.find("input.submitButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find(".globalErrorMessage").getText === Messages("inputError")

        doWith(browser.find("#confirmPrizeForm")) { e =>
          e.find(".error span", 1).getText === Messages("zipError")
          e.find("#tel_field .error").getText === Messages("error.number")
        }

        browser.fill("input[name='zip.zip1']").`with`("146")
        browser.fill("#tel").`with`("987654321")
        browser.find("#prefecture option[value='13']").click()
        browser.find("#age option[value='30代']").click()
        browser.find("#sex option[value='1']").click()
        browser.find("input.submitButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        doWith(browser.find(".prizePersonInfo")) { e =>
          e.find(".itemName .body").getText === itemName
          e.find(".companyName .body").getText === user.companyName.get
          e.find(".name .body").getText === user.firstName + " " + user.lastName
          e.find(".email .body").getText === user.email
        }

        doWith(browser.find("#submitPrizeForm")) { e =>
          e.find(".firstName .body").getText === "FIRST_NAME"
          e.find("input[name='firstName']").getAttribute("value") === "FIRST_NAME"
          e.find(".lastName .body").getText === "LAST_NAME"
          e.find("input[name='lastName']").getAttribute("value") === "LAST_NAME"
          e.find(".firstNameKana .body").getText === "FIRST_NAME_KANA"
          e.find("input[name='firstNameKana']").getAttribute("value") === "FIRST_NAME_KANA"
          e.find(".lastNameKana .body").getText === "LAST_NAME_KANA"
          e.find("input[name='lastNameKana']").getAttribute("value") === "LAST_NAME_KANA"
          e.find(".zip .body span", 0).getText === "146"
          e.find(".zip .body span", 1).getText === "0082"
          e.find("input[name='zip.zip1']").getAttribute("value") === "146"
          e.find("input[name='zip.zip2']").getAttribute("value") === "0082"
          e.find(".prefecture .body").getText === "東京都"
          e.find(".prefecture input[name='prefecture']").getAttribute("value") === "13"
          e.find(".address1 .body").getText === "ADDRESS01"
          e.find(".address2 .body").getText === "ADDRESS02"
          e.find(".address3 .body").getText === ""
          e.find(".tel .body").getText === "987654321"
          e.find(".age .body").getText === "30代"
          e.find(".age input[name='age']").getAttribute("value") === "30代"
          e.find(".sex .body").getText === "女性"
          e.find(".sex input[name='sex']").getAttribute("value") === "1"
          e.find(".prizeComment .body").getText === ""
          e.find(".prizeComment input[name='comment']").getAttribute("value") === ""
        }

        browser.find("button[value='amend']").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("prize")
        doWith(browser.find(".prizeInfo")) { e =>
          e.find(".itemName .body").getText === itemName
          e.find(".companyName .body").getText === user.companyName.get
          e.find(".name .body").getText === user.firstName + " " + user.lastName
          e.find(".email .body").getText === user.email
        }

        doWith(browser.find("#confirmPrizeForm")) { e =>
          e.find("input[name='zip.zip1']").getAttribute("value") === "146"
          e.find("input[name='zip.zip2']").getAttribute("value") === "0082"

          e.find("#address1").getAttribute("value") === "ADDRESS01"
          e.find("#address2").getAttribute("value") === "ADDRESS02"
          e.find("#address3").getAttribute("value") === ""
          e.find("input[name='address4']").getAttribute("value") === ""
          e.find("input[name='address5']").getAttribute("value") === ""

          e.find("#firstName").getAttribute("value") === "FIRST_NAME"
          e.find("#lastName").getAttribute("value") === "LAST_NAME"

          e.find("#firstNameKana").getAttribute("value") === "FIRST_NAME_KANA"
          e.find("#lastNameKana").getAttribute("value") === "LAST_NAME_KANA"

          e.find("#tel").getAttribute("value") === "987654321"
          e.find("input[name='command']").getAttribute("value") === "confirm"

          browser.fill("#address3").`with`("ADDRESS03")
          browser.fill("#comment").`with`("COMMENT")

          e.find("input.submitButton").click()
        }
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        doWith(browser.find(".prizePersonInfo")) { e =>
          e.find(".itemName .body").getText === itemName
          e.find(".companyName .body").getText === user.companyName.get
          e.find(".name .body").getText === user.firstName + " " + user.lastName
          e.find(".email .body").getText === user.email
        }

        doWith(browser.find("#submitPrizeForm")) { e =>
          e.find(".firstName .body").getText === "FIRST_NAME"
          e.find("input[name='firstName']").getAttribute("value") === "FIRST_NAME"
          e.find(".lastName .body").getText === "LAST_NAME"
          e.find("input[name='lastName']").getAttribute("value") === "LAST_NAME"
          e.find(".firstNameKana .body").getText === "FIRST_NAME_KANA"
          e.find("input[name='firstNameKana']").getAttribute("value") === "FIRST_NAME_KANA"
          e.find(".lastNameKana .body").getText === "LAST_NAME_KANA"
          e.find("input[name='lastNameKana']").getAttribute("value") === "LAST_NAME_KANA"
          e.find(".zip .body span", 0).getText === "146"
          e.find(".zip .body span", 1).getText === "0082"
          e.find("input[name='zip.zip1']").getAttribute("value") === "146"
          e.find("input[name='zip.zip2']").getAttribute("value") === "0082"
          e.find(".prefecture .body").getText === "東京都"
          e.find(".prefecture input[name='prefecture']").getAttribute("value") === "13"
          e.find(".address1 .body").getText === "ADDRESS01"
          e.find(".address2 .body").getText === "ADDRESS02"
          e.find(".address3 .body").getText === "ADDRESS03"
          e.find(".tel .body").getText === "987654321"
          e.find(".age .body").getText === "30代"
          e.find(".age input[name='age']").getAttribute("value") === "30代"
          e.find(".sex .body").getText === "女性"
          e.find(".sex input[name='sex']").getAttribute("value") === "1"
          e.find(".prizeComment .body").getText === "COMMENT"
          e.find(".prizeComment input[name='comment']").getAttribute("value") === "COMMENT"
        }
      }}
    }
  }
}
