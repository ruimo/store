package controllers

import java.text.{ParseException, SimpleDateFormat}
import java.util.{TimeZone, Locale}
import play.api.Configuration
import java.nio.file.{Path, Files, NoSuchFileException}
import play.api.mvc.{Action, Controller, Call, RequestHeader}
import play.api.i18n.Messages

trait Pictures extends Controller with NeedLogin with HasLogger {
  val CacheDateFormat = new ThreadLocal[SimpleDateFormat]() {
    override def initialValue: SimpleDateFormat = {
      val f = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
      f.setTimeZone(TimeZone.getTimeZone("GMT"))
      f
    }
  }

  def isTesting: Boolean
  def config: Configuration = if (isTesting) configForTesting else configForProduction
  def configForTesting: Configuration = play.api.Play.maybeApplication.map(_.configuration).get
  // Cache config
  lazy val configForProduction = configForTesting
  lazy val picturePathForProduction: Path = picturePathForTesting
  def picturePathForTesting: Path
  def picturePath: Path = if (isTesting) picturePathForTesting else picturePathForProduction

  def attachmentPath: Path = {
    val path = picturePath.resolve("attachments")
    if (! Files.exists(path)) {
      Files.createDirectories(path)
    }
    path
  }

  def notfoundPath =
    if (isTesting) notfoundPathForTesting else notfoundPathForProduction
  // Cache path
  lazy val notfoundPathForProduction: Path = notfoundPathForTesting
  def notfoundPathForTesting: Path = {
    val path = picturePath.resolve("notfound.jpg")
    logger.info("Not found picture '" + path.toAbsolutePath + "' will be used.")
    path
  }

  def uploadPicture(
    id: Long, no: Int, retreat: Long => Call
  ) = Action(parse.multipartFormData) { implicit request =>
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
                retreat(id)
              ).flashing("errorMessage" -> Messages("jpeg.needed"))
            }
            else {
              picture.ref.moveTo(toPath(id, no).toFile, true)
              Redirect(
                retreat(id)
              ).flashing("message" -> Messages("itemIsUpdated"))
            }
          }.getOrElse {
            Redirect(retreat(id)).flashing(
              "errorMessage" -> Messages("file.not.found")
            )
          }.withSession(
            request.session + (LoginUserKey -> user.withExpireTime(System.currentTimeMillis + SessionTimeout).toSessionString)
          )
        }
    }
  }

  def toPath(id: Long, no: Int) = picturePath.resolve(pictureName(id, no))
  def pictureName(id: Long, no: Int) = id + "_" + no + ".jpg"

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

  def removePicture(id: Long, no: Int, retreat: Long => Call) = optIsAuthenticated { implicit optLogin => implicit request =>
    try {
      Files.delete(toPath(id, no))
      notfoundPath.toFile.setLastModified(System.currentTimeMillis)
    }
    catch {
      case e: NoSuchFileException =>
      case e: Throwable => throw e
    }
    Redirect(
      retreat(id)
    ).flashing("message" -> Messages("itemIsUpdated"))
  }
}
