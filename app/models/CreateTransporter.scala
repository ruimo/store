package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class CreateTransporter(localeId: Long, transporterName: String) {
  def save(implicit conn: Connection) {
    ExceptionMapper.mapException {
      val trans = Transporter.createNew
      TransporterName.createNew(trans.id.get, LocaleInfo(localeId), transporterName)
    }
  }
}

