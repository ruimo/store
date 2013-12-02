package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class ChangeShippingInfo(transporterId: Long, slipCode: String) {
  def save(siteUser: Option[SiteUser], transactionSiteId: Long)(implicit conn: Connection) {
    TransactionShipStatus.updateShippingInfo(siteUser, transactionSiteId, transporterId, slipCode)
    TransactionShipStatus.update(siteUser, transactionSiteId, TransactionStatus.SHIPPED)
  }
}
