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
import models.{CreateNews, News, OrderBy, NewsId}
import org.joda.time.DateTime

object NewsQuery extends Controller with I18nAware with NeedLogin with HasLogger {
  def list(
    page:Int, pageSize:Int, orderBySpec: String
  ) = optIsAuthenticated { implicit optLogin => implicit request =>
    DB.withConnection { implicit conn =>
      Ok(views.html.newsList(News.list(page, pageSize, OrderBy(orderBySpec), System.currentTimeMillis)))
    }
  }

  def pagedList(
    page:Int, pageSize:Int, orderBySpec: String
  ) = optIsAuthenticated { implicit optLogin => implicit request =>
    DB.withConnection { implicit conn =>
      Ok(views.html.newsPagedList(News.list(page, pageSize, OrderBy(orderBySpec), System.currentTimeMillis)))
    }
  }

  def show(id: Long) = optIsAuthenticated { implicit optLogin => implicit request =>
    DB.withConnection { implicit conn =>
      Ok(views.html.news(News(NewsId(id))))
    }
  }
}
