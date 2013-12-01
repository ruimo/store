package models

import play.api.db.DB
import play.api.Play.current

case class ChangeTransporterNameTable(
  transporterNames: Seq[ChangeTransporterName]
) {
  def update(id: Long) {
    transporterNames.foreach {
      _.update(id)
    }
  }
}

case class ChangeTransporterName(
  localeId: Long, transporterName: String
) {
  def update(id: Long) {
    DB.withTransaction { implicit conn =>
      TransporterName.update(id, localeId, transporterName)
    }
  }

  def add(id: Long) {
    ExceptionMapper.mapException {
      DB.withTransaction { implicit conn =>
        TransporterName.add(id, localeId, transporterName)
      }
    }
  }
}
