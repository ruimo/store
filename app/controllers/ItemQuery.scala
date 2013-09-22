package controllers

import play.api._
import db.DB
import play.api.mvc._

import models.Item
import models.LocaleInfo
import play.api.Play.current
import controllers.I18n.I18nAware

object ItemQuery extends Controller with I18nAware {
  def query(queryString: String) = Action { implicit request => DB.withConnection { implicit conn => {
    val list = Item.list(LocaleInfo.byLang(lang), queryString)
    Ok(views.html.query("", queryString, list))
  }}}
}

