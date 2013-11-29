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

  def list(implicit conn: Connection): Seq[Transporter] =
    SQL(
      """
      select * from transporter order by transporter_id
      """
    ).as(
      simple *
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
}
