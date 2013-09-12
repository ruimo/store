package controllers

import play.api._
import play.api.mvc._

import models.Item
import models.LocaleInfo

object ItemQuery extends Controller {
  def query(queryString: String) = Action { implicit request => {
    val list = Item.list(LocaleInfo.byLang(lang), queryString)
    Ok(views.html.query("", queryString, list))
  }}
}

