package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class ChangeItemCategory(
  categoryId: Long
) {
  def update(itemId: Long)(implicit conn: Connection) {
    Item.changeCategory(itemId, categoryId)
  }
}
