package functional

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
        }
      }}
    }
  }
}
