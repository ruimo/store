package controllers

import play.api.mvc.Security.Authenticated
import controllers.I18n.I18nAware
import play.api.mvc.Controller
import java.nio.file.{NoSuchFileException, Files, Paths, Path}
import io.Source
import play.api.mvc._
import play.api.i18n.Messages
import java.text.{ParseException, SimpleDateFormat}
import java.util.{TimeZone, Locale}
import java.util
import scala.collection.JavaConversions._
import collection.immutable.IntMap

object ItemPictures extends Controller with I18nAware with NeedLogin with HasLogger {
  val CacheDateFormat = new ThreadLocal[SimpleDateFormat]() {
    override def initialValue: SimpleDateFormat = {
      val f = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
      f.setTimeZone(TimeZone.getTimeZone("GMT"))
      f
    }
  }

  lazy val config = play.api.Play.maybeApplication.map(_.configuration).get
  lazy val picturePath = config.getString("item.picture.path").map {
    s => Paths.get(s)
  }.getOrElse {
    Paths.get(System.getProperty("user.home"), "itemPictures")
  }
  lazy val attachmentPath = {
    val path = picturePath.resolve("attachments")
    if (! Files.exists(path)) {
      Files.createDirectories(path)
    }
    path
  }
  lazy val notfoundPath = {
    val path = picturePath.resolve("notfound.jpg")
    if (Files.isReadable(path)) {
      logger.info("Not found picture '" + path.toAbsolutePath + "' will be used.")
      path
    }
    else  {
      val p = Paths.get("public/images/notfound.jpg")
      logger.warn("File '" + path.toAbsolutePath + "' not found. '" + p.toAbsolutePath + "' will be used for not found picture instead.")
      p
    }
  }
  lazy val detailNotfoundPath = picturePath.resolve("detailnotfound.jpg")
  lazy val attachmentCount = config.getInt("item.attached.file.count").getOrElse(5)

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

  def uploadItemAttachment(itemId: Long, no: Int) = Action(parse.multipartFormData) { implicit request =>
    retrieveLoginSession(request) match {
      case None => onUnauthorized(request)
      case Some(user) =>
        request.body.file("attachment").map { picture =>
          val fileName = picture.filename
          val contentType = picture.contentType
          picture.ref.moveTo(toAttachmentPath(itemId, no, fileName).toFile, true)
          Redirect(
            routes.ItemMaintenance.startChangeItem(itemId)
          ).flashing("message" -> Messages("itemIsUpdated"))
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
    else notfoundPath
  }

  def isModified(path: Path, request: RequestHeader): Boolean =
    request.headers.get("If-Modified-Since").flatMap { value =>
      try {
        Some(CacheDateFormat.get.parse(value))
      }
      catch {
        case e: ParseException => {
          logger.error("Invalid date format '" + value + "'")
          None
        }
      }
    } match {
      case Some(t) =>
        t.getTime < path.toFile.lastModified
      case None => true
    }

  def readFile(path: Path, contentType: String = "image/jpeg", fileName: Option[String] = None): Result = {
    val source = Source.fromFile(path.toFile)(scala.io.Codec.ISO8859)
    val byteArray = source.map(_.toByte).toArray
    source.close()
    fileName match {
      case None =>
        Ok(byteArray).as(contentType).withHeaders(
          LAST_MODIFIED -> CacheDateFormat.get.format(new java.util.Date(System.currentTimeMillis))
        )

      case Some(fname) =>
        Ok(byteArray).as(contentType).withHeaders(
          LAST_MODIFIED -> CacheDateFormat.get.format(new java.util.Date(System.currentTimeMillis)),
          CONTENT_DISPOSITION -> ("attachment; filename=" + fname)
        )
    }
  }

  def getDetailPicture(itemId: Long) = Action { request =>
    val path = getDetailPath(itemId)
    if (isModified(path, request)) readFile(path) else NotModified
  }

  def getDetailPath(itemId: Long): Path = {
    val path = toDetailPath(itemId)
    if (Files.isReadable(path)) path
    else detailNotfoundPath
  }

  def remove(itemId: Long, no: Int) = Action { implicit request =>
    try {
      Files.delete(toPath(itemId, no))
      notfoundPath.toFile.setLastModified(System.currentTimeMillis)
    }
    catch {
      case e: NoSuchFileException =>
      case e: Throwable => throw e
    }
    Redirect(
      routes.ItemMaintenance.startChangeItem(itemId)
    ).flashing("message" -> Messages("itemIsUpdated"))
  }

  def removeDetail(itemId: Long) = Action { implicit request =>
    try {
      Files.delete(toDetailPath(itemId))
      detailNotfoundPath.toFile.setLastModified(System.currentTimeMillis)
    }
    catch {
      case e: NoSuchFileException =>
      case e: Throwable => throw e
    }
    Redirect(
      routes.ItemMaintenance.startChangeItem(itemId)
    ).flashing("message" -> Messages("itemIsUpdated"))
  }

  def removeAttachment(itemId: Long, no: Int, fileName: String) = Action { implicit request =>
    try {
      Files.delete(toAttachmentPath(itemId, no, fileName))
    }
    catch {
      case e: NoSuchFileException =>
      case e: Throwable => throw e
    }
    Redirect(
      routes.ItemMaintenance.startChangeItem(itemId)
    ).flashing("message" -> Messages("itemIsUpdated"))
  }

  def toPath(itemId: Long, no: Int) = picturePath.resolve(itemId + "_" + no + ".jpg")
  def toDetailPath(itemId: Long) = picturePath.resolve("detail" + itemId + ".jpg")
  def toAttachmentPath(itemId: Long, idx: Int, fileName: String) = attachmentPath.resolve(itemId + "_" + idx + "_" + fileName)

  def getItemAttachment(itemId: Long, no: Int, fileName: String) = Action { request =>
    val path = toAttachmentPath(itemId, no, fileName)
    if (Files.isReadable(path)) {
      if (isModified(path, request)) readFile(path, "application/octet-stream", Some(fileName)) else NotModified
    }
    else {
      NotFound
    }
  }

  def retrieveAttachmentNames(itemId: Long): Map[Int, String] = {
    val stream = Files.newDirectoryStream(attachmentPath, itemId + "_*")
    try {
      stream.foldLeft(IntMap[String]()) { (sum, e) =>
        val restName = e.getFileName.toString.substring((itemId + "_").length)
        val idx = restName.indexOf('_')
        if (idx == -1) sum
        else {
          sum.updated(restName.substring(0, idx).toInt, restName.substring(idx + 1))
        }
      }
    }
    finally {
      try {
        stream.close()
      }
      catch {
        case t: Throwable => t.printStackTrace
      }
    }
  }
}
