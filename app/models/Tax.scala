package models

import anorm._
import anorm.{NotAssigned, Pk}
import anorm.SqlParser
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import play.api.i18n.{Messages, Lang}

case class Tax(id: Pk[Long] = NotAssigned) extends NotNull

case class TaxHistory(id: Pk[Long] = NotAssigned, taxId: Long, taxType: TaxType, rate: BigDecimal, validUntil: Long) extends NotNull

case class TaxName(taxId: Long, locale: LocaleInfo, taxName: String) extends NotNull

object Tax {
  val simple = {
    SqlParser.get[Pk[Long]]("tax.tax_id") map {
      case id => Tax(id)
    }
  }

  def createNew(): Tax = DB.withConnection { implicit conn => {
    SQL("insert into tax (tax_id) values ((select nextval('tax_seq')))").executeUpdate()

    val taxId = SQL("select currval('tax_seq')").as(SqlParser.scalar[Long].single)

    Tax(Id(taxId))
  }}

  def list: Seq[Tax] = DB.withConnection { implicit conn => {
    SQL("select * from tax order by tax_id").as(Tax.simple *)
  }}

  val allTaxType = classOf[TaxType].getEnumConstants().toList

  def taxTypeTable(implicit lang: Lang): Seq[(String, String)] = {
    val locale = LocaleInfo.byLang(lang)

    allTaxType.map {
      e => e.ordinal.toString -> Messages("tax." + e)
    }
  }
}

object TaxName {
  val simple = {
    SqlParser.get[Long]("tax_name.tax_id") ~
    SqlParser.get[Long]("tax_name.locale_id") ~
    SqlParser.get[String]("tax_name.tax_name") map {
      case taxId~localeId~taxName =>
        TaxName(taxId, LocaleInfo(localeId), taxName)
    }
  }

  def createNew(tax: Tax, locale:LocaleInfo, name: String): TaxName = DB.withConnection { implicit conn =>
    SQL(
      """
      insert into tax_name (tax_id, locale_id, tax_name)
      values ({taxId}, {localeId}, {taxName})
      """
    ).on(
      'taxId -> tax.id.get,
      'localeId -> locale.id,
      'taxName -> name
    ).executeUpdate()

    TaxName(tax.id.get, locale, name)
  }
}

object TaxHistory {
  val simple = {
    SqlParser.get[Pk[Long]]("tax_history.tax_history_id") ~
    SqlParser.get[Long]("tax_history_id.tax_id") ~
    SqlParser.get[Int]("tax_history.tax_type") ~
    SqlParser.get[java.math.BigDecimal]("tax_history.rate") ~
    SqlParser.get[java.util.Date]("tax_history.valid_until") map {
      case id~taxId~taxType~rate~validUntil =>
        TaxHistory(id, taxId, TaxType.byIndex(taxType), rate, validUntil.getTime)
    }
  }

  def createNew(tax: Tax, taxType: TaxType, rate: BigDecimal, validUntil: Long)
    : TaxHistory = DB.withConnection { implicit conn => 
  {
    SQL(
      """
      insert into tax_history (tax_history_id, tax_id, tax_type, rate, valid_until)
      values ((select nextval('tax_history_seq')), {taxId}, {taxType}, {rate}, {validUntil})
      """
    ).on(
      'taxId -> tax.id.get,
      'taxType -> taxType.ordinal,
      'rate -> rate.bigDecimal,
      'validUntil -> new java.sql.Timestamp(validUntil)
    ).executeUpdate()

    val id = SQL("select currval('tax_history_seq')").as(SqlParser.scalar[Long].single)

    TaxHistory(Id(id), tax.id.get, taxType, rate, validUntil)
  }}
}
