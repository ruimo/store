package models

import anorm._
import anorm.SqlParser
import java.util.Locale
import play.api.Play.current
import play.api.db._
import play.api.i18n.{Messages, Lang}
import scala.collection.mutable
import scala.collection.immutable
import scala.language.postfixOps
import java.sql.Connection


case class LocaleInfo(id: Long, lang: String, country: Option[String] = None) extends NotNull {
  def toLocale: Locale = country match {
    case None => new Locale(lang)
    case Some(c) => new Locale(lang, c)
  }

  def matchExactly(l: Lang): Boolean =
    matchLanguage(l) && (country match {
      case None => true
      case Some(c) => c == l.country
    })

  def matchLanguage(l: Lang): Boolean = lang == l.language
}

object LocaleInfo {
  lazy val Ja = apply(1L)
  lazy val En = apply(2L)

  val simple = {
    SqlParser.get[Long]("locale.locale_id") ~
    SqlParser.get[String]("locale.lang") ~
    SqlParser.get[Option[String]]("locale.country") map {
      case id~lang~country => LocaleInfo(id, lang, country)
    }
  }

  lazy val registry: immutable.SortedMap[Long, LocaleInfo] = DB.withConnection { implicit conn =>
    immutable.TreeMap(
      SQL("select * from locale")
        .as(LocaleInfo.simple *)
        .map(r => r.id -> r).toList
    )
  }

  lazy val byLang: Map[Lang, LocaleInfo] =
    registry.values.foldLeft(new mutable.HashMap[Lang, LocaleInfo]) {
      (map, e) => {map.put(new Lang(e.lang, e.country.getOrElse("")), e); map}
    }.toMap

  def localeTable(implicit lang: Lang): Seq[(String, String)] = registry.values.map {
    e => e.id.toString -> Messages("lang." + e.lang)
  }.toSeq

  def getDefault(implicit lang: Lang): LocaleInfo =
    byLang.get(lang).orElse {
      byLang.get(new Lang(lang.language))
    }.getOrElse {
      LocaleInfo.En
    }

  def apply(id: Long): LocaleInfo = get(id).get

  def get(id: Long): Option[LocaleInfo] = registry.get(id)

  def insert(locale: LocaleInfo)(implicit conn: Connection) = SQL(
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
