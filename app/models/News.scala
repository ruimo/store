package models

import anorm._
import play.api.Play.current
import play.api.db._

import collection.immutable
import java.sql.{Timestamp, Connection}

case class NewsId(id: Long) extends AnyVal

case class News(id: Option[NewsId] = None, siteId: Option[Long], contents: String, updatedTime: Long)

object News {
  val simple = {
    SqlParser.get[Option[Long]]("news_id") ~
    SqlParser.get[Option[Long]]("site_id") ~
    SqlParser.get[String]("contents") ~
    SqlParser.get[java.util.Date]("updated_time") map {
      case id~siteId~contents~updatedTime =>
        News(id.map(NewsId.apply), siteId, contents, updatedTime.getTime)
    }
  }
      
}

