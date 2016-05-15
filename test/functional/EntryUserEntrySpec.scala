package functional

import helpers.UrlHelper
import helpers.UrlHelper._
import controllers.EntryUserEntry
import java.util.concurrent.TimeUnit
import java.sql.Date.{valueOf => date}
import models._
import play.api.test.Helpers._
import play.api.Play.current
import helpers.Helper._
import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.i18n.{Lang, Messages}
import play.api.db.DB
import helpers.Helper.disableMailer
import LocaleInfo._

class EntryUserEntrySpec extends Specification with SalesSpecBase {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  "Entry user entry" should {
    // "User entry button is shown if anonymousUserPurchase is true" in {
    //   val app = FakeApplication(additionalConfiguration = inMemoryDatabase() + (
    //     "anonymousUserPurchase" -> true
    //   ))

    //   running(
    //     TestServer(3333, app), SeleniumHelpers.webDriver(Helpers.FIREFOX)
    //   ) { browser => DB.withConnection { implicit conn =>
    //     implicit val lang = Lang("ja")
    //     val adminUser = loginWithTestUser(browser)
    //     logoff(browser)

    //     val site = Site.createNew(Ja, "店舗1")
    //     val cat = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
    //     val tax = Tax.createNew
    //     val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "内税")
    //     val taxHis = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))
    //     val item = Item.createNew(cat)
    //     val siteItem = SiteItem.createNew(site, item)
    //     val itemName = ItemName.createNew(item, Map(LocaleInfo.Ja -> "かえで"))
    //     val itemDesc = ItemDescription.createNew(item, site, "かえで説明")
    //     val itemPrice = ItemPrice.createNew(item, site)
    //     val itemPriceHistory = ItemPriceHistory.createNew(
    //       itemPrice, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
    //     )

    //     browser.goTo("http://localhost:3333" + itemQueryUrl())
    //     // List price should be shown.
    //     browser.find(".queryItemTableBody .queryItemUnitPrice").getText === "999円"
    //     browser.find(".purchaseButton").click()
    //     browser.await().atMost(5, TimeUnit.SECONDS).until("#registerAsEntryUserButton").areDisplayed()

    //     browser.find("#registerAsEntryUserButton").click()
    //     browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

    //     browser.fill("#userName").`with`("username01")
    //     browser.fill("#password_main").`with`("password01")
    //     browser.fill("#password_confirm").`with`("password01")
    //     browser.fill("input[name='zip1']").`with`("111")
    //     browser.fill("input[name='zip2']").`with`("2222")
    //     browser.find("#prefecture option[value='13']").click()
    //     browser.fill("#address1").`with`("address01")
    //     browser.fill("#address2").`with`("address02")
    //     browser.fill("#address3").`with`("address03")
    //     browser.fill("#tel").`with`("123454321")
    //     browser.fill("#fax").`with`("543212345")
    //     browser.fill("#firstName").`with`("firstname01")
    //     browser.fill("#lastName").`with`("lastname01")
    //     browser.fill("#firstNameKana").`with`("firstNameKana01")
    //     browser.fill("#lastNameKana").`with`("lastNameKana01")
    //     browser.fill("#email").`with`("null@ruimo.com")

    //     browser.find("#submitUserEntry").click()
    //     browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

    //     browser.title === Messages("commonTitle", Messages("shopping.cart"))
    //     DB.withConnection { implicit conn =>
    //       val createdUser: StoreUser = StoreUser.findByUserName("username01").get
    //       createdUser.userName === "username01"
    //       createdUser.firstName === "firstname01"
    //       createdUser.middleName === None
    //       createdUser.lastName === "lastname01"
    //       createdUser.email === "null@ruimo.com"
    //       createdUser.deleted === false
    //       createdUser.userRole === UserRole.ENTRY_USER
    //       createdUser.companyName === None
    //       createdUser.stretchCount === StoreUser.PasswordHashStretchCount()

    //       val ua: UserAddress = UserAddress.getByUserId(createdUser.id.get).get
    //       val addr: Address = Address.byId(ua.addressId)
    //       addr.countryCode === CountryCode.JPN
    //       addr.firstName === "firstname01"
    //       addr.middleName === ""
    //       addr.lastName === "lastname01"
    //       addr.firstNameKana === "firstNameKana01"
    //       addr.lastNameKana === "lastNameKana01"
    //       addr.zip1 === "111"
    //       addr.zip2 === "2222"
    //       addr.zip3 === ""
    //       addr.prefecture === JapanPrefecture.東京都
    //       addr.address1 === "address01"
    //       addr.address2 === "address02"
    //       addr.address3 === "address03"
    //       addr.address4 === ""
    //       addr.address5 === ""
    //       addr.tel1 === "123454321"
    //       addr.tel2 === "543212345"
    //       addr.tel3 === ""
    //       addr.comment === ""
    //       addr.email === "null@ruimo.com"
    //     }

    //     browser.goTo("http://localhost:3333" + itemQueryUrl())
    //     // List price should be shown.
    //     browser.find(".queryItemTableBody .queryItemUnitPrice").getText === "999円"
    //   }}
    // }

    "Anonymous user can be promoted to normal user after transaction end." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ disableMailer + (
        "anonymousUserPurchase" -> true
      ))

      running(
        TestServer(3333, app), SeleniumHelpers.webDriver(Helpers.FIREFOX)
      ) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val adminUser = loginWithTestUser(browser)
        logoff(browser)
        val site = Site.createNew(Ja, "店舗1")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "内税")
        val taxHis = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))
        val item = Item.createNew(cat)
        val siteItem = SiteItem.createNew(site, item)
        val itemName = ItemName.createNew(item, Map(LocaleInfo.Ja -> "かえで"))
        val itemDesc = ItemDescription.createNew(item, site, "かえで説明")
        val itemPrice = ItemPrice.createNew(item, site)
        val itemPriceHistory = ItemPriceHistory.createNew(
          itemPrice, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
        )
        val itemClass = 1L
        SiteItemNumericMetadata.createNew(site.id.get, item.id.get, SiteItemNumericMetadataType.SHIPPING_SIZE, itemClass)

        val box = ShippingBox.createNew(site.id.get, itemClass, 3, "box01")
        val fee = ShippingFee.createNew(box.id.get, CountryCode.JPN, JapanPrefecture.東京都.code())
        val feeHistory = ShippingFeeHistory.createNew(fee.id.get, tax.id.get, BigDecimal(123), Some(100), date("9999-12-31"))

        browser.goTo("http://localhost:3333" + itemQueryUrl())
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find(".purchaseButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).until("#doAnonymousLoginButton").areDisplayed()
        browser.find("#doAnonymousLoginButton").click()
        browser.await().atMost(5, TimeUnit.SECONDS).until(".toEnterShippingAddressInner").areDisplayed()
        browser.find(".toEnterShippingAddressInner a").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.fill("#firstName").`with`("firstName1")
        browser.fill("#lastName").`with`("lastName1")
        browser.fill("#firstNameKana").`with`("firstName1")
        browser.fill("#lastNameKana").`with`("lastName1")
        browser.fill("#email").`with`("null@ruimo.com")
        browser.fill("input[name='zip1']").`with`("111")
        browser.fill("input[name='zip2']").`with`("2222")
        browser.find("#prefecture option[value='13']").click()
        browser.fill("#address1").`with`("address1")
        browser.fill("#address2").`with`("address2")
        browser.fill("#address3").`with`("address3")
        browser.fill("#tel1").`with`("11111111")
        browser.find("#agreeCheck").click()
        browser.find("#submitBtn").click()

        browser.await().atMost(5, TimeUnit.SECONDS).until("#doPaypalWebPayment").areDisplayed()
        browser.find("#doPaypalWebPayment").click()

        browser.await().atMost(5, TimeUnit.SECONDS).until("#paypalWebPaymentSkip").areDisplayed()

        val (paypalTran, tranHeader) = DB.withConnection { implicit conn =>
          val headers: Seq[TransactionLogHeader] = TransactionLogHeader.list()
          headers.size === 1
          headers(0).transactionType === TransactionTypeCode.PAYPAL_WEB_PAYMENT_PLUS

          val paypalTran: TransactionLogPaypalStatus = TransactionLogPaypalStatus.byTransactionId(headers(0).id.get)
          paypalTran.transactionId === headers(0).id.get
          paypalTran.status === PaypalStatus.START
          (paypalTran, headers(0))
        }

        browser.title === Messages("commonTitle", Messages("paypalWebPaymentStartTitle"))
        browser.goTo(
          "http://localhost:3333" + controllers.routes.Paypal.onWebPaymentPlusSuccess(
            tranHeader.id.get, paypalTran.token
          ).url.addParm("lang", lang.code)
        )

        browser.await().atMost(5, TimeUnit.SECONDS).until("#anonymousRegistrationBtn").areDisplayed()
        browser.find("#anonymousRegistrationBtn").click()

        browser.await().atMost(5, TimeUnit.SECONDS).until("#submitUserEntry").areDisplayed()
        browser.find("#submitUserEntry").click()

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find(".globalErrorMessage").getText === Messages("inputError")

        browser.find("#userName_field.error input").size !== 0
        browser.find("#password_main_field.error input").size !== 0

        browser.fill("#userName").`with`("user0001")
        browser.find("#submitUserEntry").click()

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find(".globalErrorMessage").getText === Messages("inputError")

        browser.find("#userName_field.error input").size === 0
        browser.find("#password_main_field.error input").size !== 0

        browser.fill("#userName").`with`("user0001")
        browser.fill("#password_main").`with`("password1234")
        browser.find("#submitUserEntry").click()

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find(".globalErrorMessage").getText === Messages("inputError")

        browser.find("#userName_field.error input").size === 0
        browser.find("#password_main_field.error input").size === 0
        browser.find("#password_confirm_field.error .help-inline").getText === Messages("confirmPasswordDoesNotMatch")

        browser.fill("#userName").`with`("administrator")
        browser.fill("#password_main").`with`("password1234")
        browser.fill("#password_confirm").`with`("password1234")
        browser.find("#submitUserEntry").click()

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find(".globalErrorMessage").getText === Messages("inputError")
        browser.find("#userName_field .help-inline").getText === Messages("userNameIsTaken")

        browser.fill("#userName").`with`("user0001")
        browser.fill("#password_main").`with`("password1234")
        browser.fill("#password_confirm").`with`("password1234")
        browser.find("#submitUserEntry").click()

        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        DB.withConnection { implicit conn =>
          val user = StoreUser.findByUserName("user0001").get
          user.email === "null@ruimo.com"
          user.firstName === "firstName1"
          user.lastName === "lastName1"
          user.userRole === UserRole.ENTRY_USER
        }
      }}
    }
  }
}
