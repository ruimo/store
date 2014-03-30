package functional

import play.api.test._
import play.api.test.Helpers._
import play.api.Play.current
import java.sql.Connection

import functional.Helper._

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

class ItemPicturesSpec extends Specification {
  val dir = Files.createTempDirectory(null)
  lazy val withTempDir = Map("item.picture.path" -> dir.toFile.getAbsolutePath)

  "ItemPicture" should {
    "If specified picture is not found, 'notfound.jpg' will be returned." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTempDir)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        val file = dir.resolve("notfound.jpg")
        Files.write(file, util.Arrays.asList("Hello"), Charset.forName("US-ASCII"))

        downloadString(
          "http://localhost:3333" + controllers.routes.ItemPictures.getPicture(1, 0).url
        )._2 === "Hello"

        Files.delete(file)
      }}
    }

    "If specified detail picture is not found, 'detailnotfound.jpg' will be returned." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTempDir)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        val file = dir.resolve("detailnotfound.jpg")
        Files.write(file, util.Arrays.asList("Hello"), Charset.forName("US-ASCII"))

        downloadString(
          "http://localhost:3333" + controllers.routes.ItemPictures.getDetailPicture(1).url
        )._2 === "Hello"

        Files.delete(file)
      }}
    }

    "If specified picture is found and modified, it will be returned." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTempDir)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        val file = dir.resolve("2_1.jpg")
        Files.write(file, util.Arrays.asList("Hello"), Charset.forName("US-ASCII"))
        downloadString(
          Some(file.toFile.lastModified - 1000),
          "http://localhost:3333" + controllers.routes.ItemPictures.getPicture(2, 1).url
        )._2 === "Hello"

        Files.delete(file)
      }}
    }

    "If specified detail picture is found and modified, it will be returned." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTempDir)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        val file = dir.resolve("detail2.jpg")
        Files.write(file, util.Arrays.asList("Hello"), Charset.forName("US-ASCII"))
        downloadString(
          Some(file.toFile.lastModified - 1000),
          "http://localhost:3333" + controllers.routes.ItemPictures.getDetailPicture(2).url
        )._2 === "Hello"

        Files.delete(file)
      }}
    }

    "If specified picture is found but not modified, 304 will be returned." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTempDir)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        val file = dir.resolve("2_1.jpg")
        Files.write(file, util.Arrays.asList("Hello"), Charset.forName("US-ASCII"))

        downloadString(
          Some(file.toFile.lastModified),
          "http://localhost:3333" + controllers.routes.ItemPictures.getPicture(2, 1).url
        )._1 === Status.NOT_MODIFIED

        downloadString(
          Some(file.toFile.lastModified + 1000),
          "http://localhost:3333" + controllers.routes.ItemPictures.getPicture(2, 1).url
        )._1 === Status.NOT_MODIFIED

        Files.delete(file)
      }}
    }

    "If specified detail picture is found but not modified, 304 will be returned." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTempDir)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        val file = dir.resolve("detail2.jpg")
        Files.write(file, util.Arrays.asList("Hello"), Charset.forName("US-ASCII"))

        downloadString(
          Some(file.toFile.lastModified),
          "http://localhost:3333" + controllers.routes.ItemPictures.getDetailPicture(2).url
        )._1 === Status.NOT_MODIFIED

        downloadString(
          Some(file.toFile.lastModified + 1000),
          "http://localhost:3333" + controllers.routes.ItemPictures.getDetailPicture(2).url
        )._1 === Status.NOT_MODIFIED

        Files.delete(file)
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

        val file = dir.resolve("notfound.jpg")
        Files.write(file, util.Arrays.asList("Hello"), Charset.forName("US-ASCII"))

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
        val now = System.currentTimeMillis
        browser.goTo(
          "http://localhost:3333" + controllers.routes.ItemMaintenance.startChangeItem(item.id.get).url +
          "&lang=" + lang.code
        )
        browser.webDriver
          .findElement(By.id("itemPictureUpload0"))
          .sendKeys(Paths.get("testdata/kinseimaruIdx.jpg").toFile.getAbsolutePath)
        browser.click("#itemPictureUploadSubmit0")

        dir.resolve(item.id.get + "_0.jpg").toFile.exists === true

        // Download file.
        downloadBytes(
          Some(now - 1000),
          "http://localhost:3333" + controllers.routes.ItemPictures.getPicture(item.id.get, 0).url
        )._1 === Status.OK
        
        downloadBytes(
          Some(now + 1000),
          "http://localhost:3333" + controllers.routes.ItemPictures.getPicture(item.id.get, 0).url
        )._1 === Status.NOT_MODIFIED

        // Delete file.
        browser.click("#itemPictureRemove0")

        dir.resolve(item.id.get + "_0.jpg").toFile.exists === false
        
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
        downloadString(
          "http://localhost:3333" + controllers.routes.ItemPictures.getItemAttachment(1, 2, "foo").url
        ) must throwA[FileNotFoundException]
      }}
    }

    "If specified attachment is found and modified, it will be returned." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTempDir)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        Files.createDirectories(dir.resolve("attachments"))
        val file = dir.resolve("attachments").resolve("1_2_file.jpg")
        Files.write(file, util.Arrays.asList("Hello"), Charset.forName("US-ASCII"))
        downloadString(
          Some(file.toFile.lastModified - 1000),
          "http://localhost:3333" + controllers.routes.ItemPictures.getItemAttachment(1, 2, "file.jpg").url
        )._2 === "Hello"

        Files.delete(file)
      }}
    }

    "If specified attachment is found but not modified, 304 will be returned." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTempDir)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        Files.createDirectories(dir.resolve("attachments"))
        val file = dir.resolve("attachments").resolve("1_2_file.jpg")
        Files.write(file, util.Arrays.asList("Hello"), Charset.forName("US-ASCII"))

        downloadString(
          Some(file.toFile.lastModified),
          "http://localhost:3333" + controllers.routes.ItemPictures.getItemAttachment(1, 2, "file.jpg").url
        )._1 === Status.NOT_MODIFIED

        downloadString(
          Some(file.toFile.lastModified + 1000),
          "http://localhost:3333" + controllers.routes.ItemPictures.getItemAttachment(1, 2, "file.jpg").url
        )._1 === Status.NOT_MODIFIED

        Files.delete(file)
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
        Files.createDirectories(dir.resolve("attachments"))
        ItemPictures.retrieveAttachmentNames(1).isEmpty === true
      }}
    }

    "retrieveAttachmentNames returns file names." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTempDir)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        val attachmentDir = dir.resolve("attachments")
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

        Files.delete(attachmentDir.resolve("1_2_file1.jpg"))
        Files.delete(attachmentDir.resolve("2_2_file2.mp3"))
        Files.delete(attachmentDir.resolve("1_3_file3.ogg"))
      }}
    }
  }
}

