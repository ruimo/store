package models

import anorm._
import anorm.{NotAssigned, Pk}
import anorm.SqlParser
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import collection.immutable.IntMap

case class Site(id: Pk[Long] = NotAssigned, localeId: Long, name: String) extends NotNull

object Site {
  val simple = {
    SqlParser.get[Pk[Long]]("site.site_id") ~
    SqlParser.get[Long]("site.locale_id") ~
    SqlParser.get[String]("site.site_name") map {
      case id~localeId~siteName => Site(id, localeId, siteName)
    }
  }

  def createNew(locale: LocaleInfo, name: String): Site = DB.withConnection { implicit conn => {
    SQL(
      """
      insert into site (site_id, locale_id, site_name)
      values ((select nextval('site_seq')), {localeId}, {siteName})
      """
    ).on(
      'localeId -> locale.id,
      'siteName -> name
    ).executeUpdate()

    val siteId = SQL("select currval('site_seq')").as(SqlParser.scalar[Long].single)

    Site(Id(siteId), locale.id, name)
  }}

  def listByName(page: Int = 0, pageSize: Int = 20): Seq[Site] = DB.withConnection { implicit conn => {
    SQL(
      """
      select * from site order by site_name
      limit {pageSize} offset {offset}
      """
    ).on(
      'pageSize -> pageSize,
      'offset -> page * pageSize
    ).as(simple *)
  }}
}
