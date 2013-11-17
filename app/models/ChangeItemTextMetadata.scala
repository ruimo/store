package models

import play.api.db.DB
import play.api.Play.current

case class ChangeItemTextMetadataTable(
  itemTextMetadatas: Seq[ChangeItemTextMetadata]
) {
  def update(itemId: Long) {
    itemTextMetadatas.foreach {
      _.update(itemId)
    }
  }
}

case class ChangeItemTextMetadata(
  metadataType: Int, metadata: String
) {
  def update(itemId: Long) {
    DB.withTransaction { implicit conn =>
      ItemTextMetadata.update(itemId, ItemTextMetadataType.byIndex(metadataType), metadata)
    }
  }

  def add(itemId: Long) {
    ExceptionMapper.mapException {
      DB.withTransaction { implicit conn =>
        ItemTextMetadata.add(itemId, ItemTextMetadataType.byIndex(metadataType), metadata)
      }
    }
  }
}
