package models

import play.api.db.DB
import play.api.Play.current

case class ChangeItemMetadataTable(
  itemMetadatas: Seq[ChangeItemMetadata]
) {
  def update(itemId: Long) {
    itemMetadatas.foreach {
      _.update(itemId)
    }
  }
}

case class ChangeItemMetadata(
  metadataType: Int, metadata: Long
) {
  def update(itemId: Long) {
    DB.withTransaction { implicit conn =>
      ItemNumericMetadata.update(itemId, ItemNumericMetadataType.byIndex(metadataType), metadata)
    }
  }

  def add(itemId: Long) {
    ExceptionMapper.mapException {
      DB.withTransaction { implicit conn =>
        ItemNumericMetadata.add(itemId, ItemNumericMetadataType.byIndex(metadataType), metadata)
      }
    }
  }
}
