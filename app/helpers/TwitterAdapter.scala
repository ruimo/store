package helpers

import play.api.Logger
import twitter4j.{Twitter, TwitterFactory, Status, OEmbedRequest}
import twitter4j.conf.ConfigurationBuilder

class TwitterAdapter(
  consumerKey: String, secretKey: String,
  accessToken: String, accessTokenSecret: String,
  cacheDurationInMilli: Long = 5 * 60 * 1000
) {
  private val twitter = new TwitterFactory(
    new ConfigurationBuilder()
      .setOAuthConsumerKey(consumerKey)
      .setOAuthConsumerSecret(secretKey)
      .setOAuthAccessToken(accessToken)
      .setOAuthAccessTokenSecret(accessTokenSecret)
      .build()
  ).getInstance()

  def getLatestTweet(screenName: String): () => Option[Status] = Cache.mayBeCached[Option[Status]](
    gen = () => {
      val z: java.util.Iterator[Status] = twitter.getUserTimeline(screenName).iterator
      if (z.hasNext) Some(z.next) else None
    },
    expirationInMillis = Some(cacheDurationInMilli)
  )

  def getLatestTweetEmbed(
    screenName: String, omitScript: Boolean = true
  ): () => Option[(String, java.time.Instant)] = Cache.mayBeCached[Option[(String, java.time.Instant)]](
    gen = () => getLatestTweet(screenName)().map { st =>
      val tweetId = st.getId
      val req = new OEmbedRequest(tweetId, "https://twitter.com/" + screenName + "/status/" + tweetId)
      req.setOmitScript(omitScript)
      twitter.getOEmbed(req).getHtml -> java.time.Instant.ofEpochMilli(st.getCreatedAt.getTime)
    },
    expirationInMillis = Some(cacheDurationInMilli)
  )
}

object TwitterAdapter {
  val logger = Logger(getClass)
  def apply(
    consumerKey: String, secretKey: String,
    accessToken: String, accessTokenSecret: String,
    cacheDurationInMilli: Long = 5 * 60 * 1000
  ) = new TwitterAdapter(
    consumerKey, secretKey, accessToken, accessTokenSecret, cacheDurationInMilli
  )
}
