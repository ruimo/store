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
import models.PagedRecords
import models.ItemName
import models.Site

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
    shoppingCartItems: Seq[SalesItem], locale: LocaleInfo
  )(
    implicit conn: Connection, lang: Lang
  ): Seq[JsValue] = {
    val byTransaction: Seq[models.ItemDetail] = RecommendEngine.recommendByItem(
      shoppingCartItems
    ).map {
      it => models.ItemDetail.show(it.storeCode.toLong, it.itemCode.toLong, locale)
    }.filter {
      _.siteItemNumericMetadata.get(SiteItemNumericMetadataType.HIDE).map {
        _.metadata != 1
      }.getOrElse(true)
    }

    val byBoth = if (byTransaction.size < maxRecommendCount) {
      byTransaction ++ byAdmin(shoppingCartItems, maxRecommendCount - byTransaction.size, locale)
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

  def byAdmin(shoppingCartItems: Seq[SalesItem], maxCount: Int, locale: LocaleInfo)(
    implicit conn: Connection, lang: Lang
  ): Seq[models.ItemDetail] = calcByAdmin(
    shoppingCartItems, maxCount, 
    (maxRecordCount: Int) => RecommendByAdmin.listByScore(
      showDisabled = false, locale, page = 0, pageSize = maxRecordCount
    ),
    (siteId: Long, itemId: Long) => models.ItemDetail.show(siteId, itemId, locale)
  )

  def calcByAdmin(
    shoppingCartItems: Seq[SalesItem], maxCount: Int,
    queryRecommendByAdmin: Int => PagedRecords[(RecommendByAdmin, Option[ItemName], Option[Site])],
    queryItemDetail: (Long, Long) => models.ItemDetail
  ): Seq[models.ItemDetail] = {
    val salesItemsSet = shoppingCartItems.map { it => (it.storeCode.toLong, it.itemCode.toLong) }.toSet
    
    queryRecommendByAdmin(shoppingCartItems.size + maxCount).records.filter { t =>
      ! salesItemsSet.contains((t._1.siteId, t._1.itemId))
    }.take(maxCount).map { t =>
      queryItemDetail(t._1.siteId, t._1.itemId)
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
