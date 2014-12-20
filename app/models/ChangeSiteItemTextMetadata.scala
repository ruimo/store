package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class ChangeSiteItemTextMetadataTable(
  siteItemTextMetadata: Seq[ChangeSiteItemTextMetadata]
) {
  def update(itemId: Long)(implicit conn: Connection) {
    siteItemTextMetadata.foreach {
      _.update(itemId)
    }
  }
}

case class ChangeSiteItemTextMetadata(
  siteId: Long, metadataType: Int, metadata: String
) {
  def update(itemId: Long)(implicit conn: Connection) {
    SiteItemTextMetadata.update(ItemId(itemId), siteId, SiteItemTextMetadataType.byIndex(metadataType), metadata)
  }

  def add(itemId: Long)(implicit conn: Connection) {
    ExceptionMapper.mapException {
      SiteItemTextMetadata.add(ItemId(itemId), siteId, SiteItemTextMetadataType.byIndex(metadataType), metadata)
    }
  }
}
