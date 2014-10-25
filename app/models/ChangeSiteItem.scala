package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class ChangeSiteItemTable(
  sites: Seq[ChangeSiteItem]
)

case class ChangeSiteItem(
  siteId: Long
) {
  def add(itemId: Long)(implicit conn: Connection) {
    ExceptionMapper.mapException {
      SiteItem.add(ItemId(itemId), siteId)
      ItemPrice.add(ItemId(itemId), siteId)
    }
  }
}
