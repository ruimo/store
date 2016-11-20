package models

import org.joda.time.DateTime
import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class CreateSiteItemMetadata(
  siteId: Long, metadataType: Int, metadata: Long, validUntil: DateTime
) {
  def add(itemId: Long)(implicit conn: Connection) {
    ExceptionMapper.mapException {
      SiteItemNumericMetadata.createNew(
        siteId, ItemId(itemId), SiteItemNumericMetadataType.byIndex(metadataType), metadata, validUntil.getMillis
      )
    }
  }
}
