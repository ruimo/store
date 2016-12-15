package controllers

import play.api.Logger
import play.api.i18n.Messages
import play.api.mvc._
import play.api.db.DB
import scala.annotation.tailrec
import controllers.I18n.I18nAware
import java.nio.file.Path
import com.ruimo.scoins.Zip
import java.nio.file.Files
import com.ruimo.csv
import scala.io.{Codec, Source}
import com.ruimo.scoins.LoanPattern._
import scala.util.Try
import models.{ItemCsv, LocaleInfo}
import play.api.Play.current
import java.sql.Connection
import helpers.Cache

object ItemMaintenanceByCsv extends Controller with I18nAware with NeedLogin with HasLogger {
  class TooManyItemsInCsvException extends Exception

  val MaxItemCount: () => Int = Cache.config(
    _.getInt("itemCsvMaxLineCount").getOrElse(100)
  )
  val DefaultCsvCodec = Codec("Windows-31j")
  val CsvFileName = "items.csv"

  def index = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeAdmin(login) {
      Ok(views.html.admin.uploadItemCsv())
    }
  }

  def uploadZip() = Action(parse.multipartFormData) { implicit request =>
    retrieveLoginSession(request) match {
      case None => onUnauthorized(request)
      case Some(user) =>
        if (! user.isAdmin) onUnauthorized(request)
        else {
          request.body.file("zipFile").map { zipFile =>
            val filename = zipFile.filename
            if (zipFile.contentType != Some("application/zip")) {
              Redirect(
                routes.ItemMaintenanceByCsv.index
              ).flashing("errorMessage" -> Messages("zip.needed"))
            }
            else {
              try {
                val recordCount = DB.withConnection { implicit conn =>
                  processItemCsv(zipFile.ref.file.toPath)
                }
                Redirect(
                  routes.ItemMaintenanceByCsv.index
                ).flashing("message" -> Messages("itemCsvSuccess", recordCount))
              }
              catch {
                case e: ItemCsv.InvalidColumnException =>
                  Logger.error("Error in item csv.", e)
                  Redirect(routes.ItemMaintenanceByCsv.index).flashing(
                    "errorMessage" -> Messages("itemcsv.invalid.column", e.lineNo, e.colNo, e.value)
                  )
                case e: ItemCsv.NoLangDefException =>
                  Logger.error("Error in item csv.", e)
                  Redirect(routes.ItemMaintenanceByCsv.index).flashing(
                    "errorMessage" -> Messages("itemcsv.invalid.nolang", e.lineNo)
                  )
                case e: ItemCsv.InvalidSiteException =>
                  Logger.error("Error in item csv.", e)
                  Redirect(routes.ItemMaintenanceByCsv.index).flashing(
                    "errorMessage" -> Messages("itemcsv.invalid.site", e.lineNo)
                  )
                case e: ItemCsv.InvalidCategoryException =>
                  Logger.error("Error in item csv.", e)
                  Redirect(routes.ItemMaintenanceByCsv.index).flashing(
                    "errorMessage" -> Messages("itemcsv.invalid.category", e.lineNo)
                  )
                case e: ItemCsv.InvalidLocaleException =>
                  Logger.error("Error in item csv.", e)
                  Redirect(routes.ItemMaintenanceByCsv.index).flashing(
                    "errorMessage" -> Messages("itemcsv.invalid.locale", e.lineNo)
                  )
                case e: ItemCsv.InvalidTaxException =>
                  Logger.error("Error in item csv.", e)
                  Redirect(routes.ItemMaintenanceByCsv.index).flashing(
                    "errorMessage" -> Messages("itemcsv.invalid.tax", e.lineNo)
                  )
                case e: ItemCsv.InvalidCurrencyException =>
                  Logger.error("Error in item csv.", e)
                  Redirect(routes.ItemMaintenanceByCsv.index).flashing(
                    "errorMessage" -> Messages("itemcsv.invalid.currency", e.lineNo)
                  )
              }
            }
          }.getOrElse {
            Redirect(routes.ItemMaintenanceByCsv.index).flashing(
              "errorMessage" -> Messages("file.not.found")
            )
          }
        }
    }
  }

  def processItemCsv(zip: Path, csvCodec: Codec = DefaultCsvCodec)(implicit conn: Connection): Int = {
    val explodeDir = Files.createTempDirectory(null)
    Zip.explode(zip, explodeDir).map { files =>
      val csvFile: Path = explodeDir.resolve(CsvFileName)
      val processedCount: Try[Int] = using(Source.fromFile(csvFile.toFile)(csvCodec)) { src =>
        val csvLines: Iterator[Try[Seq[String]]] = csv.Parser.parseLines(src.toIterator)
        @tailrec def persist(locale: Option[LocaleInfo], lineNo: Int, itemCount: Int): Int = {
          if (itemCount > MaxItemCount())
            throw new TooManyItemsInCsvException

          if (csvLines.hasNext) {
            val csvLine: Seq[String] = csvLines.next().get
            persist(
              ItemCsv.processOneLine(
                lineNo,
                explodeDir, locale, csvLine.toIterator, conn,
                (itemId, no) => ItemPictures.toPath(itemId.id, no),
                itemId => ItemPictures.toDetailPath(itemId.id)
              ),
              lineNo + 1,
              itemCount + 1
            )
          }
          else lineNo
        }

        persist(None, 1, 0)
      } (_.close())
      processedCount.get - 1
    }.get
  }
}

