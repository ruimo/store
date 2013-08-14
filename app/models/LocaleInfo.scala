package models

import anorm._
import anorm.SqlParser
import java.util.Locale
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps

case class LocaleInfo(id: Pk[Long] = NotAssigned, lang: String, country: Option[String] = None) extends NotNull {
  def toLocale: Locale = country match {
    case None => new Locale(lang)
    case Some(c) => new Locale(lang, c)
  }
}

object LocaleInfo {
  lazy val ja = apply(1L)
  lazy val en = apply(2L)

  val simple = {
    SqlParser.get[Pk[Long]]("locale.locale_id") ~
    SqlParser.get[String]("locale.lang") ~
    SqlParser.get[Option[String]]("locale.country") map {
      case id~lang~country => LocaleInfo(id, lang, country)
    }
  }

  lazy val registry: Map[Long, LocaleInfo] = DB.withConnection { implicit conn =>
    SQL("select * from locale")
      .as(LocaleInfo.simple *)
      .map(r => r.id.get -> r)
      .toMap
  }

  def apply(id: Long): LocaleInfo = get(id).get

  def get(id: Long): Option[LocaleInfo] = registry.get(id)

  def insert(locale: LocaleInfo) = DB.withConnection { implicit conn =>
    SQL(
      """
      insert into locale values (
        {id}, {lang}, {country}
      )
      """
    ).on(
      'id -> locale.id,
      'lang -> locale.lang,
      'country -> locale.country
    ).executeUpdate()
  }
}
