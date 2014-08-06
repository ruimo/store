package controllers

import play.api.db.DB
import play.api.mvc.Controller
import controllers.I18n.I18nAware
import helpers.RecommendEngine
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import models.LocaleInfo
import models.SiteItemNumericMetadataType
import play.api.libs.json.{JsValue, Json}
import helpers.ViewHelpers
import play.api.Play.current

object Recommendation extends Controller with NeedLogin with HasLogger with I18nAware {
  def bySingleItemJson(siteId: Long, itemId: Long) = isAuthenticated { implicit login => implicit request =>
    Async {
      scala.concurrent.Future {
        DB.withConnection { implicit conn =>
          val items: Seq[JsValue] = RecommendEngine.recommendBySingleItem(siteId, itemId).map {
            it => models.ItemDetail.show(it.storeCode.toLong, it.itemCode.toLong, LocaleInfo.byLang(lang))
          }.filter {
            _.siteItemNumericMetadata.get(SiteItemNumericMetadataType.HIDE).map {
              _.metadata != 1
            }.getOrElse(true)
          }.map {
            detail => Json.obj(
              "siteId" -> detail.siteId,
              "itemId" -> detail.itemId,
              "name" -> detail.name,
              "siteName" -> detail.siteName,
              "price" -> ViewHelpers.toAmount(detail.price)
            )
          }

          Ok(Json.toJson(Map("recommended" -> items)))
        }
      }
    }
  }
}

