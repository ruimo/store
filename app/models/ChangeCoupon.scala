package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class ChangeCoupon(isCoupon: Boolean) {
  def update(itemId: ItemId)(implicit conn: Connection) {
    Coupon.update(itemId, isCoupon)
  }
}
