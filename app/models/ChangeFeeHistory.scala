package models

import play.api.db.DB
import play.api.Play.current
import org.joda.time.DateTime
import java.sql.Connection

case class ChangeFeeHistoryTable(
  histories: Seq[ChangeFeeHistory]
) {
  def update(feeId: Long)(implicit conn: Connection) {
    histories.foreach {
      _.update()
    }
  }
}

case class ChangeFeeHistory(
  historyId: Long, taxId: Long, fee: BigDecimal, costFee: Option[BigDecimal], validUntil: DateTime
) {
  def update()(implicit conn: Connection) {
    ShippingFeeHistory.update(historyId, taxId, fee, costFee, validUntil.getMillis)
  }

  def add(feeId: Long)(implicit conn: Connection) {
    ExceptionMapper.mapException {
      ShippingFeeHistory.createNew(
        feeId, taxId, fee, costFee, validUntil.getMillis
      )
    }
  }
}
