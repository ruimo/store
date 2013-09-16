package models

import play.api.db.DB
import play.api.Play.current

case class ChangeItemNameTable(
  itemNames: Seq[ChangeItemName]
) {
  def update(itemId: Long) {
    itemNames.foreach {
      _.update(itemId)
    }
  }
}

case class ChangeItemName(
  localeId: Long, itemName: String
) {
  def update(itemId: Long) {
    DB.withTransaction { implicit conn =>
      ItemName.update(itemId, localeId, itemName)
    }
  }

  def add(itemId: Long) {
    ExceptionMapper.mapException {
      DB.withTransaction { implicit conn =>
        ItemName.add(itemId, localeId, itemName)
      }
    }
  }
}
