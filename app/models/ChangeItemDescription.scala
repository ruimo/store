package models

import play.api.db.DB
import play.api.Play.current

case class ChangeItemDescriptionTable(
  itemDescriptions: Seq[ChangeItemDescription]
) {
  def update(itemId: Long) {
    itemDescriptions.foreach {
      _.update(itemId)
    }
  }
}

case class ChangeItemDescription(
  siteId: Long, localeId: Long, itemDescription: String
) {
  def update(itemId: Long) {
    DB.withTransaction { implicit conn =>
      ItemDescription.update(siteId, itemId, localeId, itemDescription)
    }
  }

  def add(itemId: Long) {
    ExceptionMapper.mapException {
      DB.withTransaction { implicit conn =>
        ItemDescription.add(siteId, itemId, localeId, itemDescription)
      }
    }
  }
}
