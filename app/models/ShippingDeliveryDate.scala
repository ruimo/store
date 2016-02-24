package models

import org.joda.time.DateTime
import java.sql.Connection

case class ShippingDeliveryDate(
  shippingDate: DateTime, deliveryDate: DateTime
) {
  def save(
    siteUser: Option[SiteUser], transactionSiteId: Long
  )(
    implicit conn: Connection
  ) {
    TransactionShipStatus.updateShippingDeliveryDate(siteUser, transactionSiteId, shippingDate.getMillis, deliveryDate.getMillis)
    TransactionShipStatus.update(siteUser, transactionSiteId, TransactionStatus.CONFIRMED)
  }
}

