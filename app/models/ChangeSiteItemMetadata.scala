package models

import play.api.db.DB
import play.api.Play.current

case class ChangeSiteItemMetadataTable(
  siteItemMetadatas: Seq[ChangeSiteItemMetadata]
) {
  def update(itemId: Long) {
    siteItemMetadatas.foreach {
      _.update(itemId)
    }
  }
}

case class ChangeSiteItemMetadata(
  siteId: Long, metadataType: Int, metadata: Long
) {
  def update(itemId: Long) {
    DB.withTransaction { implicit conn =>
      SiteItemNumericMetadata.update(itemId, siteId, SiteItemNumericMetadataType.byIndex(metadataType), metadata)
    }
  }

  def add(itemId: Long) {
    ExceptionMapper.mapException {
      DB.withTransaction { implicit conn =>
        SiteItemNumericMetadata.add(itemId, siteId, SiteItemNumericMetadataType.byIndex(metadataType), metadata)
      }
    }
  }
}
