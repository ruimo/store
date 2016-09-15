package models

import anorm._
import play.api.Play.current
import play.api.db._

import collection.immutable
import java.sql.{Timestamp, Connection}

case class NewsId(id: Long) extends AnyVal

case class News(id: Option[NewsId] = None, siteId: Option[Long], contents: String, releaseTime: Long, updatedTime: Long)

object News {
  val simple = {
    SqlParser.get[Option[Long]]("news_id") ~
    SqlParser.get[Option[Long]]("site_id") ~
    SqlParser.get[String]("contents") ~
    SqlParser.get[java.time.Instant]("release_time") ~
    SqlParser.get[java.time.Instant]("updated_time") map {
      case id~siteId~contents~releaseTime~updatedTime =>
        News(id.map(NewsId.apply), siteId, contents, releaseTime.toEpochMilli, updatedTime.toEpochMilli)
    }
  }

  def createNew(
    siteId: Option[Long], contents: String, releaseTime: Long, updatedTime: Long
  )(implicit conn: Connection) {
    SQL(
      """
      insert into news (news_id, site_id, contents, release_time, updated_time) values (
        (select nextval('news_seq')), {siteId}, {contents}, {releaseTime}, {updatedTime}
      )
      """
    ).on(
      'siteId -> siteId,
      'contents -> contents,
      'releaseTime -> java.time.Instant.ofEpochMilli(releaseTime),
      'updatedTime -> java.time.Instant.ofEpochMilli(updatedTime)
    ).executeUpdate()

    val newsId = SQL("select currval('news_seq')").as(SqlParser.scalar[Long].single)

    News(Some(NewsId(newsId)), siteId, contents, releaseTime, updatedTime)
  }

  def delete(newsId: NewsId)(implicit conn: Connection): Int =
    SQL(
      """
      delete from news where news_id = {newsId}
      """
    ).on(
      'newsId -> newsId.id
    ).executeUpdate()
}

