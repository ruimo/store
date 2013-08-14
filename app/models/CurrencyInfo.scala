package models

import anorm._
import anorm.SqlParser._
import java.util.{Currency, Locale}
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps

case class CurrencyInfo(id: Long, currencyCode: String) extends NotNull {
  def toCurrency: Currency = Currency.getInstance(currencyCode)
}

object CurrencyInfo {
  lazy val Jpy = apply(1L)
  lazy val Usd = apply(2L)

  val simple = {
    SqlParser.get[Long]("currency.currency_id") ~
    SqlParser.get[String]("currency.currency_code") map {
      case id~code => CurrencyInfo(id, code)
    }
  }

  lazy val registry: Map[Long, CurrencyInfo] = DB.withConnection { implicit conn =>
    SQL("select * from currency")
      .as(CurrencyInfo.simple *).map(r => r.id -> r).toMap
  }

  def apply(id: Long): CurrencyInfo = get(id).get

  def get(id: Long): Option[CurrencyInfo] = registry.get(id)
}
