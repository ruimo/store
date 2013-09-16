package models

import play.api.db.DB
import play.api.Play.current

case class ChangeItemCategory(
  categoryId: Long
) {
  def update(itemId: Long) {
    DB.withTransaction { implicit conn =>
      Item.changeCategory(itemId, categoryId)
    }    
  }
}
