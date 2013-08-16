package models

import anorm._
import anorm.{NotAssigned, Pk}
import anorm.SqlParser
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps

case class Tax(id: Pk[Long] = NotAssigned)

case class TaxHistory(id: Pk[Long] = NotAssigned, taxId: Long, taxType: TaxType, rate: BigDecimal, validUntil: Long)

object Tax {
  val simple = {
    SqlParser.get[Pk[Long]]("tax.tax_id") map {
      case id => Tax(id)
    }
  }
}

object TaxHistory {
  val simple = {
    SqlParser.get[Pk[Long]]("tax_history.tax_history_id") ~
    SqlParser.get[Long]("tax_history_id.tax_id") ~
    SqlParser.get[Int]("tax_history.tax_type") ~
    SqlParser.get[java.math.BigDecimal]("tax_history.rate") ~
    SqlParser.get[java.util.Date]("tax_history.valid_until") map {
      case id~taxId~taxType~rate~validUntil =>
        TaxHistory(id, taxId, TaxType.byIndex(taxType), rate, validUntil.getTime)
    }
  }
}
