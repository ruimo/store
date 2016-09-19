package controllers

import play.api.data.validation.Constraints._
import play.api.data.Form
import play.api.data.Forms._
import controllers.I18n.I18nAware
import play.api.mvc.Controller
import play.api.db.DB
import play.api.i18n.Messages
import play.api.Play.current
import play.api.i18n.Lang
import models.{CreateNews, News, OrderBy}

object NewsMaintenance extends Controller with I18nAware with NeedLogin with HasLogger {
  val createForm = Form(
    mapping(
      "title" -> text.verifying(nonEmpty, maxLength(255)),
      "contents" ->  text.verifying(nonEmpty, maxLength(65535)),
      "releaseDate" -> jodaDate(Messages("news.date.format"))
    )(CreateNews.apply)(CreateNews.unapply)
  )

  def index = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      Ok(views.html.admin.newsMaintenance())
    }
  }

  def startCreateNews = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      Ok(views.html.admin.createNews(createForm))
    }
  }

  def createNews = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      createForm.bindFromRequest.fold(
        formWithErrors => {
          logger.error("Validation error in NewsMaintenance.createNews.")
          BadRequest(views.html.admin.createNews(formWithErrors))
        },
        news => DB.withConnection { implicit conn =>
          news.save()
          Redirect(
            routes.NewsMaintenance.startCreateNews()
          ).flashing("message" -> Messages("newsIsCreated"))
        }
      )
    }
  }

  def editNews(
    page: Int, pageSize: Int, orderBySpec: String
  ) = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      DB.withConnection { implicit conn =>
        Ok(views.html.admin.editNews(News.list(page, pageSize, OrderBy(orderBySpec))))
      }
    }
  }

  def modifyNewsStart(id: Long) = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      Ok("")
    }
  }

  def modifyNews(id: Long) = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      Ok("")
    }
  }

  def deleteNews(id: Long) = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      Ok("")
    }
  }
}
