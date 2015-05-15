package models

import anorm._
import anorm.SqlParser
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import collection.immutable.IntMap
import java.sql.Connection
import play.api.i18n.Lang

case class Site(
  id: Option[Long] = None, localeId: Long, name: String
) extends Ordered[Site] {
  def compare(that: Site) = {
    this.name.compare(that.name)
  }
}

object Site {
  val simple = {
    SqlParser.get[Option[Long]]("site.site_id") ~
    SqlParser.get[Long]("site.locale_id") ~
    SqlParser.get[String]("site.site_name") map {
      case id~localeId~siteName => Site(id, localeId, siteName)
    }
  }

  def apply(siteId: Long)(implicit conn: Connection): Site =
    SQL(
      "select * from site where site_id = {id} and site.deleted = FALSE"
    ).on(
      'id -> siteId
    ).as(simple.single)

  def get(siteId: Long)(implicit conn: Connection): Option[Site] =
    SQL(
      "select * from site where site_id = {id} and site.deleted = FALSE"
    ).on(
      'id -> siteId
    ).as(simple.singleOpt)

  def createNew(locale: LocaleInfo, name: String)(implicit conn: Connection): Site = {
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

    Site(Some(siteId), locale.id, name)
  }

  def listByName(page: Int = 0, pageSize: Int = 99)(implicit conn: Connection): Seq[Site] = SQL(
    """
    select * from site where site.deleted = FALSE order by site_name
    limit {pageSize} offset {offset}
    """
  ).on(
    'pageSize -> pageSize,
    'offset -> page * pageSize
  ).as(simple *)

  def tableForDropDown(implicit login: LoginSession, conn: Connection): Seq[(String, String)] =
    SQL(
      """
      select * from site
      where site.deleted = FALSE
      """ +
      login.siteUser.map("and site_id = " + _.siteId).getOrElse("") +
      """
      order by site_name
      """
    ).as(simple *).map {
      e => e.id.get.toString -> e.name
    }

  def tableForDropDown(itemId: Long)(implicit login: LoginSession,  conn: Connection): Seq[(String, String)] =
    SQL(
      """
      select * from site
      inner join site_item on site.site_id = site_item.site_id
      where site_item.item_id = {itemId} and site.deleted = FALSE
      """ +
      login.siteUser.map("and site.site_id = " + _.siteId).getOrElse("") +
      """
      order by site_name
      """
    ).on(
      'itemId -> itemId
    ).as(simple *).map {
      e => e.id.get.toString -> e.name
    }

  def listAsMap(implicit conn: Connection): Map[Long, Site] =
    SQL(
      "select * from site where site.deleted = FALSE order by site_name"
    ).as(simple *).map {
      e => e.id.get -> e
    }.toMap

  def delete(siteId: Long)(implicit conn: Connection) {
    SQL(
      """
      update site set deleted = TRUE where site_id = {id}
      """
    ).on(
        'id -> siteId
    ).executeUpdate()
  }

  def update(id: Long, locale: LocaleInfo, name: String)(implicit conn: Connection): Long = SQL(
    """
    update site set
    locale_id = {localeId},
    site_name = {siteName}
    where site_id = {id}
    """
  ).on(
    'localeId -> locale.id,
    'siteName -> name,
    'id -> id
  ).executeUpdate()
}
