package models

import play.api.db.DB
import play.api.Play.current
import org.joda.time.DateTime

case class ChangeItemPriceTable(
  itemPrices: Seq[ChangeItemPrice]
) {
  def update() {
    itemPrices.foreach {
      _.update()
    }
  }
}

case class ChangeItemPrice(
  siteId: Long, itemPriceId: Long, itemPriceHistoryId: Long, taxId: Long,
  currencyId: Long, unitPrice: BigDecimal, validUntil: DateTime
) {
  def update() {
    DB.withTransaction { implicit conn =>
      ItemPriceHistory.update(itemPriceHistoryId, taxId, currencyId, unitPrice, validUntil)
    }
  }

  def add(itemId: Long) {
    ExceptionMapper.mapException {
      DB.withTransaction { implicit conn =>
        ItemPriceHistory.add(itemId, siteId, taxId, currencyId, unitPrice, validUntil)
      }
    }
  }
}
