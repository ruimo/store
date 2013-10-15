package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class ChangeTransactionStatus(transactionSiteId: Long, status: Int) {
  def save(siteUser: Option[SiteUser])(implicit conn: Connection) {
    TransactionShipStatus.update(siteUser, transactionSiteId, TransactionStatus.byIndex(status))
  }
}
