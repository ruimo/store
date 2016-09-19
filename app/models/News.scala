package models

import anorm._
import play.api.Play.current
import play.api.db._

import collection.immutable
import java.sql.{Timestamp, Connection}

case class NewsId(id: Long) extends AnyVal

case class News(
  id: Option[NewsId] = None,
  siteId: Option[Long],
  title: String,
  contents: String,
  releaseTime: Long,
  updatedTime: Long
)

object News {
  val simple = {
    SqlParser.get[Option[Long]]("news_id") ~
    SqlParser.get[Option[Long]]("site_id") ~
    SqlParser.get[String]("title") ~
    SqlParser.get[String]("contents") ~
    SqlParser.get[java.time.Instant]("release_time") ~
    SqlParser.get[java.time.Instant]("updated_time") map {
      case id~siteId~title~contents~releaseTime~updatedTime =>
        News(id.map(NewsId.apply), siteId, title, contents, releaseTime.toEpochMilli, updatedTime.toEpochMilli)
    }
  }

  def apply(id: NewsId)(implicit conn: Connection): News = SQL(
    """
    select * from news where news_id = {id}
    """
  ).on(
    'id -> id.id
  ).as(simple.single)

  val withSite = simple ~ (Site.simple ?) map {
    case news~site => (news, site)
  }

  def list(page: Int = 0, pageSize: Int = 10, orderBy: OrderBy = OrderBy("news.release_time desc"))(
    implicit conn: Connection
  ): PagedRecords[(News, Option[Site])] = {
    val records: Seq[(News, Option[Site])] = SQL(
      """
      select * from news
      left join site s on s.site_id = news.site_id
      order by """ + orderBy + """
      limit {pageSize} offset {offset}
      """
    ).on(
      'pageSize -> pageSize,
      'offset -> page * pageSize
    ).as(
      withSite *
    )

    val count = SQL(
      "select count(*) from news"
    ).as(SqlParser.scalar[Long].single)

    PagedRecords(page, pageSize, (count + pageSize - 1) / pageSize, orderBy, records)
  }

  def createNew(
    siteId: Option[Long], title: String, contents: String, releaseTime: Long, updatedTime: Long = System.currentTimeMillis
  )(implicit conn: Connection): News = {
    SQL(
      """
      insert into news (news_id, site_id, title, contents, release_time, updated_time) values (
        (select nextval('news_seq')), {siteId}, {title}, {contents}, {releaseTime}, {updatedTime}
      )
      """
    ).on(
      'siteId -> siteId,
      'title -> title,
      'contents -> contents,
      'releaseTime -> java.time.Instant.ofEpochMilli(releaseTime),
      'updatedTime -> java.time.Instant.ofEpochMilli(updatedTime)
    ).executeUpdate()

    val newsId = SQL("select currval('news_seq')").as(SqlParser.scalar[Long].single)

    News(Some(NewsId(newsId)), siteId, title, contents, releaseTime, updatedTime)
  }

  def update(
    id: NewsId, siteId: Option[Long], title: String, contents: String, releaseTime: Long, updatedTime: Long = System.currentTimeMillis
  )(implicit conn: Connection): Int =
    SQL(
      """
      update news set
        site_id = {siteId},
        title = {title},
        contents = {contents},
        release_time = {releaseTime},
        updated_time = {updatedTime}
      where news_id = {newsId}
      """
    ).on(
      'newsId -> id.id,
      'siteId -> siteId,
      'title -> title,
      'contents -> contents,
      'releaseTime -> java.time.Instant.ofEpochMilli(releaseTime),
      'updatedTime -> java.time.Instant.ofEpochMilli(updatedTime)
    ).executeUpdate()

  def delete(newsId: NewsId)(implicit conn: Connection): Int =
    SQL(
      """
      delete from news where news_id = {newsId}
      """
    ).on(
      'newsId -> newsId.id
    ).executeUpdate()
}

