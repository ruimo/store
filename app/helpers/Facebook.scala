package helpers

import scala.util.Try
import play.api.Logger
import play.api.libs.json._
import scala.concurrent.Await
import play.api.libs.ws.{WS, WSResponse}
import scala.concurrent.duration._
import play.api.mvc.Results
import play.api.Play.current
import scala.collection.immutable
import scala.collection.mutable
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.util.regex.Pattern

case class FacebookPostV25(
  messageBody: Option[String],
  story: Option[String],
  createdTime: Instant,
  pageId: Long,
  postId: Long
)

object FacebookPostV25 {
  val PostTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")
  val PostIdSeparator = Pattern.compile("_")

  def apply(messageBody: Option[String], story: Option[String], createdTime: String, id: String): FacebookPostV25 = {
    val ids: Array[Long] = try {
      PostIdSeparator.split(id).ensuring(_.length == 2, "Invalid post id format.").map(_.toLong)
    }
    catch {
      case t: Throwable => throw new RuntimeException("Invalid facebook post id '" + id + "'", t)
    }

    FacebookPostV25(
      messageBody,
      story,
      Instant.from(PostTimeFormat.parse(createdTime)),
      ids(0), ids(1)
    )
  }

  def apply(json: JsValue): FacebookPostV25 = {
    FacebookPostV25(
      (json \ "message").asOpt[String],
      (json \ "story").asOpt[String],
      (json \ "created_time").as[String],
      (json \ "id").as[String]
    )
  }
}

class Facebook(
  appId: String, appSecret: String, 
  tokenCacheDurationInMilli: Long = 60 * 60 * 1000, cacheDurationInMilli: Long = 60 * 1000
) {
  import Facebook._
  private val feedCache = new mutable.LinkedHashMap[String, () => immutable.Seq[FacebookPostV25]]()

  def feedsV25(pageId: String): immutable.Seq[FacebookPostV25] = {
    feedCache.synchronized {
      feedCache.get(pageId).getOrElse {
        while (feedCache.size > FeedCacheSize) {
          val (key, value) = feedCache.head
          feedCache.remove(key)
        }
        val f: () => immutable.Seq[FacebookPostV25] = Cache.mayBeCached(
          gen = () => retrieveFeedsV25(pageId),
          expirationInMillis = Some(FeedCachePeriod)
        )
        feedCache.put(pageId, f)
        f
      }
    }()
  }

  val accessToken: () => String = Cache.mayBeCached[String](
    gen = () => acuireAccessToken,
    expirationInMillis = Some(tokenCacheDurationInMilli)
  )

  def retrieveFeedsV25(pageId: String): immutable.Seq[FacebookPostV25] = {
    logger.info("Obtaining facebook posts. appId: '" + appId + "'")
    val resp: WSResponse = Await.result(
      WS.url(graphFeedUrl(pageId))
        .withQueryString(
          "access_token" -> accessToken()
        )
        .get(), Duration(30, SECONDS)
    )

    assert(
      resp.status == Results.Ok.header.status,
      "Status invalid (=" + resp.status + ") appId: '" + appId + "'. body: " + resp.body
    )

    try {
      parsePostsV25(Json.parse(resp.body))
    }
    catch {
      case t: Throwable =>
        logger.error("Cannot parse facebook response '" + resp.body + "'.")
        throw t
    }
  }

  def acuireAccessToken: String = {
    logger.info("Acquiring access token. appId: '" + appId + "'")
    val resp: WSResponse = Await.result(
      WS.url(AccessTokenUrl)
        .withQueryString(
          "client_id" -> appId,
          "client_secret" -> appSecret,
          "grant_type" -> "client_credentials"
        )
        .get(), Duration(30, SECONDS)
    )

    assert(
      resp.status == Results.Ok.header.status,
      "Status invalid (=" + resp.status + ") appId: '" + appId + "'"
    )

    AccessTokenExtractor(resp.body) match {
      case Left(e) => throw new RuntimeException(
        "Acquiring access token response from Facebook for '" + appId + "' is invalid '" + e + "'."
      )
      case Right(s) => s
    }
  }
}

object Facebook {
  val logger = Logger(getClass)
  val FeedCacheSize = 100
  val FeedCachePeriod = 3 * 60 * 1000
  val AccessTokenUrl = "https://graph.facebook.com/oauth/access_token"
  def graphFeedUrl(pageId: String) = "https://graph.facebook.com/v2.5/" + pageId + "/feed"

  def apply(appId: String, appSecret: String): Facebook = new Facebook(appId, appSecret)
  def stripPrefix(prefix: String)(s: String): Either[String, String] =
    if (s.startsWith(prefix)) Right(s.substring(prefix.length)) else Left(s)

  val AccessTokenExtractor: String => Either[String, String] = stripPrefix("access_token=") _

  def parsePostsV25(json: JsValue): immutable.Seq[FacebookPostV25] =
    (json \ "data").as[JsArray].value.map(FacebookPostV25.apply).to[immutable.Seq]
}
