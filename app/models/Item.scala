package models

import anorm._
import anorm.{NotAssigned, Pk}
import anorm.SqlParser
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps

case class Item(id: Pk[Long] = NotAssigned, categoryId: Long) extends NotNull

case class ItemName(localeId: Long, itemId: Long, name: String) extends NotNull

case class ItemDescription(localeId: Long, itemId: Long, description: String) extends NotNull

case class ItemPrice(id: Pk[Long] = NotAssigned, siteId: Long, itemId: Long) extends NotNull

case class ItemPriceHistory(
  id: Pk[Long] = NotAssigned,
  itemPriceId: Long, 
  taxId: Long, 
  currency: CurrencyInfo,
  unitPrice: BigDecimal,
  validUntil: Long
) extends NotNull

object Item {
  val simple = {
    SqlParser.get[Pk[Long]]("item.item_id") ~
    SqlParser.get[Long]("item.category_id") map {
      case id~categoryId => Item(id, categoryId)
    }
  }
}

object ItemName {
  val simple = {
    SqlParser.get[Long]("item_name.locale_id") ~
    SqlParser.get[Long]("item_name.item_id") ~
    SqlParser.get[String]("item_name.name") map {
      case localeId~itemId~name => ItemName(localeId, itemId, name)
    }
  }
}

object ItemDescription {
  val simple = {
    SqlParser.get[Long]("item_description.locale_id") ~
    SqlParser.get[Long]("item_description.item_id") ~
    SqlParser.get[String]("item_description.description") map {
      case localeId~itemId~description => ItemDescription(localeId, itemId, description)
    }
  }
}

object ItemPrice {
  val simple = {
    SqlParser.get[Pk[Long]]("item_price.item_price_id") ~
    SqlParser.get[Long]("item_price.site_id") ~
    SqlParser.get[Long]("item_price.item_id") map {
      case id~siteId~itemId => ItemPrice(id, siteId, itemId)
    }
  }
}

object ItemPriceHistory {
  val simple = {
    SqlParser.get[Pk[Long]]("item_price_history.item_price_history_id") ~
    SqlParser.get[Long]("item_price_history.item_price_id") ~
    SqlParser.get[Long]("item_price_history.tax_id") ~
    SqlParser.get[Long]("item_price_history.currency_id") ~
    SqlParser.get[java.math.BigDecimal]("item_price_history.unit_price") ~
    SqlParser.get[java.util.Date]("item_price_history.valid_until") map {
      case id~itemPriceId~taxId~currencyId~unitPrice~validUntil
        => ItemPriceHistory(id, itemPriceId, taxId, CurrencyInfo(currencyId), unitPrice, validUntil.getTime)
    }
  }
}


