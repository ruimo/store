package models

import anorm._
import anorm.{NotAssigned, Pk}
import anorm.SqlParser
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import collection.immutable.IntMap

case class Item(id: Pk[Long] = NotAssigned, categoryId: Long) extends NotNull

case class ItemName(localeId: Long, itemId: Long, name: String) extends NotNull

case class ItemDescription(localeId: Long, itemId: Long, siteId: Long, description: String) extends NotNull

case class ItemPrice(id: Pk[Long] = NotAssigned, siteId: Long, itemId: Long) extends NotNull

case class ItemPriceHistory(
  id: Pk[Long] = NotAssigned,
  itemPriceId: Long, 
  taxId: Long, 
  currency: CurrencyInfo,
  unitPrice: BigDecimal,
  validUntil: Long
) extends NotNull

case class ItemNumericMetadata(id: Pk[Long] = NotAssigned, metadataType: Int, metadata: Long) extends NotNull

case class SiteItem(itemId: Long, siteId: Long) extends NotNull

object Item {
  val simple = {
    SqlParser.get[Pk[Long]]("item.item_id") ~
    SqlParser.get[Long]("item.category_id") map {
      case id~categoryId => Item(id, categoryId)
    }
  }

  def createNew(category: Category): Item = DB.withConnection { implicit conn => {
    SQL(
      """
      insert into item values (
        (select nextval('item_seq')), {categoryId}
      )
      """
    ).on(
      'categoryId -> category.id
    ).executeUpdate()

    val itemId = SQL("select currval('item_seq')").as(SqlParser.scalar[Long].single)

    Item(Id(itemId), category.id.get)
  }}

  val listParser = Item.simple~ItemName.simple~ItemDescription.simple~ItemPrice.simple~SiteItem.simple map {
    case item~itemName~itemDescription~itemPrice~siteItem => (
      item, itemName, itemDescription, itemPrice, siteItem
    )
  }

  def list(siteId: Long, locale: LocaleInfo, queryString: String, page: Int = 0, pageSize: Int = 10):
    Seq[(Item, ItemName, ItemDescription, ItemPrice, ItemPriceHistory, IntMap[ItemNumericMetadata])] =
  DB.withConnection { implicit conn => {
    SQL(
      """
      select * from item
      inner join item_name on item.item_id = item_name.item_id
      inner join item_description on item.item_id = item_description.item_id
      inner join item_price on item.item_id = item_price.item_id
      inner join site_item on item.item_id = site_item.item_id
      where item_name.locale_id = {localeId}
      and item_description.locale_id = {localeId}
      and item_description.site_id = {siteId}
      and item_price.site_id = {siteId}
      and site_item.site_id = {siteId}
      """
    ).on(
      'localeId -> locale.id,
      'siteId -> siteId
    ).as(listParser *)

    List()
  }}
}

object ItemName {
  val simple = {
    SqlParser.get[Long]("item_name.locale_id") ~
    SqlParser.get[Long]("item_name.item_id") ~
    SqlParser.get[String]("item_name.item_name") map {
      case localeId~itemId~name => ItemName(localeId, itemId, name)
    }
  }

  def createNew(item: Item, names: Map[LocaleInfo, String]): Map[LocaleInfo, ItemName] = DB.withConnection {
    implicit conn => {
      names.transform { (k, v) => {
        SQL(
          """
          insert into item_name (locale_id, item_id, item_name)
          values ({localeId}, {itemId}, {itemName})
          """
        ).on(
          'localeId -> k.id,
          'itemId -> item.id.get,
          'itemName -> v
        ).executeUpdate()

        ItemName(k.id, item.id.get, v)
      }}
    }
  }

  def list(item: Item): Map[LocaleInfo, ItemName] = DB.withConnection { implicit conn => {
    SQL(
      "select * from item_name where item_id = {itemId}"
    ).on(
      'itemId -> item.id.get
    ).as(simple *).map { e =>
      LocaleInfo(e.localeId) -> e
    }.toMap
  }}
}

object ItemDescription {
  val simple = {
    SqlParser.get[Long]("item_description.locale_id") ~
    SqlParser.get[Long]("item_description.item_id") ~
    SqlParser.get[Long]("item_description.site_id") ~
    SqlParser.get[String]("item_description.description") map {
      case localeId~itemId~siteId~description => ItemDescription(localeId, itemId, siteId, description)
    }
  }

  def createNew(item: Item, site: Site, description: String): ItemDescription = DB.withConnection {
    implicit conn => {
      SQL(
        """
        insert into item_description (locale_id, item_id, site_id, description)
        values ({localeId}, {itemId}, {siteId}, {description})
        """
      ).on(
        'localeId -> site.localeId,
        'itemId -> item.id.get,
        'siteId -> site.id.get,
        'description -> description
      ).executeUpdate()

      ItemDescription(site.localeId, item.id.get, site.id.get, description)
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

object ItemNumericMetadata {
  val simple = {
    SqlParser.get[Pk[Long]]("item_numeric_metadata.item_id") ~
    SqlParser.get[Int]("item_numeric_metadata.metadata_type") ~
    SqlParser.get[Long]("item_numeric_metadata.metadata") map {
      case id~metadata_type~metadata => ItemNumericMetadata(id, metadata_type, metadata)
    }
  }
}

object SiteItem {
  val simple = {
    SqlParser.get[Long]("site_item.item_id") ~
    SqlParser.get[Long]("site_item.site_id") map {
      case itemId~siteId => SiteItem(itemId, siteId)
    }
  }

  def createNew(site: Site, item: Item): SiteItem = DB.withConnection { implicit conn => {
    SQL("insert into site_item (item_id, site_id) values ({itemId}, {siteId})").on(
      'itemId -> item.id.get,
      'siteId -> site.id.get
    ).executeUpdate()

    SiteItem(item.id.get, site.id.get)
  }}
}
