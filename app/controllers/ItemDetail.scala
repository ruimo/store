package controllers

import play.api._
import db.DB
import libs.json.{JsObject, Json}
import play.api.mvc._

import models.{SiteItemNumericMetadataType, Item, LocaleInfo}
import play.api.Play.current
import controllers.I18n.I18nAware

object ItemDetail extends Controller with I18nAware with NeedLogin {
  def show(itemId: Long, siteId: Long) = optIsAuthenticated { implicit optLogin => implicit request => DB.withConnection { implicit conn => {
    val itemDetail = models.ItemDetail.show(siteId, itemId, LocaleInfo.getDefault)
println("*** detail.siteItemTextMetadata = " + itemDetail.siteItemTextMetadata)
    itemDetail.siteItemNumericMetadata.get(SiteItemNumericMetadataType.ITEM_DETAIL_TEMPLATE) match {
      case None => Ok(views.html.itemDetail(itemDetail))
      case Some(metadata) => 
        if (metadata.metadata == 0) Ok(views.html.itemDetail(itemDetail))
        else Ok(views.html.itemDetailTemplate(metadata.metadata, itemDetail, ItemPictures.retrieveAttachmentNames(itemId)))
    }
  }}}

  def showAsJson(itemId: Long, siteId: Long) = optIsAuthenticated { implicit optLogin => implicit request => DB.withConnection { implicit conn => {
    Ok(asJson(models.ItemDetail.show(siteId, itemId, LocaleInfo.getDefault)))
  }}}

  def asJson(detail: models.ItemDetail): JsObject = Json.obj(
    "name" -> detail.name
  )
}
