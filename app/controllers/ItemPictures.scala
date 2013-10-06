package controllers

import play.api.mvc.Security.Authenticated
import controllers.I18n.I18nAware
import play.api.mvc.Controller
import java.nio.file.{Files, Paths}
import io.Source
import play.api.mvc._
import play.api.i18n.Messages

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
            picture.ref.moveTo(toPath(itemId, no).toFile)
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
    val path = toPath(itemId, no)
    if (Files.isReadable(path)) {
      val source = Source.fromFile(path.toFile)(scala.io.Codec.ISO8859)
      val byteArray = source.map(_.toByte).toArray
      source.close()
      Ok(byteArray).as("image/jpeg")
    }
    else {
      val source = Source.fromFile(picturePath.resolve("notfound.jpg").toFile)(scala.io.Codec.ISO8859)
      val byteArray = source.map(_.toByte).toArray
      source.close()
      Ok(byteArray).as("image/jpeg")
    }
  }

  def toPath(itemId: Long, no: Int) = picturePath.resolve(itemId + "_" + no + ".jpg")
}
