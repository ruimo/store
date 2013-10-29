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
import java.nio.file.Files
import java.util
import java.nio.charset.Charset
import java.net.{HttpURLConnection, URL}
import java.io.{BufferedReader, InputStreamReader}
import java.text.SimpleDateFormat

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
        )._1 === 304

        downloadString(
          Some(file.toFile.lastModified + 1000),
          "http://localhost:3333" + controllers.routes.ItemPictures.getPicture(2, 1).url
        )._1 === 304

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
        )._1 === 304

        downloadString(
          Some(file.toFile.lastModified + 1000),
          "http://localhost:3333" + controllers.routes.ItemPictures.getDetailPicture(2).url
        )._1 === 304

        Files.delete(file)
      }}
    }
  }

  def downloadString(urlString: String): (Int, String) = downloadString(None, urlString)

  def downloadString(ifModifiedSince: Option[Long], urlString: String): (Int, String) = {
    val url = new URL(urlString)
    val conn = url.openConnection().asInstanceOf[HttpURLConnection]
    try {
      if (ifModifiedSince.isDefined) conn.setIfModifiedSince(ifModifiedSince.get)
      val is = conn.getInputStream()
      val br = new BufferedReader(new InputStreamReader(is, "UTF-8"))
      val buf = new StringBuilder()
      readFully(buf, br)
      (conn.getResponseCode, buf.toString)
    }
    finally {
      conn.disconnect()
    }
  }

  def readFully(buf: StringBuilder, br: BufferedReader) {
    val s = br.readLine()
    if (s == null) return
    buf.append(s)
    readFully(buf, br)
  }
}

