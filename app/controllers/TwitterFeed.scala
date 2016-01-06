package controllers

import controllers.I18n.I18nAware
import helpers.Cache
import play.api.Play.current
import play.api.mvc.{Controller, RequestHeader}
import helpers.TwitterAdapter
import play.api.Play
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
    Ok(twitter().getLatestTweetEmbed(screenName)().getOrElse("NONE"))
  }
}
