package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class ChangeItemMetadataTable(
  itemMetadatas: Seq[ChangeItemMetadata]
) {
  def update(itemId: Long)(implicit conn: Connection) {
    itemMetadatas.foreach {
      _.update(itemId)
    }
  }
}

case class ChangeItemMetadata(
  metadataType: Int, metadata: Long
) {
  def update(itemId: Long)(implicit conn: Connection) {
    ItemNumericMetadata.update(ItemId(itemId), ItemNumericMetadataType.byIndex(metadataType), metadata)
  }

  def add(itemId: Long) {
    ExceptionMapper.mapException {
      DB.withTransaction { implicit conn =>
        ItemNumericMetadata.add(ItemId(itemId), ItemNumericMetadataType.byIndex(metadataType), metadata)
      }
    }
  }
}
