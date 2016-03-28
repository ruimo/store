package controllers

import play.api.i18n.{Lang, Messages}
import play.api._
import db.DB
import libs.json.{JsObject, Json, JsString}
import play.api.mvc._

import models.{SiteItemNumericMetadataType, Item, LocaleInfo}
import play.api.Play.current
import controllers.I18n.I18nAware
import models.{ItemPriceStrategy, ItemPriceStrategyContext}

object ItemDetail extends Controller with I18nAware with NeedLogin {
  def show(itemId: Long, siteId: Long) = optIsAuthenticated { implicit optLogin => implicit request => DB.withConnection { implicit conn => {
    models.ItemDetail.show(
      siteId, itemId, LocaleInfo.getDefault,
      itemPriceStrategy = ItemPriceStrategy(ItemPriceStrategyContext(optLogin))
    ) match {
      case None => Ok(views.html.itemDetailNotFound())
      case Some(itemDetail) =>
        if (itemDetail.siteItemNumericMetadata.get(SiteItemNumericMetadataType.HIDE).map(_.metadata).getOrElse(0) == 1) {
          Ok(views.html.itemDetailNotFound())
        }
        else {
          itemDetail.siteItemNumericMetadata.get(SiteItemNumericMetadataType.ITEM_DETAIL_TEMPLATE) match {
            case None => Ok(views.html.itemDetail(itemDetail))
            case Some(metadata) => 
              if (metadata.metadata == 0) Ok(views.html.itemDetail(itemDetail))
              else Ok(
                views.html.itemDetailTemplate(metadata.metadata, itemDetail, ItemPictures.retrieveAttachmentNames(itemId))
              )
          }
        }
    }
  }}}

  def showAsJson(itemId: Long, siteId: Long) = optIsAuthenticated { implicit optLogin => implicit request => DB.withConnection { implicit conn => {
    Ok(
      asJson(
        models.ItemDetail.show(
          siteId, itemId, LocaleInfo.getDefault,
          itemPriceStrategy = ItemPriceStrategy(ItemPriceStrategyContext(optLogin))
        )
      )
    )
  }}}

  def asJson(detail: Option[models.ItemDetail])(implicit lang: Lang): JsObject = Json.obj(
    "name" -> JsString(
      detail.map { itd =>
        if (itd.siteItemNumericMetadata.get(SiteItemNumericMetadataType.HIDE).map(_.metadata).getOrElse(0) == 1) ""
        else itd.name
      }.getOrElse("")
    )
  )
}
