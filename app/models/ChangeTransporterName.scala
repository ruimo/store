package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class ChangeTransporterNameTable(
  transporterNames: Seq[ChangeTransporterName]
) {
  def update(id: Long)(implicit conn: Connection) {
    transporterNames.foreach {
      _.update(id)
    }
  }
}

case class ChangeTransporterName(
  localeId: Long, transporterName: String
) {
  def update(id: Long)(implicit conn: Connection) {
    TransporterName.update(id, localeId, transporterName)
  }

  def add(id: Long)(implicit conn: Connection) {
    ExceptionMapper.mapException {
      TransporterName.add(id, localeId, transporterName)
    }
  }
}
