package controllers

import controllers.I18n.I18nAware
import helpers.Cache
import play.api.Play.current
import play.api.mvc.{Controller, RequestHeader}
import helpers.TwitterAdapter
import play.api.Play
import play.api.libs.json.{JsObject, Json, JsString, JsNumber}
import play.api.libs.json.{JsObject, Json, JsString, JsNumber}

object TwitterFeed extends Controller with NeedLogin with HasLogger with I18nAware {
  val twitter: () => TwitterAdapter = Cache.cacheOnProd(
    () => TwitterAdapter(
      Play.maybeApplication.get.configuration.getString("twitter.consumerKey").get,
      Play.maybeApplication.get.configuration.getString("twitter.secretKey").get,
      Play.maybeApplication.get.configuration.getString("twitter.accessToken").get,
      Play.maybeApplication.get.configuration.getString("twitter.accessTokenSecret").get
    )
  )

  def latestTweet(screenName: String) = optIsAuthenticatedJson { implicit optLogin => implicit request =>
    Ok(twitter().getLatestTweetEmbed(screenName)().map(_._1)getOrElse("NONE"))
  }

  def latestTweetJson(
    screenName: String,
    omitScript: Boolean,
    maxWidth: Option[Int]
  ) = optIsAuthenticatedJson { implicit optLogin => implicit request =>
    twitter().getLatestTweetEmbed(
      screenName = screenName,
      omitScript = omitScript,
      maxWidth = maxWidth
    )() match {
      case None => NotFound("No tweet for '" + screenName + "' found.")
      case Some(t) => Ok(
        Json.toJson(
          JsObject(
            Seq(
              "html" -> JsString(t._1),
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
