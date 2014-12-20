package functional

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
import models.StoreUser
import models.CountryCode
import models.Address
import models.UserAddress
import models.JapanPrefecture

import com.ruimo.scoins.Scoping._

class UserInfoUpdateSpec extends Specification {
  "Use info update" should {
    "Can update user info having no address redocrd" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)

        browser.goTo(
          "http://localhost:3333" + 
          controllers.routes.UserEntry.updateUserInfoStart() +
          "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("updateUserInformation")
        
        browser.find(".submitButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".globalErrorMessage").getText === Messages("inputError")
        browser.find("#currentPassword_field .help-inline").getText === Messages("error.required")
        browser.find("#firstName_field .help-inline").getText === Messages("error.required")
        browser.find("#lastName_field .help-inline").getText === Messages("error.required")
        browser.find("#firstNameKana_field .help-inline").getText === Messages("error.required")
        browser.find("#lastNameKana_field .help-inline").getText === Messages("error.required")
        browser.find("#email_field .help-inline").getText === Messages("error.email")
        browser.find("#zip_field .help-inline").getText === Messages("zipError")
        browser.find("#address1_field .help-inline").getText === Messages("error.required")
        browser.find("#address2_field .help-inline").getText === Messages("error.required")
        browser.find("#address3_field .help-inline").getText === ""
        browser.find("#tel1_field .help-inline").getText === Messages("error.number")

        browser.fill("#currentPassword").`with`("aaaaaaaa")
        browser.fill("#firstName").`with`("first name")
        browser.fill("#lastName").`with`("last name")
        browser.fill("#firstNameKana").`with`("first name kana")
        browser.fill("#lastNameKana").`with`("last name kana")
        browser.fill("#email").`with`("foo")
        browser.fill("#zip_field input[name='zip1']").`with`("12")
        browser.fill("#zip_field input[name='zip2']").`with`("233")
        browser.fill("#address1").`with`("address 1")
        browser.fill("#address2").`with`("address 2")
        browser.fill("#address3").`with`("address 3")
        browser.fill("#tel1").`with`("ABC")

        browser.find(".submitButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#email_field .help-inline").getText === Messages("error.email")
        browser.find("#zip_field .help-inline").getText === Messages("zipError")
        browser.find("#zip_field .help-inline").getText === Messages("zipError")
        browser.find("#tel1_field .help-inline").getText === Messages("error.number")

        browser.fill("#currentPassword").`with`("aaaaaaaa")
        browser.fill("#email").`with`("null@ruimo.com")
        browser.fill("#zip_field input[name='zip1']").`with`("123")
        browser.fill("#zip_field input[name='zip2']").`with`("2334")
        browser.fill("#tel1").`with`("12345678")

        browser.find(".submitButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#currentPassword_field .help-inline").getText === Messages("confirmPasswordDoesNotMatch")
        browser.find("#email").getAttribute("value") === "null@ruimo.com"

        browser.fill("#currentPassword").`with`("password")
        browser.find(".submitButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find(".message").getText === Messages("userInfoIsUpdated")

        DB.withConnection { implicit conn =>
          doWith(StoreUser(user.id.get)) { u =>
            u.userName === "administrator"
            u.firstName === "first name"
            u.middleName === None
            u.lastName === "last name"
            u.email === "null@ruimo.com"
            u.passwordHash === user.passwordHash
            u.salt === user.salt
            u.deleted === user.deleted
            u.companyName === user.companyName
          }

          val userAddress = UserAddress.getByUserId(user.id.get).get
          doWith(Address.byId(userAddress.addressId)) { a =>
            a.countryCode === CountryCode.JPN
            a.firstName === "first name"
            a.middleName === ""
            a.lastName === "last name"
            a.firstNameKana === "first name kana"
            a.lastNameKana === "last name kana"
            a.zip1 === "123"
            a.zip2 === "2334"
            a.zip3 === ""
            a.prefecture === JapanPrefecture.北海道
            a.address1 === "address 1"
            a.address2 === "address 2"
            a.address3 === "address 3"
            a.address4 === ""
            a.address5 === ""
            a.tel1 === "12345678"
            a.tel2 === ""
            a.tel3 === ""
            a.comment === ""
          }
        }
      }}
    }
  }
}
