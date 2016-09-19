package models

import play.api.db.DB
import play.api.Play.current
import org.joda.time.DateTime
import java.sql.Connection

case class CreateNews(
  title: String,
  contents: String,
  releaseTime: DateTime
) {
  def save()(implicit conn: Connection): News = News.createNew(
    siteId = None,
    title = title,
    contents = contents,
    releaseTime = releaseTime.getMillis
  )
}

