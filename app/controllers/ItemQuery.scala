package controllers

import play.api._
import db.DB
import play.api.mvc._

import models.Item
import models.LocaleInfo
import play.api.Play.current
import controllers.I18n.I18nAware

object ItemQuery extends Controller with I18nAware with NeedLogin {
  def query(
    queryString: List[String], page: Int, pageSize: Int
  ) = Action { implicit request => DB.withConnection { implicit conn => {
    implicit val login = loginSession(request, conn)
    val list = Item.list(None, LocaleInfo.byLang(lang), queryString, page, pageSize)
    Ok(views.html.query("", queryString, list))
  }}}

  def queryByCheckBox(
    page: Int, pageSize: Int
  ) = Action { implicit request => DB.withConnection { implicit conn => {
    implicit val login = loginSession(request, conn)
    request.queryString.get("queryCheckBox") match {
      case None =>
        Ok(views.html.queryByCheckBox())
      case Some(seq) =>
        Redirect(routes.ItemQuery.query(seq.toList, page, pageSize))
    }
  }}}
}

