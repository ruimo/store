package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class ChangeTransactionStatus(transactionSiteId: Long, status: Int) {
  def save()(implicit conn: Connection) {
    TransactionShipStatus.update(transactionSiteId, TransactionStatus.byIndex(status))
  }
}
