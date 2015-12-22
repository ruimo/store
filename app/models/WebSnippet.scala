package models

import java.time.Instant
import helpers.Cache
import anorm._
import anorm.SqlParser
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import java.sql.Connection

case class WebSnippetId(id: Long) extends AnyVal

case class WebSnippet(
  id: Option[WebSnippetId] = None,
  siteId: Long,
  contentCode: String,
  content: String,
  updatedTime: Long
)


class MaxWebSnippetCountException(val siteId: Long) extends Exception

object WebSnippet {
  def MaxSnippetCountPerSite = Cache.Conf.getInt("maxSnippetCountPerSite").getOrElse(10)

  val simple = {
    SqlParser.get[Option[Long]]("web_snippet.web_snippet_id") ~
    SqlParser.get[Long]("web_snippet.site_id") ~
    SqlParser.get[String]("web_snippet.content_code") ~
    SqlParser.get[String]("web_snippet.content") ~
    SqlParser.get[java.util.Date]("web_snippet.updated_time") map {
      case id~siteId~contentCode~content~updatedTime =>
        WebSnippet(id.map(WebSnippetId.apply), siteId, contentCode, content, updatedTime.getTime)
    }
  }

  def createNew(
    siteId: Long, contentCode: String, content: String, created: Long = System.currentTimeMillis, maxCount: Int = MaxSnippetCountPerSite
  )(implicit conn: Connection): WebSnippetId = {
    val updateCount = SQL(
      """
      insert into web_snippet (
        web_snippet_id, site_id, content_code, content, updated_time
      ) 
      select
        nextval('web_snippet_seq'), {siteId}, {contentCode}, {content}, {created}
      where (select count(*) from web_snippet where site_id = {siteId}) < {maxCount}
      """
    ).on(
      'siteId -> siteId,
      'contentCode -> contentCode,
      'content -> content,
      'maxCount -> maxCount,
      'created -> Instant.ofEpochMilli(created)
    ).executeUpdate()

    if (updateCount == 0)
      throw new MaxWebSnippetCountException(siteId)

    WebSnippetId(SQL("select currval('web_snippet_seq')").as(SqlParser.scalar[Long].single))
  }

  def listNewerBySite(
    contentCode: Option[String] = None, maxCountBySite: Int = 1
  )(implicit conn: Connection): Seq[WebSnippet] = SQL(
    """
    select * from web_snippet w1
    where updated_time >= (
      select updated_time
      from web_snippet w2
      where w1.site_id = w2.site_id
      """ + (contentCode.map(cc => "and w2.content_code = {contentCode} ").getOrElse("")) + """
      order by w2.updated_time desc
      limit 1 offset {maxCount}
    )
    """ + (contentCode.map(cc => "and content_code = {contentCode} ").getOrElse(""))
  ).on(
    (
      NamedParameter("maxCount", maxCountBySite - 1) ::
        contentCode.map { cc => List(NamedParameter("contentCode", cc)) }.getOrElse(Nil)
    ):_*
  ).as(
    simple *
  )

  val withSite = simple ~ Site.simple map {
    case webSnippet~site => (webSnippet, site)
  }

  def list(
    page: Int = 0, pageSize: Int = 50, orderBy: OrderBy = OrderBy("web_snippet.site_id")
  )(
    implicit conn: Connection
  ): PagedRecords[(WebSnippet, Site)] = {
    val records: Seq[(WebSnippet, Site)] = SQL(
      """
      select * from web_snippet w
      inner join site s on s.site_id = w.site_id
      oreder by """ + orderBy + """
      limit {pageSize} offset {offset}
      """
    ).on(
      'pageSize -> pageSize,
      'offset -> page * pageSize
    ).as(
      withSite *
    )

    val count = SQL(
      "select count(*) from web_snippet"
    ).as(SqlParser.scalar[Long].single)

    PagedRecords(page, pageSize, (count + pageSize - 1) / pageSize, orderBy, records)
  }
}
