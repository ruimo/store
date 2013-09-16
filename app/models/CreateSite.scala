package models

import play.api.db.DB
import play.api.Play.current

case class CreateSite(localeId: Long, siteName: String) {
  def save() {
    DB.withConnection { implicit conn =>
      Site.createNew(LocaleInfo(localeId), siteName)
    }
  }
}

