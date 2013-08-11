package models

import anorm._
import anorm.SqlParser._
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
  val simple = {
    get[Pk[Long]]("locale.locale_id") ~
    get[String]("locale.lang") ~
    get[Option[String]]("locale.country") map {
      case id~lang~country => LocaleInfo(id, lang, country)
    }
  }

  lazy val registry: Map[Long, Locale] = DB.withConnection { implicit conn =>
    SQL("select * from locale")
      .as(LocaleInfo.simple *)
      .map(r => r.id.get -> r.toLocale)
      .toMap
  }

  def apply(id: Long): Option[Locale] = registry.get(id)

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
