package helpers

import org.specs2.mutable._
import com.ruimo.scoins.Scoping._
import play.api.libs.json._
import java.time.Instant

class FacebookSpec extends Specification {
  "Facebook" should {
    "can parse json." in {
      val json: JsValue = Json.parse(
        """
{
  "message": "Body message",
  "created_time": "2015-12-09T13:30:00+0000",
  "id": "20531316728_10154243083616729"
}
        """
      )

      doWith(FacebookPostV25(json)) { post =>
        post.messageBody === Some("Body message")
        post.createdTime === Instant.parse("2015-12-09T13:30:00Z")
        post.pageId === 20531316728L
        post.postId === 10154243083616729L
      }
    }

    "Can parse posts" in {
      val json: JsValue = Json.parse(
"""
{
   "data": [
      {
         "message": "Message 01",
         "created_time": "2015-12-09T19:00:49+0000",
         "id": "20531316728_10154244454516729"
      },
      {
         "message": "Message 02",
         "created_time": "2015-12-09T13:30:00+0000",
         "id": "20531316728_10154243083616729"
      }
   ]
}
"""
      )

      doWith(Facebook.parsePostsV25(json)) { posts =>
        posts.size === 2
        posts(0) === FacebookPostV25(
          Some("Message 01"), None, Instant.parse("2015-12-09T19:00:49Z"), 20531316728L, 10154244454516729L
        )
        posts(1) === FacebookPostV25(
          Some("Message 02"), None, Instant.parse("2015-12-09T13:30:00Z"), 20531316728L, 10154243083616729L
        )
      }
    }
  }
}
