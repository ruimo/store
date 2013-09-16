package models

import play.api.db.DB
import play.api.Play.current

case class ChangeItemPriceTable(
  itemPrices: Seq[ChangeItemPrice]
) {
  def update(itemId: Long) {
    itemPrices.foreach {
      _.update(itemId)
    }
  }
}

case class ChangeItemPrice(
  siteId: Long, localeId: Long, itemPrice: BigDecimal
) {
  def update(itemId: Long) {
    DB.withTransaction { implicit conn =>
//      ItemPrice.update(siteId, itemId, localeId, itemPrice)
    }
  }

  def add(itemId: Long) {
    ExceptionMapper.mapException {
      DB.withTransaction { implicit conn =>
//        ItemPrice.add(siteId, itemId, localeId, itemPrice)
      }
    }
  }
}
