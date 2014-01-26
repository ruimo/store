package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class CreateShippingFee(feeId: Long, countryCode: Int, locationCodeTable: List[Int]) {
  def update(boxId: Long)(implicit conn: Connection) {
    locationCodeTable.map { loc =>
      try {
        ExceptionMapper.mapException {
          ShippingFee.createNew(boxId, CountryCode.byIndex(countryCode), loc)
        }
      }
      catch {
        case e: UniqueConstraintException =>
      }
    }
  }
}

