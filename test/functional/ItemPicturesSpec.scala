package functional

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
import controllers.ItemPictures
import java.nio.file.{Paths, Files}
import java.util
import java.nio.charset.Charset
import java.net.{HttpURLConnection, URL}
import java.io._
import java.text.SimpleDateFormat
import java.sql.Date.{valueOf => date}
import play.api.http.Status
import org.openqa.selenium.By
import scala.Some
import play.api.test.TestServer
import play.api.test.FakeApplication
import controllers.NeedLogin

class ItemPicturesSpec extends Specification {
  val testDir = Files.createTempDirectory(null)
  lazy val withTempDir = Map(
    "item.picture.path" -> testDir.toFile.getAbsolutePath,
    "item.picture.fortest" -> true
  )
  lazy val avoidLogin = Map(
    "need.authentication.entirely" -> false
  )

  "ItemPicture" should {
    "If specified picture is not found, 'notfound.jpg' will be returned." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTempDir)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        val file = testDir.resolve("notfound.jpg")
        Files.write(file, util.Arrays.asList("Hello"), Charset.forName("US-ASCII"))

        if (NeedLogin.needAuthenticationEntirely) {
          loginWithTestUser(browser)
        }

        downloadString(
          "http://localhost:3333" + controllers.routes.ItemPictures.getPicture(1, 0).url
        )._2 === "Hello"

        Files.deleteIfExists(file)
      }}
    }

    "If specified detail picture is not found, 'detailnotfound.jpg' will be returned." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTempDir)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        val file = testDir.resolve("detailnotfound.jpg")
        Files.deleteIfExists(file)
        Files.write(file, util.Arrays.asList("Hello"), Charset.forName("US-ASCII"))

        if (NeedLogin.needAuthenticationEntirely) {
          loginWithTestUser(browser)
        }

        downloadString(
          "http://localhost:3333" + controllers.routes.ItemPictures.getDetailPicture(1).url
        )._2 === "Hello"

        Files.deleteIfExists(file)
      }}
    }

    "If specified picture is found and modified, it will be returned." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTempDir)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        val file = testDir.resolve("2_1.jpg")
        Files.deleteIfExists(file)
        Files.write(file, util.Arrays.asList("Hello"), Charset.forName("US-ASCII"))
        if (NeedLogin.needAuthenticationEntirely) {
          loginWithTestUser(browser)
        }

        downloadString(
          Some(file.toFile.lastModified - 1000),
          "http://localhost:3333" + controllers.routes.ItemPictures.getPicture(2, 1).url
        )._2 === "Hello"

        Files.deleteIfExists(file)
      }}
    }

    "If specified detail picture is found and modified, it will be returned." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTempDir)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        val file = testDir.resolve("detail2.jpg")
        Files.deleteIfExists(file)
        Files.write(file, util.Arrays.asList("Hello"), Charset.forName("US-ASCII"))
        if (NeedLogin.needAuthenticationEntirely) {
          loginWithTestUser(browser)
        }

        downloadString(
          Some(file.toFile.lastModified - 1000),
          "http://localhost:3333" + controllers.routes.ItemPictures.getDetailPicture(2).url
        )._2 === "Hello"

        Files.deleteIfExists(file)
      }}
    }

    "If specified picture is found but not modified, 304 will be returned." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTempDir)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        val file = testDir.resolve("2_1.jpg")
        Files.deleteIfExists(file)
        Files.write(file, util.Arrays.asList("Hello"), Charset.forName("US-ASCII"))
        if (NeedLogin.needAuthenticationEntirely) {
          loginWithTestUser(browser)
        }

        downloadString(
          Some(file.toFile.lastModified),
          "http://localhost:3333" + controllers.routes.ItemPictures.getPicture(2, 1).url
        )._1 === Status.NOT_MODIFIED

        downloadString(
          Some(file.toFile.lastModified + 1000),
          "http://localhost:3333" + controllers.routes.ItemPictures.getPicture(2, 1).url
        )._1 === Status.NOT_MODIFIED

        Files.deleteIfExists(file)
      }}
    }

    "If specified detail picture is found but not modified, 304 will be returned." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTempDir)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        val file = testDir.resolve("detail2.jpg")
        Files.deleteIfExists(file)
        Files.write(file, util.Arrays.asList("Hello"), Charset.forName("US-ASCII"))

        if (NeedLogin.needAuthenticationEntirely) {
          loginWithTestUser(browser)
        }
        downloadString(
          Some(file.toFile.lastModified),
          "http://localhost:3333" + controllers.routes.ItemPictures.getDetailPicture(2).url
        )._1 === Status.NOT_MODIFIED

        downloadString(
          Some(file.toFile.lastModified + 1000),
          "http://localhost:3333" + controllers.routes.ItemPictures.getDetailPicture(2).url
        )._1 === Status.NOT_MODIFIED

        Files.deleteIfExists(file)
      }}
    }

    "Upload item picture." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTempDir)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit def date2milli(d: java.sql.Date) = d.getTime
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site = Site.createNew(LocaleInfo.Ja, "Store01")
        val cat = Category.createNew(Map(LocaleInfo.Ja -> "Cat01"))
        val tax = Tax.createNew
        val taxName = TaxName.createNew(tax, LocaleInfo.Ja, "tax01")
        val taxHis = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))
        val item = Item.createNew(cat)

        val itemName = ItemName.createNew(item, Map(LocaleInfo.Ja -> "ItemName01"))
        val itemDescription = ItemDescription.createNew(item, site, "ItemDescription01")

        val itemPrice = ItemPrice.createNew(item, site)
        val itemPriceHis = ItemPriceHistory.createNew(
          itemPrice, tax, CurrencyInfo.Jpy, BigDecimal("123"), BigDecimal("234"), date("9999-12-31")
        )

        val file = testDir.resolve("notfound.jpg")
        Files.deleteIfExists(file)
        Files.write(file, util.Arrays.asList("Hello"), Charset.forName("US-ASCII"))

        if (NeedLogin.needAuthenticationEntirely) {
          loginWithTestUser(browser)
        }
        // Since no item pictures found, notfound.jpg will be obtained.
        downloadString(
          "http://localhost:3333" + controllers.routes.ItemPictures.getPicture(item.id.get, 0).url
        )._2 === "Hello"

        // Set timestamp of 'notfound.jpg' to very old.
        file.toFile.setLastModified(date("1990-01-01"))

        // Of course, the file is not modified.
        downloadString(
          Some(date("2013-01-01")),
          "http://localhost:3333" + controllers.routes.ItemPictures.getPicture(item.id.get, 0).url
        )._1 === Status.NOT_MODIFIED

        // Now upload new file.
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.startChangeItem(item.id.get).url +
          "&lang=" + lang.code
        )
        browser.webDriver
          .findElement(By.id("itemPictureUpload0"))
          .sendKeys(Paths.get("testdata/kinseimaruIdx.jpg").toFile.getAbsolutePath)
        val now = System.currentTimeMillis
        browser.click("#itemPictureUploadSubmit0")

        testDir.resolve(item.id.get + "_0.jpg").toFile.exists === true

        // Download file.
        downloadBytes(
          Some(now - 1000),
          "http://localhost:3333" + controllers.routes.ItemPictures.getPicture(item.id.get, 0).url
        )._1 === Status.OK
        
        downloadBytes(
          Some(now + 5000),
          "http://localhost:3333" + controllers.routes.ItemPictures.getPicture(item.id.get, 0).url
        )._1 === Status.NOT_MODIFIED

        // Delete file.
        browser.click("#itemPictureRemove0")

        testDir.resolve(item.id.get + "_0.jpg").toFile.exists === false
        
        // Download file. 'notfound.jpg' should be obtained.
        // 200 should be returned. Otherwise, browser cache will not be refreshed!
        val str = downloadString(
          Some(now - 1000),
          "http://localhost:3333" + controllers.routes.ItemPictures.getPicture(item.id.get, 0).url
        )
        str._1 === Status.OK
        str._2 === "Hello"
      }}
    }

    "If specified attachment is not found, 404 will be returned." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTempDir)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        if (NeedLogin.needAuthenticationEntirely) {
          loginWithTestUser(browser)
        }
        downloadString(
          "http://localhost:3333" + controllers.routes.ItemPictures.getItemAttachment(1, 2, "foo").url
        ) must throwA[FileNotFoundException]
      }}
    }

    "If specified attachment is found and modified, it will be returned." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTempDir)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        Files.createDirectories(testDir.resolve("attachments"))
        val file = testDir.resolve("attachments").resolve("1_2_file.jpg")
        Files.write(file, util.Arrays.asList("Hello"), Charset.forName("US-ASCII"))
        if (NeedLogin.needAuthenticationEntirely) {
          loginWithTestUser(browser)
        }
        downloadString(
          Some(file.toFile.lastModified - 1000),
          "http://localhost:3333" + controllers.routes.ItemPictures.getItemAttachment(1, 2, "file.jpg").url
        )._2 === "Hello"

        Files.deleteIfExists(file)
      }}
    }

    "If specified attachment is found but not modified, 304 will be returned." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTempDir ++ avoidLogin)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        Files.createDirectories(testDir.resolve("attachments"))
        val file = testDir.resolve("attachments").resolve("1_2_file.jpg")
        Files.write(file, util.Arrays.asList("Hello"), Charset.forName("US-ASCII"))

        downloadString(
          Some(file.toFile.lastModified),
          "http://localhost:3333" + controllers.routes.ItemPictures.getItemAttachment(1, 2, "file.jpg").url
        )._1 === Status.NOT_MODIFIED

        downloadString(
          Some(file.toFile.lastModified + 1000),
          "http://localhost:3333" + controllers.routes.ItemPictures.getItemAttachment(1, 2, "file.jpg").url
        )._1 === Status.NOT_MODIFIED

        Files.deleteIfExists(file)
      }}
    }

    "Attachment count reflects item.attached.file.count settings." in {
      val app = FakeApplication(
        additionalConfiguration = inMemoryDatabase()
      )
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        ItemPictures.attachmentCount === 5
      }}
    }

    "retrieveAttachmentNames returns empty if no files are found." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTempDir)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        Files.createDirectories(testDir.resolve("attachments"))
        ItemPictures.retrieveAttachmentNames(1).isEmpty === true
      }}
    }

    "retrieveAttachmentNames returns file names." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTempDir)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        val attachmentDir = testDir.resolve("attachments")
        Files.createDirectories(attachmentDir)
        Files.write(
          attachmentDir.resolve("1_2_file1.jpg"), util.Arrays.asList("000"), Charset.forName("US-ASCII")
        )
        Files.write(
          attachmentDir.resolve("2_2_file2.mp3"), util.Arrays.asList("111"), Charset.forName("US-ASCII")
        )
        Files.write(
          attachmentDir.resolve("1_3_file3.ogg"), util.Arrays.asList("222"), Charset.forName("US-ASCII")
        )
        
        val map = ItemPictures.retrieveAttachmentNames(1)
        map.size === 2
        map(2) === "file1.jpg"
        map(3) === "file3.ogg"

        Files.deleteIfExists(attachmentDir.resolve("1_2_file1.jpg"))
        Files.deleteIfExists(attachmentDir.resolve("2_2_file2.mp3"))
        Files.deleteIfExists(attachmentDir.resolve("1_3_file3.ogg"))
      }}
    }
  }
}

