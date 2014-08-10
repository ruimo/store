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
import com.ruimo.recoeng.json.SalesItem
import models.ShoppingCartItem
import scala.concurrent.Future
import play.api.mvc.Result
import java.sql.Connection

object Recommendation extends Controller with NeedLogin with HasLogger with I18nAware {
  def byItemJson(siteId: Long, itemId: Long) = isAuthenticated { implicit login => implicit request =>
    Async {
      scala.concurrent.Future {
        DB.withConnection { implicit conn =>
          val items = byItems(
            Seq(SalesItem(siteId.toString, itemId.toString, 1)),
            LocaleInfo.byLang(lang)
          )

          Ok(Json.toJson(Map("recommended" -> items)))
        }
      }
    }
  }

  def byItems(
    salesItems: Seq[SalesItem], locale: LocaleInfo
  )(
    implicit conn: Connection
  ): Seq[JsValue] = RecommendEngine.recommendByItem(
    salesItems
  ).map {
    it => models.ItemDetail.show(it.storeCode.toLong, it.itemCode.toLong, locale)
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

  def byShoppingCartJson() = isAuthenticated { implicit login => implicit request =>
    Async {
      scala.concurrent.Future {
        DB.withConnection { implicit conn =>
          val items = byItems(
            ShoppingCartItem.listAllItemsForUser(login.storeUser.id.get),
            LocaleInfo.byLang(lang)
          )
          
          Ok(Json.toJson(Map("recommended" -> items)))
        }
      }
    }
  }
}
