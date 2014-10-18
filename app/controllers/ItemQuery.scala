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
    qs: List[String], page: Int, pageSize: Int, orderBySpec: String, templateNo: Int
  ) = optIsAuthenticated { implicit optLogin => implicit request => DB.withConnection { implicit conn => {
    val queryString = if (qs.size == 1) QueryString(qs.head) else QueryString(qs.filter {! _.isEmpty})

    val list = Item.list(
      locale = LocaleInfo.byLang(lang),
      queryString = queryString,
      page = page,
      pageSize = pageSize,
      orderBy = OrderBy(orderBySpec)
    )
    Ok(
      if (templateNo == 0)
        views.html.query("", queryString, list)
      else
        views.html.queryTemplate("", queryString, list, templateNo)
    )
  }}}

  def queryByCategory(
    qs: List[String], c: Option[Long], page: Int, pageSize: Int, orderBySpec: String, templateNo: Int
  ) = optIsAuthenticated { implicit optLogin => implicit request => DB.withConnection { implicit conn => {
    val queryString = if (qs.size == 1) QueryString(qs.head) else QueryString(qs.filter {! _.isEmpty})

    val list = Item.list(
      locale = LocaleInfo.byLang(lang), 
      queryString = queryString,
      category = c,
      page = page,
      pageSize = pageSize,
      orderBy = OrderBy(orderBySpec)
    )
    Ok(
      if (templateNo == 0)
        views.html.query("", queryString, list)
      else
        views.html.queryTemplate("", queryString, list, templateNo)
    )
  }}}

  def queryByCheckBox(
    page: Int, pageSize: Int, templateNo: Int
  ) = optIsAuthenticated { implicit optLogin => implicit request => DB.withConnection { implicit conn => {
    request.queryString.get("queryText") match {
      case None =>
        Ok(views.html.queryByCheckBox())
      case Some(seq) =>
        Redirect(routes.ItemQuery.query(seq.toList, page, pageSize, templateNo = templateNo))
    }
  }}}

  def queryBySelect(
    page: Int, pageSize: Int, templateNo: Int
  ) = optIsAuthenticated { implicit optLogin => implicit request => DB.withConnection { implicit conn => {
    request.queryString.get("queryText") match {
      case None =>
        Ok(views.html.queryBySelect())
      case Some(seq) =>
        Redirect(routes.ItemQuery.query(seq.toList, page, pageSize, templateNo = templateNo))
    }
  }}}

  def queryByRadio(
    page: Int, pageSize: Int, templateNo: Int
  ) = optIsAuthenticated { implicit optLogin => implicit request => DB.withConnection { implicit conn => {
    val list = request.queryString.filterKeys {_.startsWith("queryText")}.values.foldLeft(List[String]())(_ ++ _)
    if (list .isEmpty)
      Ok(views.html.queryByRadio())
    else
      Redirect(routes.ItemQuery.query(list, page, pageSize, templateNo = templateNo))
    }
  }}
}

