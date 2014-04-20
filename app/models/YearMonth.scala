package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class YearMonth(year: Int, month: Int) {
  def next = if (month < 12) YearMonth(year, month + 1) else YearMonth(year + 1, 1)
}

object YearMonth {
  val MinYear = 1900
  val MaxYear = 9999
}
