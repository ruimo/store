package models

import anorm._
import anorm.{NotAssigned, Pk}
import anorm.SqlParser
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import play.api.i18n.Lang
import java.sql.Connection

case class Transporter(id: Pk[Long] = NotAssigned) extends NotNull

case class TransporterName(
  id: Pk[Long] = NotAssigned,
  localeId: Long,
  transporterId: Long,
  transporterName: String
) extends NotNull

object Transporter {
  val simple = {
    SqlParser.get[Pk[Long]]("transporter.transporter_id") map {
      case id => Transporter(id)
    }
  }

  val withName = Transporter.simple ~ TransporterName.simple map {
    case t~name => (t, name)
  }

  val withNameOpt = Transporter.simple ~ (TransporterName.simple ?) map {
    case trans~name => (trans, name)
  }

  def list(implicit conn: Connection): Seq[Transporter] =
    SQL(
      """
      select * from transporter order by transporter_id
      """
    ).as(
      simple *
    )

  def listWithName(implicit conn: Connection, lang: Lang): Seq[(Transporter, Option[TransporterName])] =
    SQL(
      """
      select *
      from transporter
      left outer join transporter_name on transporter_name.transporter_id = transporter.transporter_id
      and transporter_name.locale_id = {localeId}
      order by transporter.transporter_id
      """
    ).on(
      'localeId -> LocaleInfo.byLang(lang).id
    ).as(
      withNameOpt *
    )

  def tableForDropDown(implicit lang: Lang, conn: Connection): Seq[(String, String)] = 
    tableForDropDown(LocaleInfo.byLang(lang))

  def tableForDropDown(locale: LocaleInfo)(implicit conn: Connection): Seq[(String, String)] = {
    SQL(
      """
      select * from transporter
      inner join transporter_name on transporter.transporter_id = transporter_name.transporter_id
      where locale_id = {localeId}
      order by transporter_name.transporter_name
      """
    ).on(
      'localeId -> locale.id
    ).as(
      withName *
    ).map {
      e => e._1.id.get.toString -> e._2.transporterName
    }
  }

  def createNew(implicit conn: Connection): Transporter = {
    SQL(
      """
      insert into transporter values (
        (select nextval('transporter_seq'))
      )
      """
    ).executeUpdate()

    val id = SQL("select currval('transporter_seq')").as(SqlParser.scalar[Long].single)
    Transporter(Id(id))
  }
}

object TransporterName {
  val simple = {
    SqlParser.get[Pk[Long]]("transporter_name.transporter_name_id") ~
    SqlParser.get[Long]("transporter_name.locale_id") ~
    SqlParser.get[Long]("transporter_name.transporter_id") ~
    SqlParser.get[String]("transporter_name.transporter_name") map {
      case id~localeId~transporterId~transporterName => TransporterName(
        id, localeId, transporterId, transporterName
      )
    }
  }

  def createNew(
    transporterId: Long, locale: LocaleInfo, name: String
  )(
    implicit conn: Connection
  ): TransporterName = {
    SQL(
      """
      insert into transporter_name (
        transporter_name_id,
        locale_id,
        transporter_id,
        transporter_name
      ) values (
        (select nextval('transporter_name_seq')),
        {localeId},
        {transporterId},
        {transporterName}
      )
      """
    ).on(
      'localeId -> locale.id,
      'transporterId -> transporterId,
      'transporterName -> name
    ).executeUpdate()

    val id = SQL("select currval('transporter_name_seq')").as(SqlParser.scalar[Long].single)
    TransporterName(Id(id), locale.id, transporterId, name)
  }

  def list(transporterId: Long)(implicit conn: Connection): Map[LocaleInfo, TransporterName] =
    SQL(
      """
      select * from transporter_name where transporter_id = {id}
      """
    ).on(
      'id -> transporterId
    ).as(simple * ).map { e =>
      LocaleInfo(e.localeId) -> e
    }.toMap

  def update(transporterId: Long, localeId: Long, transporterName: String)(implicit conn: Connection) {
    SQL(
      """
      update transporter_name set transporter_name = {transporterName}
      where transporter_id = {transporterId} and locale_id = {localeId}
      """
    ).on(
      'transporterName -> transporterName,
      'transporterId -> transporterId,
      'localeId -> localeId
    ).executeUpdate()
  }

  def add(transporterId: Long, localeId: Long, transporterName: String)(implicit conn: Connection) {
    SQL(
      """
      insert into transporter_name (transporter_name_id, transporter_id, locale_id, transporter_name)
      values (
        (select nextval('transporter_name_seq')),
        {transporterId}, {localeId}, {transporterName}
      )
      """
    ).on(
      'transporterId -> transporterId,
      'localeId -> localeId,
      'transporterName -> transporterName
    ).executeUpdate()
  }

  def remove(id: Long, localeId: Long)(implicit conn: Connection) {
    SQL(
      """
      delete from transporter_name
      where transporter_id = {id}
      and locale_id = {localeId}
      """
    ).on(
      'id -> id,
      'localeId -> localeId
    ).executeUpdate()
  }
}
