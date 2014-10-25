package models

import play.api.db.DB
import play.api.Play.current
import org.joda.time.DateTime
import java.sql.Connection

case class ChangeItemPriceTable(
  itemPrices: Seq[ChangeItemPrice]
) {
  def update()(implicit conn: Connection) {
    itemPrices.foreach {
      _.update()
    }
  }
}

case class ChangeItemPrice(
  siteId: Long, itemPriceId: Long, itemPriceHistoryId: Long, taxId: Long,
  currencyId: Long, unitPrice: BigDecimal, costPrice: BigDecimal, validUntil: DateTime
) {
  def update()(implicit conn: Connection) {
    ItemPriceHistory.update(itemPriceHistoryId, taxId, currencyId, unitPrice, costPrice, validUntil)
  }

  def add(itemId: Long)(implicit conn: Connection) {
    ExceptionMapper.mapException {
      ItemPriceHistory.add(ItemId(itemId), siteId, taxId, currencyId, unitPrice, costPrice, validUntil)
    }
  }
}
