package models

import play.api.db.DB
import play.api.Play.current
import org.joda.time.DateTime
import java.sql.Connection

case class ChangeFeeHistoryTable(
  histories: Seq[ChangeFeeHistory]
) {
  def update(feeId: Long) {
    DB.withTransaction { implicit conn =>
      histories.foreach {
        _.update(feeId)
      }
    }
  }
}

case class ChangeFeeHistory(
  taxId: Long, fee: BigDecimal, validUntil: DateTime
) {
  def update(feeId: Long)(implicit conn: Connection) {
  }
}
