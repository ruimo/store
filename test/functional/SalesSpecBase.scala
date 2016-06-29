package functional

import helpers.Helper.disableMailer
import helpers.UrlHelper
import helpers.UrlHelper._
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
import java.util.concurrent.TimeUnit
import java.sql.Connection
import scala.collection.JavaConversions._
import com.ruimo.scoins.Scoping._
import org.fluentlenium.core.domain.{FluentWebElement, FluentList}
import play.api.test.TestBrowser

trait SalesSpecBase {
  implicit val lang = Lang("ja")

  def itemQueryUrl(q: List[String] = List()): String =
    controllers.routes.ItemQuery.query(q = List(), 0, 10).url.addParm("lang", lang.code).toString

  def itemSizeExists = true

  val defaultConf: List[(String, Any)] = List()
}
