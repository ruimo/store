package functional

import anorm._
import java.util.concurrent.TimeUnit
import play.api.test._
import play.api.test.Helpers._
import play.api.Play.current
import java.sql.Connection
import helpers.Helper._
import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.i18n.{Lang, Messages}
import models._
import play.api.db.DB
import play.api.test.TestServer
import play.api.test.FakeApplication
import java.sql.Date.{valueOf => date}
import com.ruimo.scoins.Scoping._
import java.sql.Date.{valueOf => date}
import helpers.Helper.disableMailer
import helpers.UrlHelper
import helpers.UrlHelper.fromString

class ItemReserveSpec extends Specification {
  "Item reservation" should {
    "Show reserve button" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ disableMailer)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit def date2milli(d: java.sql.Date) = d.getTime
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site = Site.createNew(LocaleInfo.Ja, "Store01")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "外税")
        val taxHis = TaxHistory.createNew(tax, TaxType.OUTER_TAX, BigDecimal("5"), date("9999-12-31"))
        val item01 = Item.createNew(cat)
        val siteItem01 = SiteItem.createNew(site, item01)
        val itemName01 = ItemName.createNew(item01, Map(LocaleInfo.Ja -> "かえで"))
        val itemDesc01 = ItemDescription.createNew(item01, site, "かえで説明")
        val itemPrice01 = ItemPrice.createNew(item01, site)
        val itemPriceHistory01 = ItemPriceHistory.createNew(
          itemPrice01, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
        )
        val item02 = Item.createNew(cat)
        val siteItem02 = SiteItem.createNew(site, item02)
        val itemName02 = ItemName.createNew(item02, Map(LocaleInfo.Ja -> "もみじ"))
        val itemDesc02 = ItemDescription.createNew(item02, site, "もみじ説明")
        val itemPrice02 = ItemPrice.createNew(item02, site)
        val itemPriceHistory02 = ItemPriceHistory.createNew(
          itemPrice02, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
        )

        SiteItemNumericMetadata.createNew(
          site.id.get, item02.id.get, SiteItemNumericMetadataType.RESERVATION_ITEM, 1
        )
        
        browser.goTo(
          "http://localhost:3333"
          + controllers.routes.ItemQuery.query(
            q = List(), orderBySpec = "item_name.item_name", templateNo = 0
          ).url.addParm("lang", lang.code)
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("item.list")
        doWith(browser.find(".queryItemTable")) { tbl =>
          doWith(tbl.find(".queryItemTableBody", 0)) { tr =>
            tr.find(".queryItemItemName").getText === "かえで"
            tr.find("button.addToCartButton").getText === Messages("purchase")
          }

          doWith(tbl.find(".queryItemTableBody", 1)) { tr =>
            tr.find(".queryItemItemName").getText === "もみじ"
            doWith(tr.find("button.reserveButton")) { btn =>
              btn.getText === Messages("itemReservation")
              btn.click()
            }
          }
        }
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("itemReservation")
        doWith(browser.find(".itemReservation")) { tbl =>
          tbl.find(".siteName.body").getText === site.name
          tbl.find(".itemName.body").getText === "もみじ"
        }

        doWith(browser.find("#itemReservationForm")) { form =>
          form.find("#siteId").getAttribute("value") === site.id.get.toString
          form.find("#itemId").getAttribute("value") === item02.id.get.id.toString
          form.find("#name").getAttribute("value") === user.fullName
          form.find("#email").getAttribute("value") === user.email
        }

        browser.goTo(
          "http://localhost:3333"
          + controllers.routes.ItemQuery.query(q = List(), templateNo = 1).url + "&lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("item.list")
        doWith(browser.find(".queryItemTable")) { tbl =>
          doWith(tbl.find(".queryItemTableBody", 0)) { tr =>
            tr.find(".queryItemItemName").getAttribute("textContent").trim === "かえで"
            tr.find("button.addToCartButton").getAttribute("textContent").trim === Messages("purchase")
          }

          doWith(tbl.find(".queryItemTableBody", 1)) { tr =>
            tr.find(".queryItemItemName").getAttribute("textContent").trim === "もみじ"
            tr.find("button.reserveButton").getAttribute("textContent").trim === Messages("itemReservation")
          }
        }

        browser.goTo(
          "http://localhost:3333"
          + controllers.routes.ItemDetail.show(item02.id.get.id, site.id.get).url + "&lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("item.detail")
        doWith(browser.find("button.reserveButton")) { btn =>
          btn.getText === Messages("itemReservation")
          btn.click()
        }
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("itemReservation")
        doWith(browser.find("#itemReservationForm")) { form =>
          form.find("#siteId").getAttribute("value") === site.id.get.toString
          form.find("#itemId").getAttribute("value") === item02.id.get.id.toString
          form.find("#name").getAttribute("value") === user.fullName
          form.find("#email").getAttribute("value") === user.email
        }

        SiteItemNumericMetadata.createNew(
          site.id.get, item02.id.get, SiteItemNumericMetadataType.ITEM_DETAIL_TEMPLATE, 1
        )
        browser.goTo(
          "http://localhost:3333"
          + controllers.routes.ItemDetail.show(item02.id.get.id, site.id.get).url + "&lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("item.detail")
        browser.find("button.reserveButton").getAttribute("textContent").trim === Messages("itemReservation")
      }}      
    }

    "Can reserve item without comment" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit def date2milli(d: java.sql.Date) = d.getTime
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)

        val site = Site.createNew(LocaleInfo.Ja, "Store01")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "外税")
        val taxHis = TaxHistory.createNew(tax, TaxType.OUTER_TAX, BigDecimal("5"), date("9999-12-31"))
        val item01 = Item.createNew(cat)
        val siteItem01 = SiteItem.createNew(site, item01)
        val itemName01 = ItemName.createNew(item01, Map(LocaleInfo.Ja -> "かえで"))
        val itemDesc01 = ItemDescription.createNew(item01, site, "かえで説明")
        val itemPrice01 = ItemPrice.createNew(item01, site)
        val itemPriceHistory01 = ItemPriceHistory.createNew(
          itemPrice01, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
        )
        SiteItemNumericMetadata.createNew(
          site.id.get, item01.id.get, SiteItemNumericMetadataType.RESERVATION_ITEM, 1
        )
        browser.goTo(
          "http://localhost:3333"
          + controllers.routes.ItemInquiryReserve.startItemReservation(site.id.get, item01.id.get.id).url
          + "&lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("itemReservation")
        doWith(browser.find(".itemReservation.siteItemContainer")) { tbl =>
          tbl.find(".siteName.body").getText === site.name
          tbl.find(".itemName.body").getText === "かえで"
        }
        doWith(browser.find("#itemReservationForm")) { form =>
          form.find("#siteId").getAttribute("value") === site.id.get.toString
          form.find("#itemId").getAttribute("value") === item01.id.get.id.toString
          form.find("#name").getAttribute("value") === user.fullName
          form.find("#email").getAttribute("value") === user.email
        }

        browser.fill("#name").`with`("")
        browser.fill("#email").`with`("")

        browser.find("#submitItemReservation").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("itemReservation")
        browser.find("#name_field dd.error").getText === Messages("error.required")
        browser.find("#email_field dd.error").getText === Messages("error.required")
        browser.find("#comment_field dd.error").getTexts.size === 0

        // Reserve without comment
        browser.fill("#name").`with`("MyName")
        browser.fill("#email").`with`("email@xxx.xxx")

        browser.find("#submitItemReservation").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("itemReservationConfirm")
        val rec: ItemInquiry = SQL("select * from item_inquiry").as(ItemInquiry.simple.single)

        doWith(browser.find("#submitItemReservationForm")) { form =>
          form.find("#id").getAttribute("value") === rec.id.get.id.toString

          doWith(form.find(".itemInquiry.confirmationTable")) { tbl =>
            tbl.find(".siteName.body").getText === site.name
            tbl.find(".itemName.body").getText === "かえで"
            tbl.find(".name.body").getText === "MyName"
            tbl.find(".email.body").getText === "email@xxx.xxx"
            tbl.find(".message.body").getText === ""
          }
        }
        // amend entry
        browser.find("#amendItemReservation").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("itemReservation")
        browser.find("#siteId").getAttribute("value") === site.id.get.toString
        browser.find("#itemId").getAttribute("value") === item01.id.get.id.toString
        browser.find("#name").getAttribute("value") === "MyName"
        browser.find("#email").getAttribute("value") === "email@xxx.xxx"
        browser.find("#comment").getText === ""
        
        // Confirm error
        browser.fill("#name").`with`("")
        browser.fill("#email").`with`("")
        browser.find("#submitItemReservation").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#name_field dd.error").getText === Messages("error.required")
        browser.find("#email_field dd.error").getText === Messages("error.required")
        browser.find("#comment_field dd.error").getTexts.size === 0

        browser.fill("#name").`with`("MyName2")
        browser.fill("#email").`with`("email2@xxx.xxx")
        browser.find("#submitItemReservation").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#id").getAttribute("value") === rec.id.get.id.toString
        browser.find(".confirmationTable .siteName.body").getText === site.name
        browser.find(".confirmationTable .itemName.body").getText === "かえで"
        browser.find(".confirmationTable .name.body").getText === "MyName2"
        browser.find(".confirmationTable .email.body").getText === "email2@xxx.xxx"
        browser.find(".confirmationTable .message.body").getText === ""
        
        browser.find("#submitItemReservation").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("company.name")

        doWith(SQL("select * from item_inquiry").as(ItemInquiry.simple.single)) { inq =>
          inq.id === rec.id
          inq.siteId === site.id.get
          inq.itemId === item01.id.get
          inq.storeUserId === user.id.get
          inq.inquiryType === ItemInquiryType.RESERVATION
          inq.submitUserName === "MyName2"
          inq.email === "email2@xxx.xxx"
          inq.status === ItemInquiryStatus.SUBMITTED

          doWith(ItemInquiryField(inq.id.get)) { fields =>
            fields.size === 1
            fields('Message) === ""
          }
        }
      }}      
    }

    "Can reserve item with comment" in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit def date2milli(d: java.sql.Date) = d.getTime
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)

        val site = Site.createNew(LocaleInfo.Ja, "Store01")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "外税")
        val taxHis = TaxHistory.createNew(tax, TaxType.OUTER_TAX, BigDecimal("5"), date("9999-12-31"))
        val item01 = Item.createNew(cat)
        val siteItem01 = SiteItem.createNew(site, item01)
        val itemName01 = ItemName.createNew(item01, Map(LocaleInfo.Ja -> "かえで"))
        val itemDesc01 = ItemDescription.createNew(item01, site, "かえで説明")
        val itemPrice01 = ItemPrice.createNew(item01, site)
        val itemPriceHistory01 = ItemPriceHistory.createNew(
          itemPrice01, tax, CurrencyInfo.Jpy, BigDecimal(999), None, BigDecimal("888"), date("9999-12-31")
        )
        SiteItemNumericMetadata.createNew(
          site.id.get, item01.id.get, SiteItemNumericMetadataType.RESERVATION_ITEM, 1
        )
        browser.goTo(
          "http://localhost:3333"
          + controllers.routes.ItemInquiryReserve.startItemReservation(site.id.get, item01.id.get.id).url
          + "&lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("itemReservation")
        doWith(browser.find(".itemReservation.siteItemContainer")) { tbl =>
          tbl.find(".siteName.body").getText === site.name
          tbl.find(".itemName.body").getText === "かえで"
        }
        doWith(browser.find("#itemReservationForm")) { form =>
          form.find("#siteId").getAttribute("value") === site.id.get.toString
          form.find("#itemId").getAttribute("value") === item01.id.get.id.toString
          form.find("#name").getAttribute("value") === user.fullName
          form.find("#email").getAttribute("value") === user.email
        }

        browser.fill("#name").`with`("")
        browser.fill("#email").`with`("")

        browser.find("#submitItemReservation").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("itemReservation")
        browser.find("#name_field dd.error").getText === Messages("error.required")
        browser.find("#email_field dd.error").getText === Messages("error.required")
        browser.find("#comment_field dd.error").getTexts.size === 0

        // Reserve with comment
        browser.fill("#name").`with`("MyName")
        browser.fill("#email").`with`("email@xxx.xxx")
        browser.fill("#comment").`with`("Comment")

        browser.find("#submitItemReservation").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("itemReservationConfirm")
        val rec: ItemInquiry = SQL("select * from item_inquiry").as(ItemInquiry.simple.single)

        doWith(browser.find("#submitItemReservationForm")) { form =>
          form.find("#id").getAttribute("value") === rec.id.get.id.toString

          doWith(form.find(".itemInquiry.confirmationTable")) { tbl =>
            tbl.find(".siteName.body").getText === site.name
            tbl.find(".itemName.body").getText === "かえで"
            tbl.find(".name.body").getText === "MyName"
            tbl.find(".email.body").getText === "email@xxx.xxx"
            tbl.find(".message.body").getText === "Comment"
          }
        }

        // amend entry
        browser.find("#amendItemReservation").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("itemReservation")
        browser.find("#siteId").getAttribute("value") === site.id.get.toString
        browser.find("#itemId").getAttribute("value") === item01.id.get.id.toString
        browser.find("#name").getAttribute("value") === "MyName"
        browser.find("#email").getAttribute("value") === "email@xxx.xxx"
        browser.find("#comment").getText === "Comment"
        
        // Confirm error
        browser.fill("#name").`with`("")
        browser.fill("#email").`with`("")
        browser.find("#submitItemReservation").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#name_field dd.error").getText === Messages("error.required")
        browser.find("#email_field dd.error").getText === Messages("error.required")
        browser.find("#comment_field dd.error").getTexts.size === 0

        browser.fill("#name").`with`("MyName2")
        browser.fill("#email").`with`("email2@xxx.xxx")
        browser.fill("#comment").`with`("Comment2")
        browser.find("#submitItemReservation").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.find("#id").getAttribute("value") === rec.id.get.id.toString
        browser.find(".confirmationTable .siteName.body").getText === site.name
        browser.find(".confirmationTable .itemName.body").getText === "かえで"
        browser.find(".confirmationTable .name.body").getText === "MyName2"
        browser.find(".confirmationTable .email.body").getText === "email2@xxx.xxx"
        browser.find(".confirmationTable .message.body").getText === "Comment2"
        
        browser.find("#submitItemReservation").click()
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("company.name")

        doWith(SQL("select * from item_inquiry").as(ItemInquiry.simple.single)) { inq =>
          inq.id === rec.id
          inq.siteId === site.id.get
          inq.itemId === item01.id.get
          inq.storeUserId === user.id.get
          inq.inquiryType === ItemInquiryType.RESERVATION
          inq.submitUserName === "MyName2"
          inq.email === "email2@xxx.xxx"
          inq.status === ItemInquiryStatus.SUBMITTED

          doWith(ItemInquiryField(inq.id.get)) { fields =>
            fields.size === 1
            fields('Message) === "Comment2"
          }
        }
      }}      
    }
  }
}
