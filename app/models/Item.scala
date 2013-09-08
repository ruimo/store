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

case class ItemNumericMetadata(
  id: Pk[Long] = NotAssigned, itemId: Long, metadataType: Int, metadata: Long
) extends NotNull

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

  def list(site: Site, locale: LocaleInfo, queryString: String, page: Int = 0, pageSize: Int = 10):
    Seq[(Item, ItemName, ItemDescription, ItemPrice, ItemPriceHistory, IntMap[ItemNumericMetadata])] =
    listBySiteId(site.id.get, locale, queryString, page, pageSize)

  def listBySiteId(siteId: Long, locale: LocaleInfo, queryString: String, page: Int = 0, pageSize: Int = 10):
    Seq[(Item, ItemName, ItemDescription, ItemPrice, ItemPriceHistory, IntMap[ItemNumericMetadata])] =
  DB.withConnection { implicit conn => {
    val itemTable = SQL(
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
      'localeId -> siteId,
      'siteId -> siteId
    ).as(listParser *)

    itemTable.map {e => {
      val itemId = e._1.id.get
      val itemPriceId = e._4.id.get

      val priceHistory = ItemPriceHistory.at(itemPriceId)
    }}


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

  def createNew(site: Site, item: Item): ItemPrice = DB.withConnection { implicit conn => {
    SQL(
      """
      insert into item_price (item_price_id, site_id, item_id)
      values ((select nextval('item_price_seq')), {siteId}, {itemId})
      """
    ).on(
      'siteId -> site.id.get,
      'itemId -> item.id.get
    ).executeUpdate()

    val itemPriceId = SQL("select currval('item_price_seq')").as(SqlParser.scalar[Long].single)

    ItemPrice(Id(itemPriceId), site.id.get, item.id.get)
  }}

  def get(site: Site, item: Item): Option[ItemPrice] = DB.withConnection { implicit conn => {
    SQL(
      "select * from item_price where item_id = {itemId} and site_id = {siteId}"
    ).on(
      'itemId -> item.id.get,
      'siteId -> site.id.get
    ).as(
      ItemPrice.simple.singleOpt
    )
  }}
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

  def createNew(itemPrice: ItemPrice, tax: Tax, currency: CurrencyInfo, unitPrice: BigDecimal, validUntil: Long)
    : ItemPriceHistory = DB.withConnection { implicit conn =>
  {
    SQL(
      """
      insert into item_price_history(
        item_price_history_id, item_price_id, tax_id, currency_id, unit_price, valid_until
      ) values (
        (select nextval('item_price_history_seq')),
        {itemPriceId}, {taxId}, {currencyId}, {unitPrice}, {validUntil}
      )
      """
    ).on(
      'itemPriceId -> itemPrice.id.get,
      'taxId -> tax.id.get,
      'currencyId -> currency.id,
      'unitPrice -> unitPrice.bigDecimal,
      'validUntil -> new java.sql.Timestamp(validUntil)
    ).executeUpdate()

    val id = SQL("select currval('item_price_history_seq')").as(SqlParser.scalar[Long].single)

    ItemPriceHistory(Id(id), itemPrice.id.get, tax.id.get, currency, unitPrice, validUntil)
  }}

  def list(itemPrice: ItemPrice): Seq[ItemPriceHistory] = DB.withConnection { implicit conn =>
    SQL(
      "select * from item_price_history where item_price_id = {itemPriceId}"
    ).on(
      'itemPriceId -> itemPrice.id.get
    ).as(
      ItemPriceHistory.simple *
    )
  }

  def at(itemPriceId: Long, now: Long = System.currentTimeMillis):
    ItemPriceHistory = DB.withConnection { implicit conn => 
  {
    SQL(
      """
      select * from item_price_history
      where item_price_id = {itemPriceId}
      and {now} < valid_until
      order by valid_until
      limit 1
      """
    ).on(
      'itemPriceId -> itemPriceId,
      'now -> new java.sql.Timestamp(now)
    ).as(
      ItemPriceHistory.simple.single
    )
  }}
}

object ItemNumericMetadata {
  val simple = {
    SqlParser.get[Pk[Long]]("item_numeric_metadata.item_numeric_metadata_id") ~
    SqlParser.get[Long]("item_numeric_metadata.item_id") ~
    SqlParser.get[Int]("item_numeric_metadata.metadata_type") ~
    SqlParser.get[Long]("item_numeric_metadata.metadata") map {
      case id~itemId~metadata_type~metadata => ItemNumericMetadata(id, itemId, metadata_type, metadata)
    }
  }

  def createNew(item: Item, metadataType: MetadataType, metadata: Long):
    ItemNumericMetadata = DB.withConnection { implicit conn =>
  {
    SQL(
      """
      insert into item_numeric_metadata(item_numeric_metadata_id, item_id, metadata_type, metadata)
      values (
        (select nextval('item_numeric_metadata_seq')),
        {itemId}, {metadataType}, {metadata}
     )
      """
    ).on(
      'itemId -> item.id.get,
      'metadataType -> metadataType.ordinal,
      'metadata -> metadata
    ).executeUpdate()

    val id = SQL("select currval('item_numeric_metadata_seq')").as(SqlParser.scalar[Long].single)

    ItemNumericMetadata(Id(id), item.id.get, metadataType.ordinal, metadata)
  }}

  def apply(item: Item, metadataType: MetadataType):
    ItemNumericMetadata = DB.withConnection { implicit conn =>
  {
    SQL(
      """
      select * from item_numeric_metadata
      where item_id = {itemId}
      and metadata_type = {metadataType}
      """
    ).on(
      'itemId -> item.id.get,
      'metadataType -> metadataType.ordinal
    ).as(
      ItemNumericMetadata.simple.single
    )
  }}
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
