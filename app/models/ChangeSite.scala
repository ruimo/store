package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class ChangeSite(siteId: Long, localeId: Long, siteName: String) {
  def update()(implicit conn: Connection) {
    Site.update(siteId, LocaleInfo(localeId), siteName)
  }
}

object ChangeSite {
  def apply(site: Site): ChangeSite = ChangeSite(site.id.get, site.localeId, site.name)
}
