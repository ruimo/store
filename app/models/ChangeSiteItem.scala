package models

import play.api.db.DB
import play.api.Play.current

case class ChangeSiteItemTable(
  sites: Seq[ChangeSiteItem]
)

case class ChangeSiteItem(
  siteId: Long
) {
  def add(itemId: Long) {
    ExceptionMapper.mapException {
      DB.withTransaction { implicit conn =>
        SiteItem.add(itemId, siteId)
      }
    }
  }
}
