package controllers

import scala.annotation.tailrec
import scala.collection.immutable
import java.io.InputStream
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

  def isTesting = configForTesting.getBoolean("item.picture.fortest").getOrElse(false)
  def config = if (isTesting) configForTesting else configForProduction
  def configForTesting = play.api.Play.maybeApplication.map(_.configuration).get
  // Cache config
  lazy val configForProduction = configForTesting
  def picturePath = if (isTesting) picturePathForTesting else picturePathForProduction
  // Cache path
  lazy val picturePathForProduction = picturePathForTesting
  def picturePathForTesting = {
    val ret = config.getString("item.picture.path").map {
      s => Paths.get(s)
    }.getOrElse {
      Paths.get(System.getProperty("user.home"), "itemPictures")
    }

    logger.info("logger: Using item.picture.path = '" + ret + "'")
    println("Using item.picture.path = '" + ret + "'")
    ret
  }
  def attachmentPath = {
    val path = picturePath.resolve("attachments")
    if (! Files.exists(path)) {
      Files.createDirectories(path)
    }
    path
  }
  def notfoundPath =
    if (isTesting) notfoundPathForTesting else notfoundPathForProduction
  // Cache path
  lazy val notfoundPathForProduction = notfoundPathForTesting
  def notfoundPathForTesting = {
    val path = picturePath.resolve("notfound.jpg")
    logger.info("Not found picture '" + path.toAbsolutePath + "' will be used.")
    path
  }
  def detailNotfoundPath = picturePath.resolve("detailnotfound.jpg")
  lazy val attachmentCount = config.getInt("item.attached.file.count").getOrElse(5)

  def upload(itemId: Long, no: Int) = Action(parse.multipartFormData) { implicit request =>
    retrieveLoginSession(request) match {
      case None => onUnauthorized(request)
      case Some(user) =>
        if (user.isBuyer) onUnauthorized(request)
        else {
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
  }

  def uploadDetail(itemId: Long) = Action(parse.multipartFormData) { implicit request =>
    retrieveLoginSession(request) match {
      case None => onUnauthorized(request)
      case Some(user) =>
        if (user.isBuyer) onUnauthorized(request)
        else {
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
  }

  def uploadItemAttachment(itemId: Long, no: Int) = Action(parse.multipartFormData) { implicit request =>
    retrieveLoginSession(request) match {
      case None => onUnauthorized(request)
      case Some(user) =>
        if (user.isBuyer) onUnauthorized(request)
        else {
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
  }

  def getPicture(itemId: Long, no: Int) = optIsAuthenticated { implicit optLogin => request =>
    val path = getPath(itemId, no)
    if (Files.isReadable(path)) {
      if (isModified(path, request)) readFile(path) else NotModified
    }
    else {
      readPictureFromClasspath(itemId, no)
    }
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
    val byteArray = try {
      source.map(_.toByte).toArray
    }
    finally {
      try {
        source.close()
      }
      catch {
        case t: Throwable => logger.error("Cannot close stream.", t)
      }
    }

    bytesResult(byteArray, contentType, fileName)
  }

  def readPictureFromClasspath(itemId: Long, no: Int, contentType: String = "image/jpeg"): Result = {
    val result = if (config.getBoolean("item.picture.for.demo").getOrElse(false)) {
      val fileName = "public/images/itemPictures/" + itemPictureName(itemId, no)
      readFileFromClasspath(fileName, contentType)
    }
    else Results.NotFound

    if (result == Results.NotFound) {
      readFileFromClasspath("public/images/notfound.jpg", contentType)
    }
    else {
      result
    }
  }

  def readDetailPictureFromClasspath(itemId: Long, contentType: String = "image/jpeg"): Result = {
    val result = if (config.getBoolean("item.picture.for.demo").getOrElse(false)) {
      val fileName = "public/images/itemPictures/" + detailPictureName(itemId)
      readFileFromClasspath(fileName, contentType)
    }
    else Results.NotFound

    if (result == Results.NotFound) {
      readFileFromClasspath("public/images/detailnotfound.jpg", contentType)
    }
    else {
      result
    }
  }

  def readFileFromClasspath(fileName: String, contentType: String): Result = {
    Option(getClass.getClassLoader.getResourceAsStream(fileName)) match {
      case None => Results.NotFound
      case Some(is) =>
        val byteArray = try {
          readFully(is)
        }
      finally {
        try {
          is.close()
        }
        catch {
          case t: Throwable => logger.error("Cannot close stream.", t)
        }
      }

      bytesResult(byteArray, contentType, None)
    }
  }

  def readFully(is: InputStream): Array[Byte] = {
    @tailrec def readFully(size: Int, work: immutable.Vector[(Int, Array[Byte])]): Array[Byte] = {
      val buf = new Array[Byte](64 * 1024)
      val readLen = is.read(buf)
      if (readLen == -1) {
        var offset = 0
        work.foldLeft(new Array[Byte](size)) {
          (ary, e) => 
            System.arraycopy(e._2, 0, ary, offset, e._1)
            offset = offset + e._1
            ary
        }
      }
      else {
        readFully(size + readLen, work :+ (readLen, buf))
      }
    }

    readFully(0, immutable.Vector[(Int, Array[Byte])]())
  }

  def bytesResult(byteArray: Array[Byte], contentType: String, fileName: Option[String]): Result = {
    fileName match {
      case None =>
        Ok(byteArray).as(contentType).withHeaders(
          CACHE_CONTROL -> "max-age=0",
          EXPIRES -> "Mon, 26 Jul 1997 05:00:00 GMT",
          LAST_MODIFIED -> CacheDateFormat.get.format(new java.util.Date(System.currentTimeMillis))
        )

      case Some(fname) =>
        Ok(byteArray).as(contentType).withHeaders(
          CACHE_CONTROL -> "max-age=0",
          EXPIRES -> "Mon, 26 Jul 1997 05:00:00 GMT",
          LAST_MODIFIED -> CacheDateFormat.get.format(new java.util.Date(System.currentTimeMillis)),
          CONTENT_DISPOSITION -> ("attachment; filename=" + fname)
        )
    }
  }

  def detailPictureExists(itemId: Long): Boolean = Files.isReadable(toDetailPath(itemId))

  def getDetailPicture(itemId: Long) = optIsAuthenticated { implicit optLogin => request =>
    val path = getDetailPath(itemId)
    if (Files.isReadable(path)) {
      if (isModified(path, request)) readFile(path) else NotModified
    }
    else {
      readDetailPictureFromClasspath(itemId)
    }
  }

  def getDetailPath(itemId: Long): Path = {
    val path = toDetailPath(itemId)
    if (Files.isReadable(path)) path
    else detailNotfoundPath
  }

  def remove(itemId: Long, no: Int) = optIsAuthenticated { implicit optLogin => implicit request =>
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

  def removeDetail(itemId: Long) = optIsAuthenticated { implicit optLogin => implicit request =>
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

  def removeAttachment(itemId: Long, no: Int, fileName: String) = optIsAuthenticated { implicit optLogin => implicit request =>
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

  def toPath(itemId: Long, no: Int) = picturePath.resolve(itemPictureName(itemId, no))
  def itemPictureName(itemId: Long, no: Int) = itemId + "_" + no + ".jpg"
  def detailPictureName(itemId: Long) = "detail" + itemId + ".jpg"
  def toDetailPath(itemId: Long) = picturePath.resolve(detailPictureName(itemId))
  def toAttachmentPath(itemId: Long, idx: Int, fileName: String) = attachmentPath.resolve(itemId + "_" + idx + "_" + fileName)

  def getItemAttachment(itemId: Long, no: Int, fileName: String) = optIsAuthenticated { implicit optLogin => request =>
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
