package controllers

import play.api._
import db.DB
import play.api.mvc._

import models.{OrderBy, Item, LocaleInfo}
import play.api.Play.current
import controllers.I18n.I18nAware
import helpers.QueryString

object ItemQuery extends Controller with I18nAware with NeedLogin {
  def query(
    qs: List[String], page: Int, pageSize: Int, orderBySpec: String
  ) = Action { implicit request => DB.withConnection { implicit conn => {
    val queryString = if (qs.size == 1) QueryString(qs.head) else QueryString(qs.filter {! _.isEmpty})
    implicit val login = loginSession(request, conn)
    val list = Item.list(None, LocaleInfo.byLang(lang), queryString, page, pageSize, orderBy = OrderBy(orderBySpec))
    Ok(views.html.query("", queryString, list))
  }}}

  def queryByCheckBox(
    page: Int, pageSize: Int
  ) = Action { implicit request => DB.withConnection { implicit conn => {
    implicit val login = loginSession(request, conn)
    request.queryString.get("queryText") match {
      case None =>
        Ok(views.html.queryByCheckBox())
      case Some(seq) =>
        Redirect(routes.ItemQuery.query(seq.toList, page, pageSize))
    }
  }}}

  def queryBySelect(
    page: Int, pageSize: Int
  ) = Action { implicit request => DB.withConnection { implicit conn => {
    implicit val login = loginSession(request, conn)
    request.queryString.get("queryText") match {
      case None =>
        Ok(views.html.queryBySelect())
      case Some(seq) =>
        Redirect(routes.ItemQuery.query(seq.toList, page, pageSize))
    }
  }}}

  def queryByRadio(
    page: Int, pageSize: Int
  ) = Action { implicit request => DB.withConnection { implicit conn => {
    implicit val login = loginSession(request, conn)
    val list = request.queryString.filterKeys {_.startsWith("queryText")}.values.foldLeft(List[String]())(_ ++ _)
    if (list .isEmpty)
      Ok(views.html.queryByRadio())
    else
      Redirect(routes.ItemQuery.query(list, page, pageSize))
    }
  }}
}

