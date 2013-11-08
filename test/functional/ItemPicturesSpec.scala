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
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
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
          itemPrice, tax, CurrencyInfo.Jpy, BigDecimal("123"), date("9999-12-31")
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
  }

  def downloadString(urlString: String): (Int, String) = downloadString(None, urlString)

  def download[T](ifModifiedSince: Option[Long], urlString: String)(f: InputStream => T): (Int, T) = {
    val url = new URL(urlString)
    val conn = url.openConnection().asInstanceOf[HttpURLConnection]
    try {
      if (ifModifiedSince.isDefined) conn.setIfModifiedSince(ifModifiedSince.get)
      (conn.getResponseCode, f(conn.getInputStream))
    }
    finally {
      conn.disconnect()
    }
  }

  def downloadString(ifModifiedSince: Option[Long], urlString: String): (Int, String) =
    download(ifModifiedSince, urlString) { is =>
      val br = new BufferedReader(new InputStreamReader(is, "UTF-8"))
      val buf = new StringBuilder()
      readFully(buf, br)
      buf.toString
    }

  def downloadBytes(ifModifiedSince: Option[Long], urlString: String): (Int, Array[Byte]) =
    download(ifModifiedSince, urlString) { is =>
      def reader(buf: ByteArrayOutputStream): ByteArrayOutputStream = {
        val c = is.read
        if (c == -1) buf
        else {
          buf.write(c)
          reader(buf)
        }
      }

      reader(new ByteArrayOutputStream).toByteArray
    }

  def readFully(buf: StringBuilder, br: BufferedReader) {
    val s = br.readLine()
    if (s == null) return
    buf.append(s)
    readFully(buf, br)
  }
}

