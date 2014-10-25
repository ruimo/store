package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class ChangeItemTextMetadataTable(
  itemTextMetadatas: Seq[ChangeItemTextMetadata]
) {
  def update(itemId: Long)(implicit conn: Connection) {
    itemTextMetadatas.foreach {
      _.update(itemId)
    }
  }
}

case class ChangeItemTextMetadata(
  metadataType: Int, metadata: String
) {
  def update(itemId: Long)(implicit conn: Connection) {
    ItemTextMetadata.update(ItemId(itemId), ItemTextMetadataType.byIndex(metadataType), metadata)
  }

  def add(itemId: Long) {
    ExceptionMapper.mapException {
      DB.withTransaction { implicit conn =>
        ItemTextMetadata.add(ItemId(itemId), ItemTextMetadataType.byIndex(metadataType), metadata)
      }
    }
  }
}
