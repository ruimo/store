package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class ChangeSiteItemMetadataTable(
  siteItemMetadatas: Seq[ChangeSiteItemMetadata]
) {
  def update(itemId: Long)(implicit conn: Connection) {
    siteItemMetadatas.foreach {
      _.update(itemId)
    }
  }
}

case class ChangeSiteItemMetadata(
  siteId: Long, metadataType: Int, metadata: Long
) {
  def update(itemId: Long)(implicit conn: Connection) {
    SiteItemNumericMetadata.update(ItemId(itemId), siteId, SiteItemNumericMetadataType.byIndex(metadataType), metadata)
  }

  def add(itemId: Long)(implicit conn: Connection) {
    ExceptionMapper.mapException {
      SiteItemNumericMetadata.add(ItemId(itemId), siteId, SiteItemNumericMetadataType.byIndex(metadataType), metadata)
    }
  }
}
