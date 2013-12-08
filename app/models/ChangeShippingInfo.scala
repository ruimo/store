package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class ChangeShippingInfo(transporterId: Long, slipCode: String) {
  def save(
    siteUser: Option[SiteUser], transactionSiteId: Long
  )(
    sendMail: => Unit
  )(
    implicit conn: Connection
  ) {
    TransactionShipStatus.updateShippingInfo(siteUser, transactionSiteId, transporterId, slipCode)
    TransactionShipStatus.update(siteUser, transactionSiteId, TransactionStatus.SHIPPED)
    val status = TransactionShipStatus.byTransactionSiteId(transactionSiteId)
    if (! status.mailSent) {
      TransactionShipStatus.mailSent(transactionSiteId)
      sendMail
    }
  }
}
