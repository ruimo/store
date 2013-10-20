package controllers

import play.api._
import db.DB
import play.api.mvc._

import models.Item
import models.LocaleInfo
import play.api.Play.current
import controllers.I18n.I18nAware

object ItemDetail extends Controller with I18nAware with NeedLogin {
  def show(itemId: Long, siteId: Long) = Action { implicit request => DB.withConnection { implicit conn => {
    Ok("")
  }}}
}
