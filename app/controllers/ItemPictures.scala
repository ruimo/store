package controllers

import play.api.mvc.Security.Authenticated
import controllers.I18n.I18nAware
import play.api.mvc.Controller
import java.nio.file.{NoSuchFileException, Files, Paths, Path}
import io.Source
import play.api.mvc._
import play.api.i18n.Messages
import java.text.{ParseException, SimpleDateFormat}
import java.util.Locale

object ItemPictures extends Controller with I18nAware with NeedLogin with HasLogger {
  lazy val config = play.api.Play.maybeApplication.map(_.configuration).get
  lazy val picturePath = config.getString("item.picture.path").map {
    s => Paths.get(s)
  }.getOrElse {
    Paths.get(System.getProperty("user.home"), "itemPictures")
  }

  def upload(itemId: Long, no: Int) = Action(parse.multipartFormData) { implicit request =>
    retrieveLoginSession(request) match {
      case None => onUnauthorized(request)
      case Some(user) =>
        request.body.file("picture").map { picture =>
          val filename = picture.filename
          val contentType = picture.contentType
          if (contentType != Some("image/jpeg")) {
            Redirect(
              routes.ItemMaintenance.startChangeItem(itemId)
            ).flashing("errorMessage" -> Messages("jpeg.needed"))
          }
          else {
            picture.ref.moveTo(toPath(itemId, no).toFile, true)
            Redirect(
              routes.ItemMaintenance.startChangeItem(itemId)
            ).flashing("message" -> Messages("itemIsUpdated"))
          }
        }.getOrElse {
          Redirect(routes.ItemMaintenance.startChangeItem(itemId)).flashing(
            "errorMessage" -> Messages("file.not.found")
          )
        }.withSession(
          request.session + (LoginUserKey -> user.withExpireTime(System.currentTimeMillis + SessionTimeout).toSessionString)
        )
    }
  }

  def uploadDetail(itemId: Long) = Action(parse.multipartFormData) { implicit request =>
    retrieveLoginSession(request) match {
      case None => onUnauthorized(request)
      case Some(user) =>
        request.body.file("picture").map { picture =>
          val filename = picture.filename
          val contentType = picture.contentType
          if (contentType != Some("image/jpeg")) {
            Redirect(
              routes.ItemMaintenance.startChangeItem(itemId)
            ).flashing("errorMessage" -> Messages("jpeg.needed"))
          }
          else {
            picture.ref.moveTo(toDetailPath(itemId).toFile, true)
            Redirect(
              routes.ItemMaintenance.startChangeItem(itemId)
            ).flashing("message" -> Messages("itemIsUpdated"))
          }
        }.getOrElse {
          Redirect(routes.ItemMaintenance.startChangeItem(itemId)).flashing(
            "errorMessage" -> Messages("file.not.found")
          )
        }.withSession(
          request.session + (LoginUserKey -> user.withExpireTime(System.currentTimeMillis + SessionTimeout).toSessionString)
        )
    }
  }

  def getPicture(itemId: Long, no: Int) = Action { request =>
    val path = getPath(itemId, no)
    if (isModified(path, request)) readFile(path) else NotModified
  }

  def getPath(itemId: Long, no: Int): Path = {
    val path = toPath(itemId, no)
    if (Files.isReadable(path)) path
    else picturePath.resolve("notfound.jpg")
  }

  def isModified(path: Path, request: RequestHeader): Boolean =
    request.headers.get("If-Modified-Since").flatMap { value =>
      try {
println("request date = " + value)
        val dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
        Some(dateFormat.parse(value))
      }
      catch {
        case e: ParseException => {
          logger.error("Invalid date format '" + value + "'")
          None
        }
      }
    } match {
      case Some(t) => {
println("request date = " + t + "(" + t.getTime + ")")
println("file = " + path)
println("file date = " + path.toFile.lastModified)
        t.getTime < path.toFile.lastModified
      }
      case None => true
    }

  def readFile(path: Path): Result = {
println("timezone = " + java.util.TimeZone.getDefault())
    val source = Source.fromFile(path.toFile)(scala.io.Codec.ISO8859)
    val byteArray = source.map(_.toByte).toArray
    source.close()
    Ok(byteArray).as("image/jpeg").withHeaders(
      LAST_MODIFIED -> String.format(
        "%1$ta, %1$td %1$tb %1$tY %1$tH:%1$tM:%1$tS %1$tZ", 
        java.lang.Long.valueOf(System.currentTimeMillis)
      )
    )
  }

  def getDetailPicture(itemId: Long) = Action { request =>
    val path = getDetailPath(itemId)
    if (isModified(path, request)) readFile(path) else NotModified
  }

  def getDetailPath(itemId: Long): Path = {
    val path = toDetailPath(itemId)
    if (Files.isReadable(path)) path
    else picturePath.resolve("detailnotfound.jpg")
  }

  def remove(itemId: Long, no: Int) = Action { implicit request =>
    try {
      Files.delete(toPath(itemId, no))
    }
    catch {
      case e: NoSuchFileException =>
      case e => throw e
    }
    Redirect(
      routes.ItemMaintenance.startChangeItem(itemId)
    ).flashing("message" -> Messages("itemIsUpdated"))
  }

  def removeDetail(itemId: Long) = Action { implicit request =>
    try {
      Files.delete(toDetailPath(itemId))
    }
    catch {
      case e: NoSuchFileException =>
      case e => throw e
    }
    Redirect(
      routes.ItemMaintenance.startChangeItem(itemId)
    ).flashing("message" -> Messages("itemIsUpdated"))
  }

  def toPath(itemId: Long, no: Int) = picturePath.resolve(itemId + "_" + no + ".jpg")
  def toDetailPath(itemId: Long) = picturePath.resolve("detail" + itemId + ".jpg")
}
