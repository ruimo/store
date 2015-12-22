package controllers

import models.WebSnippet
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.mvc.Controller
import controllers.I18n.I18nAware
import play.api.data.Form
import play.api.i18n.Messages
import play.api.db.DB
import play.api.Play.current
import scala.collection.immutable

import play.api.libs.json._

object WebSnippetsMaintenance extends Controller with I18nAware with NeedLogin with HasLogger {
  def index = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    DB.withConnection { implicit conn =>
      val records = WebSnippet.list()

      assumeAdmin(login) {
        Ok(views.html.admin.webSnippetsMaintenance(records))
      }
    }
  }
}
