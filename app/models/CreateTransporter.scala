package models

import play.api.db.DB
import play.api.Play.current

case class CreateTransporter(localeId: Long, transporterName: String) {
  def save() {
    DB.withConnection { implicit conn =>
      val trans = Transporter.createNew
      TransporterName.createNew(trans.id.get, LocaleInfo(localeId), transporterName)
    }
  }
}

