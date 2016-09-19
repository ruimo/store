package controllers

import java.text.{SimpleDateFormat}
import java.util.{TimeZone, Locale}
import play.api.Configuration
import java.nio.file.{Path, Files}

trait Pictures extends HasLogger {
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
}
