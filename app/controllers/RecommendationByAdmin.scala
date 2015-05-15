package controllers

import play.api.data.format.Formats._
import play.api.data.Form
import play.api.data.Forms
import play.api.data.Forms._
import play.api.i18n.Messages
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
  val changeRecordForm = Form(
    mapping(
      "id" -> longNumber,
      "score" -> longNumber(min = 0),
      "enabled" -> boolean
    )(ChangeRecommendationByAdmin.apply)(ChangeRecommendationByAdmin.unapply)
  )

  def startEditRecommendByAdmin = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      Ok(views.html.admin.recommendationByAdminMenu())
    }
  }

  def selectItem(
    qs: List[String], pgStart: Int, pgSize: Int, orderBySpec: String
  ) = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      val queryStr = if (qs.size == 1) QueryString(qs.head) else QueryString(qs.filter {! _.isEmpty})
      DB.withConnection { implicit conn =>
        val list = Item.listForMaintenance(
          siteUser = None, locale = LocaleInfo.getDefault, queryString = queryStr, page = pgStart,
          pageSize = pgSize, orderBy = OrderBy(orderBySpec)
        )

        Ok(views.html.admin.selectItemForRecommendByAdmin(queryStr, list))
      }
    }
  }

  def startUpdate(
    page: Int, pageSize: Int, orderBySpec: String
  ) = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      DB.withConnection { implicit conn =>
        val records = RecommendByAdmin.listByScore(
          true, LocaleInfo.getDefault, page, pageSize
        ).map { t =>
          (t._1, t._2, t._3, changeRecordForm.fill(ChangeRecommendationByAdmin(t._1.id.get, t._1.score, t._1.enabled)))
        }
        Ok(views.html.admin.editRecommendationByAdmin(records))
      }
    }
  }

  def addRecommendation(
    siteId: Long, itemId: Long
  ) = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      DB.withConnection { implicit conn =>
        try {
          RecommendByAdmin.createNew(siteId, itemId)
          Redirect(
            routes.RecommendationByAdmin.selectItem(List())
          ).flashing(
            "message" -> Messages("itemIsCreated")
          )
        }
        catch {
          case e: UniqueConstraintException =>
            Redirect(
              routes.RecommendationByAdmin.selectItem(List())
            ).flashing(
              "errorMessage" -> Messages("unique.constraint.violation")
            )
        }
      }
    }
  }

  def removeRecommendation(id: Long) = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      DB.withConnection { implicit conn =>
        RecommendByAdmin.remove(id)
        Redirect(
          routes.RecommendationByAdmin.startUpdate()
        ).flashing(
          "message" -> Messages("recommendationRemoved")
        )
      }
    }
  }

  def changeRecommendation(
    page: Int, pageSize: Int, orderBySpec: String
  ) = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      changeRecordForm.bindFromRequest.fold(
        formWithErrors => {
          DB.withConnection { implicit conn =>
            val records = RecommendByAdmin.listByScore(
              true, LocaleInfo.getDefault, page, pageSize
            ).map { t =>
              if (t._1.id.get == formWithErrors("id").value.get.toLong) {
                (t._1, t._2, t._3, formWithErrors)
              }
              else {
                (t._1, t._2, t._3,
                 changeRecordForm.fill(ChangeRecommendationByAdmin(t._1.id.get, t._1.score, t._1.enabled)))
              }
            }
            BadRequest(views.html.admin.editRecommendationByAdmin(records))
          }
        },
        newRecommendation => {
          DB.withConnection { implicit conn =>
            RecommendByAdmin.updateScoreAndEnabled(
              newRecommendation.id,
              newRecommendation.score,
              newRecommendation.enabled
            )
            Redirect(
              routes.RecommendationByAdmin.startUpdate(page, pageSize)
            ).flashing(
              "message" -> Messages("recommendationUpdated")
            )
          }
        }
      )
    }
  }
}
