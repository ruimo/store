package controllers

import play.api.i18n.Lang
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
import models.RecommendByAdmin

object Recommendation extends Controller with NeedLogin with HasLogger with I18nAware {
  def config = play.api.Play.maybeApplication.map(_.configuration).get
  def maxRecommendCount: Int = config.getInt("recommend.maxCount").getOrElse(5)

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
    implicit conn: Connection, lang: Lang
  ): Seq[JsValue] = {
    val byTransaction: Seq[models.ItemDetail] = RecommendEngine.recommendByItem(
      salesItems
    ).map {
      it => models.ItemDetail.show(it.storeCode.toLong, it.itemCode.toLong, locale)
    }.filter {
      _.siteItemNumericMetadata.get(SiteItemNumericMetadataType.HIDE).map {
        _.metadata != 1
      }.getOrElse(true)
    }

    val byBoth = if (byTransaction.size < maxRecommendCount) {
      byTransaction ++ byAdmin(salesItems, maxRecommendCount - byTransaction.size, locale)
    }
    else {
      byTransaction
    }

    byBoth.map {
      detail => Json.obj(
        "siteId" -> detail.siteId,
        "itemId" -> detail.itemId,
        "name" -> detail.name,
        "siteName" -> detail.siteName,
        "price" -> ViewHelpers.toAmount(detail.price)
      )
    }
  }

  def byAdmin(salesItems: Seq[SalesItem], maxCount: Int, locale: LocaleInfo)(
    implicit conn: Connection, lang: Lang
  ): Seq[models.ItemDetail] = {
    val salesItemsSet = salesItems.map { it => (it.storeCode.toLong, it.itemCode.toLong) }.toSet

    RecommendByAdmin.listByScore(
      showDisabled = false, locale, page = 0, pageSize = salesItems.size + maxCount
    ).records.filter { t =>
      ! salesItemsSet.contains((t._1.siteId, t._1.itemId))
    }.map { t =>
      models.ItemDetail.show(t._1.siteId, t._1.itemId, locale)
    }
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

  def index = isAuthenticated { implicit login => forSuperUser { implicit request =>
    Ok(views.html.admin.recommendationMenu())
  }}
}
