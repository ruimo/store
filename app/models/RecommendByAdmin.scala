package models

import anorm._
import anorm.SqlParser
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import java.sql.Connection

case class RecommendByAdmin(
  id: Option[Long] = None,
  siteId: Long,
  itemId: Long,
  score: Double,
  enabled: Boolean
)

object RecommendByAdmin {
  val simple = {
    SqlParser.get[Option[Long]]("recommend_by_admin.recommend_by_admin_id") ~
    SqlParser.get[Long]("recommend_by_admin.site_id") ~
    SqlParser.get[Long]("recommend_by_admin.item_id") ~
    SqlParser.get[Double]("recommend_by_admin.score") ~
    SqlParser.get[Boolean]("recommend_by_admin.enabled") map {
      case id~siteId~itemId~score~enabled => RecommendByAdmin(id, siteId, itemId, score, enabled)
    }
  }

  def apply(id: Long)(implicit conn: Connection): RecommendByAdmin =
    SQL(
      "select * from recommend_by_admin where recommend_by_admin_id = {id}"
    ).on(
      'id -> id
    ).as(simple.single)

  def createNew(
    siteId: Long, itemId: Long, score: Double, enabled: Boolean
  )(
    implicit conn: Connection
  ): RecommendByAdmin = {
    SQL(
      """
      insert into recommend_by_admin (
        recommend_by_admin_id, site_id, item_id, score, enabled
      ) values (
        (select nextval('recommend_by_admin_seq')),
        {site_id}, {item_id}, {score}, {enabled}
      )
      """
    ).on(
      'site_id -> siteId,
      'item_id -> itemId,
      'score -> score,
      'enabled -> enabled
    ).executeUpdate()

    val id = SQL("select currval('recommend_by_admin_seq')").as(SqlParser.scalar[Long].single)
    RecommendByAdmin(Some(id), siteId, itemId, score, enabled)
  }

  def count(implicit conn: Connection): Long = SQL(
    "select count(*) from recommend_by_admin"
  ).as (
    SqlParser.scalar[Long].single
  )

  def listByScore(showDisabled: Boolean = true)(implicit conn: Connection): Seq[RecommendByAdmin] = SQL(
    "select * from recommend_by_admin " +
    (if (showDisabled) "" else "where enabled = true") +
    "  order by score desc"
  ).as(RecommendByAdmin.simple *)

  def remove(id: Long)(implicit conn: Connection) {
    SQL("delete from recommend_by_admin where recommend_by_admin_id = {id}").on('id -> id).executeUpdate()
  }

  def update(rec: RecommendByAdmin)(implicit conn: Connection) {
    SQL(
      """
      update recommend_by_admin set
      site_id = {siteId},
      item_id = {itemId},
      score = {score},
      enabled = {enabled}
      where recommend_by_admin_id = {id}
      """
    ).on(
      'siteId -> rec.siteId,
      'itemId -> rec.itemId,
      'score -> rec.score,
      'enabled -> rec.enabled,
      'id -> rec.id.get
    ).executeUpdate()
  }
}
