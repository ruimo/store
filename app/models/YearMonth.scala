package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class YearMonth(year: Int, month: Int)

object YearMonth {
  val MinYear = 1900
  val MaxYear = 9999
}
