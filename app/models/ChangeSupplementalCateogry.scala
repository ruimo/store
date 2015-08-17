package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class ChangeSupplementalCategory(categoryId: Long) {
  def add(itemId: Long)(implicit conn: Connection) {
    ExceptionMapper.mapException {
      SupplementalCategory.createNew(ItemId(itemId), categoryId)
    }
  }
}
