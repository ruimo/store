package controllers

import io.Source
import java.text.{ParseException, SimpleDateFormat}
import java.util.{TimeZone, Locale}
import play.api.Configuration
import java.nio.file.{Path, Files, NoSuchFileException, Paths}
import play.api.mvc.{Action, Controller, Call, RequestHeader, Result, Results}
import play.api.i18n.Messages

object NewsPictures extends Controller with NeedLogin with HasLogger with Pictures {
  def isTesting = configForTesting.getBoolean("news.picture.fortest").getOrElse(false)
  def picturePathForTesting: Path = {
    val ret = config.getString("news.picture.path").map {
      s => Paths.get(s)
    }.getOrElse {
      Paths.get(System.getProperty("user.home"), "newsPictures")
    }

    logger.info("Using news.picture.path = '" + ret + "'")
    ret
  }
  def onPictureNotFound(id: Long, no: Int): Result = Results.NotFound

  def upload(id: Long, no: Int) = uploadPicture(id, no, routes.NewsMaintenance.modifyNewsStart(_))
  def remove(id: Long, no: Int) = removePicture(id, no, routes.NewsMaintenance.modifyNewsStart(_))
}
