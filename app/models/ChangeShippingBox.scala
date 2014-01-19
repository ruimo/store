package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class ChangeShippingBox(
  id: Long,
  siteId: Long,
  itemClass: Long,
  boxSize: Int,
  boxName: String
) {
  def save(implicit conn: Connection) {
    ExceptionMapper.mapException {
      DB.withConnection { implicit conn =>
        ShippingBox.update(id, siteId, itemClass, boxSize, boxName)
      }    
    }
  }
}
