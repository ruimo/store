package controllers

import helpers.{CategoryIdSearchCondition, CategoryCodeSearchCondition}
import play.api._
import db.DB
import libs.json.{JsObject, Json, JsString, JsArray}
import play.api.mvc._

import models.{OrderBy, Item, LocaleInfo, CategoryName}
import play.api.Play.current
import controllers.I18n.I18nAware
import helpers.QueryString

object ItemQuery extends Controller with I18nAware with NeedLogin {
  def query(
    qs: List[String], page: Int, pageSize: Int, orderBySpec: String, templateNo: Int
  ) = optIsAuthenticated { implicit optLogin => implicit request => DB.withConnection { implicit conn => {
    val queryString = if (qs.size == 1) QueryString(qs.head) else QueryString(qs.filter {! _.isEmpty})

    val list = Item.list(
      locale = LocaleInfo.getDefault,
      queryString = queryString,
      page = page,
      pageSize = pageSize,
      orderBy = OrderBy(orderBySpec)
    )
    Ok(
      if (templateNo == 0)
        views.html.query(
          "", queryString, list,
          (newPage, newPageSize, newTemplateNo, newOrderBy) =>
            routes.ItemQuery.query(qs, newPage, newPageSize, newOrderBy, newTemplateNo)
        )
      else
        views.html.queryTemplate(
          "", queryString, list, templateNo,
          (newPage, newPageSize, newTemplateNo, newOrderBy) =>
            routes.ItemQuery.query(qs, newPage, newPageSize, newOrderBy, newTemplateNo)
        )
    )
  }}}

  def queryByCategory(
    qs: List[String], c: Option[Long], page: Int, pageSize: Int, orderBySpec: String, templateNo: Int
  ) = optIsAuthenticated { implicit optLogin => implicit request => DB.withConnection { implicit conn => {
    val queryString = if (qs.size == 1) QueryString(qs.head) else QueryString(qs.filter {! _.isEmpty})

    val list = Item.list(
      locale = LocaleInfo.getDefault, 
      queryString = queryString,
      category = CategoryIdSearchCondition(c.toSeq: _*),
      page = page,
      pageSize = pageSize,
      orderBy = OrderBy(orderBySpec)
    )
    Ok(
      if (templateNo == 0)
        views.html.query(
          "", queryString, list,
          (newPage, newPageSize, newTemplateNo, newOrderBy) =>
            routes.ItemQuery.queryByCategory(qs, c, newPage, newPageSize, newOrderBy, newTemplateNo)
        )
      else
        views.html.queryTemplate(
          "", queryString, list, templateNo,
          (newPage, newPageSize, newTemplateNo, newOrderBy) =>
            routes.ItemQuery.queryByCategory(qs, c, newPage, newPageSize, newOrderBy, newTemplateNo)
        )
    )
  }}}

  def queryBySite(
    qs: List[String], sid: Option[Long], page: Int, pageSize: Int, orderBySpec: String, templateNo: Int
  ) = optIsAuthenticated { implicit optLogin => implicit request => DB.withConnection { implicit conn => {
    val queryString = if (qs.size == 1) QueryString(qs.head) else QueryString(qs.filter {! _.isEmpty})

    val list = Item.list(
      locale = LocaleInfo.getDefault,
      queryString = queryString,
      siteId = sid,
      page = page,
      pageSize = pageSize,
      orderBy = OrderBy(orderBySpec)
    )
    Ok(
      if (templateNo == 0)
        views.html.query(
          "", queryString, list,
          (newPage, newPageSize, newTemplateNo, newOrderBy) =>
            routes.ItemQuery.queryBySite(qs, sid, newPage, newPageSize, newOrderBy, newTemplateNo)
        )
      else
        views.html.queryTemplate(
          "", queryString, list, templateNo,
          (newPage, newPageSize, newTemplateNo, newOrderBy) =>
            routes.ItemQuery.queryBySite(qs, sid, newPage, newPageSize, newOrderBy, newTemplateNo)
        )
    )
  }}}

  def queryBySiteAndCategory(
    qs: List[String], sid: Option[Long], c: Option[Long], page: Int, pageSize: Int, orderBySpec: String, 
    templateNo: Int
  ) = optIsAuthenticated { implicit optLogin => implicit request => DB.withConnection { implicit conn => {
    val queryString = if (qs.size == 1) QueryString(qs.head) else QueryString(qs.filter {! _.isEmpty})

    val list = Item.list(
      locale = LocaleInfo.getDefault,
      queryString = queryString,
      category = CategoryIdSearchCondition(c.toSeq: _*),
      siteId = sid,
      page = page,
      pageSize = pageSize,
      orderBy = OrderBy(orderBySpec)
    )
    Ok(
      if (templateNo == 0)
        views.html.query(
          "", queryString, list,
          (newPage, newPageSize, newTemplateNo, newOrderBy) =>
            routes.ItemQuery.queryBySiteAndCategory(qs, sid, c, newPage, newPageSize, newOrderBy, newTemplateNo)
        )
      else
        views.html.queryTemplate(
          "", queryString, list, templateNo,
          (newPage, newPageSize, newTemplateNo, newOrderBy) =>
            routes.ItemQuery.queryBySiteAndCategory(qs, sid, c, newPage, newPageSize, newOrderBy, newTemplateNo)
        )
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
    if (list.isEmpty)
      Ok(views.html.queryByRadio())
    else
      Redirect(routes.ItemQuery.query(list, page, pageSize, templateNo = templateNo))
    }
  }}

  def queryAdvanced(
    qs: List[String], cs: String, ccs: String, sid: Option[Long], page: Int, pageSize: Int, orderBySpec: String, 
    templateNo: Int
  ) = optIsAuthenticated { implicit optLogin => implicit request => DB.withConnection { implicit conn =>
    Ok(
      views.html.queryAdvanced(
        templateNo,
        routes.ItemQuery.queryAdvancedContent(qs, cs, ccs, sid, page, pageSize, orderBySpec).url
      )
    )
  }}

  def queryAdvancedContent(
    qs: List[String], cs: String, ccs: String, sid: Option[Long], page: Int, pageSize: Int, orderBySpec: String, templateNo: Int
  ) = optIsAuthenticated { implicit optLogin => implicit request => DB.withConnection { implicit conn =>
    val queryString = if (qs.size == 1) QueryString(qs.head) else QueryString(qs.filter {! _.isEmpty})
    val list = Item.list(
      locale = LocaleInfo.getDefault,
      queryString = queryString,
      category = CategoryIdSearchCondition(cs),
      categoryCodes = CategoryCodeSearchCondition(ccs),
      siteId = sid,
      page = page,
      pageSize = pageSize,
      orderBy = OrderBy(orderBySpec)
    )

    Ok(
      views.html.queryAdvancedContent(
        list,
        (newPage, newPageSize, newTemplateNo, newOrderBy) =>
          routes.ItemQuery.queryAdvanced(qs, cs, ccs, sid, newPage, newPageSize, newOrderBy, templateNo)
      )
    )
  }}

  def categoryNameJson = optIsAuthenticatedJson { implicit optLogin => implicit request =>
    request.body.asJson.map { json =>
      val categoryCodes = (json \ "categoryCodes").as[Seq[String]]

      DB.withConnection { implicit conn =>
        val categoryNames: Seq[(String, String)] = 
          CategoryName.categoryNameByCode(categoryCodes, LocaleInfo.getDefault)
        Ok(
          Json.toJson(
            JsObject(
              Seq(
                "categoryNames" -> JsArray(
                  categoryNames.map { n =>
                    JsObject(
                      Seq(
                        "categoryCode" -> JsString(n._1),
                        "categoryName" -> JsString(n._2)
                      )
                    )
                  }
                )
              )
            )
          )
        )
      }
    }.get
  }
}
