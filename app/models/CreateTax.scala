package models

import java.sql.Date.{valueOf => date}
import play.api.db.DB
import play.api.Play.current

case class CreateTax(taxType: Int, localeId: Long, name: String, rate: BigDecimal) {
  def save(): Unit = DB.withConnection { implicit conn => {
    val tax = Tax.createNew
    TaxName.createNew(tax, LocaleInfo(localeId), name)
    TaxHistory.createNew(tax, TaxType.byIndex(taxType), rate, date("9999-12-31").getTime)
  }}
}

