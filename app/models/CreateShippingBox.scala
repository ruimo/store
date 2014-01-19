package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class CreateShippingBox(
  siteId: Long,
  itemClass: Long,
  boxSize: Int,
  boxName: String
) {
  def save(implicit conn: Connection) {
    DB.withConnection { implicit conn =>
      ShippingBox.createNew(siteId, itemClass, boxSize, boxName)
    }
  }
}
