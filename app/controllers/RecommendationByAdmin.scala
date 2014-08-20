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
import helpers.QueryString
import models._

object RecommendationByAdmin extends Controller with NeedLogin with HasLogger with I18nAware {
  def startEditRecommendByAdmin = isAuthenticated { implicit login => forSuperUser { implicit request =>
    Ok(views.html.admin.recommendationByAdminMenu())
  }}

  def selectItem(
    qs: List[String], pgStart: Int, pgSize: Int, orderBySpec: String
  ) = isAuthenticated { implicit login => forSuperUser { implicit request =>
    val queryStr = if (qs.size == 1) QueryString(qs.head) else QueryString(qs.filter {! _.isEmpty})
    DB.withConnection { implicit conn => {
      val list = Item.listForMaintenance(
        siteUser = None, locale = LocaleInfo.byLang(lang), queryString = queryStr, page = pgStart,
        pageSize = pgSize, orderBy = OrderBy(orderBySpec)
      )

      Ok(views.html.admin.selectItemForRecommendByAdmin(queryStr, list))
    }}
  }}

  def startEdit = isAuthenticated { implicit login => forSuperUser { implicit request =>
    Ok("")
  }}


  def addRecommendation(
    siteId: Long, itemId: Long
  ) = isAuthenticated { implicit login => forSuperUser { implicit request =>
println("siteId = " + siteId + ", itemId = " + itemId)
    Ok("")
  }}
}
