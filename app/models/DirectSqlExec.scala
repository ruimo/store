package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class DirectSqlExec(
  sql: String
) {
}
