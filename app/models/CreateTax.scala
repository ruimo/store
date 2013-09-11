package models

import java.sql.Date.{valueOf => date}

case class CreateTax(taxType: Int, localeId: Long, name: String, rate: BigDecimal) {
  def save() {
    val tax = Tax.createNew()
    TaxName.createNew(tax, LocaleInfo(localeId), name)
    TaxHistory.createNew(tax, TaxType.byIndex(taxType), rate, date("9999-12-31").getTime)
  }
}

