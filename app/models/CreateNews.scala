package models

import play.api.db.DB
import play.api.Play.current
import org.joda.time.DateTime
import java.sql.Connection

case class CreateNews(
  title: String,
  contents: String,
  releaseTime: DateTime,
  siteId: Option[Long]
) {
  def save()(implicit conn: Connection): News = News.createNew(
    siteId = siteId,
    title = title,
    contents = contents,
    releaseTime = releaseTime.getMillis
  )

  def update(id: Long)(implicit conn: Connection): Int = News.update(
    id = NewsId(id),
    siteId = siteId,
    title = title,
    contents = contents,
    releaseTime = releaseTime.getMillis
  )
}

