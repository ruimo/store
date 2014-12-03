package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class CreateItem(
  localeId: Long, siteId: Long, categoryId: Long, itemName: String, taxId: Long, 
  currencyId: Long, price: BigDecimal, listPrice: Option[BigDecimal], costPrice: BigDecimal, description: String,
  isCoupon: Boolean
) {
  def save()(implicit conn: Connection) {
    Item.createItem(this)
  }

  def site(implicit conn: Connection) = Site(siteId)

  def currency(implicit conn: Connection) = CurrencyInfo(currencyId)
}
