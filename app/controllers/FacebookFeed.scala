package controllers

import controllers.I18n.I18nAware
import helpers.Cache
import play.api.Play.current
import play.api.mvc.{Controller, RequestHeader}
import helpers.Facebook
import play.api.Play
import play.api.libs.json.{JsObject, Json, JsString, JsNumber}

object FacebookFeed extends Controller with NeedLogin with HasLogger with I18nAware {
  val facebook: () => Facebook = Cache.cacheOnProd(
    () => Facebook(
      Play.maybeApplication.get.configuration.getString("facebook.appId").get,
      Play.maybeApplication.get.configuration.getString("facebook.appSecret").get
    )
  )

  def latestPostId(pageId: String) = optIsAuthenticatedJson { implicit optLogin => implicit request =>
    facebook().feedsV25(pageId).headOption.map {
      feed => (feed.postId.toString, feed.createdTime)
    } match {
      case None => NotFound("No feed for page '" + pageId + "' found.")
      case Some(t) => Ok(
        Json.toJson(
          JsObject(
            Seq(
              "postId" -> JsString(t._1),
              // 64bit double float has 52 bit width fraction.
              // 2^52 / 1000 / 60 / 60 / 24 / 365 = 142808.207362.
              // For about 142,808 years since epoch, there should be
              // no error to conver long to double float.
              "lastUpdate" -> JsNumber(t._2.getEpochSecond)
            )
          )
        )
      )
    }
  }
}
