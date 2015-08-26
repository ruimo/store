package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

trait HasYearMonth {
  val year: Int
  val month: Int
  def next: HasYearMonth
  val command: String
}

case class YearMonth(year: Int, month: Int, command: String) extends HasYearMonth {
  def next: YearMonth = if (month < 12) YearMonth(year, month + 1, command) else YearMonth(year + 1, 1, command)
}

case class YearMonthSite(year: Int, month: Int, siteId: Long, command: String) extends HasYearMonth {
  def next: YearMonthSite = 
    if (month < 12) YearMonthSite(year, month + 1, siteId, command) else YearMonthSite(year + 1, 1, siteId, command)
}

case class YearMonthUser(year: Int, month: Int, userId: Long, command: String) extends HasYearMonth {
  def next: YearMonthUser = 
    if (month < 12) YearMonthUser(year, month + 1, userId, command) else YearMonthUser(year + 1, 1, userId, command)
  lazy val userIdOpt: Option[Long] = if (userId == 0) None else Some(userId)
}

object YearMonth {
  val MinYear = 1900
  val MaxYear = 9999
}
