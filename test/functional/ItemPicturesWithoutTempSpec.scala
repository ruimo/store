package functional

import play.api.test._
import play.api.test.Helpers._
import play.api.Play.current
import java.sql.Connection

import helpers.Helper._

import org.specs2.mutable.Specification
import play.api.test.{ Helpers, TestServer, FakeApplication }
import play.api.i18n.{ Lang, Messages }
import models._
import play.api.db.DB
import play.api.test.TestServer
import play.api.test.FakeApplication
import java.sql.Date.{ valueOf => date }
import controllers.ItemPictures
import java.nio.file.{ Paths, Files }
import java.util
import java.nio.charset.Charset
import java.net.{ HttpURLConnection, URL }
import java.io._
import java.text.SimpleDateFormat
import java.sql.Date.{ valueOf => date }
import play.api.http.Status
import org.openqa.selenium.By
import scala.Some
import play.api.test.TestServer
import play.api.test.FakeApplication

class ItemPicturesWithoutTempSpec extends Specification {
  "ItemPicture" should {
    "If specified picture is not found, 'notfound.jpg' will be returned." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser =>
        DB.withConnection { implicit conn =>
          downloadBytes(Some(-1),
            "http://localhost:3333" + controllers.routes.ItemPictures.getPicture(1, 0).url)._1 === Status.OK
        }
      }
    }
  }
}

