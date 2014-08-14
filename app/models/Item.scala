package models

import anorm._
import anorm.SqlParser
import model.Until
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import collection.immutable.{HashMap, IntMap}
import java.sql.{Timestamp, Connection}
import play.api.data.Form
import org.joda.time.DateTime
import annotation.tailrec
import helpers.QueryString
import play.api.Play

case class Item(id: Option[Long] = None, categoryId: Long) extends NotNull

case class ItemName(localeId: Long, itemId: Long, name: String) extends NotNull

case class ItemDescription(localeId: Long, itemId: Long, siteId: Long, description: String) extends NotNull

case class ItemPrice(id: Option[Long] = None, siteId: Long, itemId: Long) extends NotNull

case class ItemPriceHistory(
  id: Option[Long] = None,
  itemPriceId: Long, 
  taxId: Long, 
  currency: CurrencyInfo,
  unitPrice: BigDecimal,
  costPrice: BigDecimal,
  validUntil: Long
) extends NotNull

case class ItemNumericMetadata(
  id: Option[Long] = None, itemId: Long, metadataType: ItemNumericMetadataType, metadata: Long
) extends NotNull

case class ItemTextMetadata(
  id: Option[Long] = None, itemId: Long, metadataType: ItemTextMetadataType, metadata: String
) extends NotNull

case class SiteItem(itemId: Long, siteId: Long) extends NotNull

case class SiteItemNumericMetadata(
  id: Option[Long] = None, itemId: Long, siteId: Long, metadataType: SiteItemNumericMetadataType, metadata: Long
) extends NotNull

object Item {
  val ItemListDefaultOrderBy = OrderBy("item_name.item_name", Asc)
  val ItemListQueryColumnsToAdd = Play.current.configuration.getString("item.list.query.columns.add").get

  val simple = {
    SqlParser.get[Option[Long]]("item.item_id") ~
    SqlParser.get[Long]("item.category_id") map {
      case id~categoryId => Item(id, categoryId)
    }
  }

  val itemParser = Item.simple~ItemName.simple~ItemDescription.simple~ItemPrice.simple~Site.simple map {
    case item~itemName~itemDescription~itemPrice~site => (
      item, itemName, itemDescription, itemPrice, site
    )
  }

  val itemListParser = Item.simple~ItemName.simple~ItemDescription.simple~ItemPriceHistory.simple~Site.simple map {
    case item~itemName~itemDescription~itemPrice~site => (
      item, itemName, itemDescription, itemPrice, site
    )
  }

  val itemListForMaintenanceParser = 
    Item.simple~(ItemName.simple ?)~(ItemDescription.simple ?)~(ItemPriceHistory.simple ?)~(Site.simple ?) map {
      case item~itemName~itemDescription~itemPrice~site => (
        item, itemName, itemDescription, itemPrice, site
      )
    }

  def apply(id: Long)(implicit conn: Connection): Item =
    SQL(
      "select * from item where item_id = {id}"
    ).on(
      'id -> id
    ).as(simple.single)

  def itemInfo(
    id: Long, locale: LocaleInfo,
    now: Long = System.currentTimeMillis
  )(
    implicit conn: Connection
  ): (Item, ItemName, ItemDescription, Site, ItemPriceHistory, Map[ItemNumericMetadataType, ItemNumericMetadata]) = {
    val item = SQL(
      """
      select * from item
      inner join item_name on item.item_id = item_name.item_id
      inner join item_description on item.item_id = item_description.item_id
      inner join item_price on item.item_id = item_price.item_id
      inner join site_item on item.item_id = site_item.item_id
      inner join site on site_item.site_id = site.site_id
      where item.item_id= {id}
      and item_name.locale_id = {localeId}
      and item_description.locale_id = {localeId}
      order by item_name.item_name
      """
    ).on(
      'id -> id,
      'localeId -> locale.id
    ).as(
      itemParser.single
    )

    val itemId = item._1.id.get
    val itemPriceId = item._4.id.get
    val priceHistory = ItemPriceHistory.at(itemPriceId, now)
    val metadata = ItemNumericMetadata.allById(itemId)

    (item._1, item._2, item._3, item._5, priceHistory, metadata)
  }

  def createNew(category: Category)(implicit conn: Connection): Item = createNew(category.id.get)

  def createNew(categoryId: Long)(implicit conn: Connection): Item = {
    SQL(
      """
      insert into item values (
        (select nextval('item_seq')), {categoryId}
      )
      """
    ).on(
      'categoryId -> categoryId
    ).executeUpdate()

    val itemId = SQL("select currval('item_seq')").as(SqlParser.scalar[Long].single)

    Item(Some(itemId), categoryId)
  }

  def listForMaintenance(
    siteUser: Option[SiteUser] = None, locale: LocaleInfo, queryString: QueryString,
    page: Int = 0, pageSize: Int = 10,
    now: Long = System.currentTimeMillis,
    orderBy: OrderBy = ItemListDefaultOrderBy
  )(
    implicit conn: Connection
  ): PagedRecords[(
    Item, Option[ItemName], Option[ItemDescription], Option[Site], Option[ItemPriceHistory],
    Map[ItemNumericMetadataType, ItemNumericMetadata],
    Option[Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]],
    Map[ItemTextMetadataType, ItemTextMetadata]
  )] = {
    val sqlBody = """
      left join item_name on item.item_id = item_name.item_id and item_name.locale_id = {localeId}
      left join item_description on item.item_id = item_description.item_id and item_description.locale_id = {localeId}
      left join item_price on item.item_id = item_price.item_id 
      left join item_price_history on item_price.item_price_id = item_price_history.item_price_id
      left join site_item on item.item_id = site_item.item_id and item_price.site_id = site_item.site_id
      left join site on site_item.site_id = site.site_id
      where 1 = 1
    """ + 
    siteUser.map { "  and site.site_id = " + _.siteId }.getOrElse("") +
    """
      and (
        (item_price_history.item_price_history_id is null)
        or item_price_history.item_price_history_id = (
          select iph.item_price_history_id
          from item_price_history iph
          where
            iph.item_price_id = item_price.item_price_id and
            iph.valid_until > {now}
            order by iph.valid_until
            limit 1
        )
      )
    """ +
    createQueryConditionSql(queryString)

    val columns = if (ItemListQueryColumnsToAdd.isEmpty) "*" else "*, " + ItemListQueryColumnsToAdd

    val sql = SQL(
      s"select $columns from item $sqlBody order by $orderBy limit {pageSize} offset {offset}"
    )

    val list = applyQueryString(queryString, sql)
      .on(
        'localeId -> locale.id,
        'pageSize -> pageSize,
        'offset -> page * pageSize,
        'now -> new Timestamp(now)
      ).as(
        itemListForMaintenanceParser *
      ).map {e => {
        val itemId = e._1.id.get
        val metadata = ItemNumericMetadata.allById(itemId)
        val textMetadata = ItemTextMetadata.allById(itemId)
        val siteMetadata = e._5.map {md => SiteItemNumericMetadata.all(md.id.get, itemId)}

        (e._1, e._2, e._3, e._5, e._4, metadata, siteMetadata, textMetadata)
      }}

    val countSql = SQL(
      "select count(*) from item" + sqlBody
    )

    val count = applyQueryString(queryString, countSql)
      .on(
        'localeId -> locale.id,
        'now -> new Timestamp(now)
      ).as(
        SqlParser.scalar[Long].single
      )

    PagedRecords(page, pageSize, (count + pageSize - 1) / pageSize, orderBy, list)
  }

  // Do not pass user input directly into orderBy argument. That will
  // be SQL injection vulnerability.
  def list(
    siteUser: Option[SiteUser] = None, locale: LocaleInfo, queryString: QueryString,
    page: Int = 0, pageSize: Int = 10,
    now: Long = System.currentTimeMillis,
    orderBy: OrderBy = ItemListDefaultOrderBy
  )(
    implicit conn: Connection
  ): PagedRecords[(
    Item, ItemName, ItemDescription, Site, ItemPriceHistory,
    Map[ItemNumericMetadataType, ItemNumericMetadata],
    Map[SiteItemNumericMetadataType, SiteItemNumericMetadata],
    Map[ItemTextMetadataType, ItemTextMetadata]
  )] = {
    val sqlBody = """
      inner join item_name on item.item_id = item_name.item_id
      inner join item_description on item.item_id = item_description.item_id
      inner join item_price on item.item_id = item_price.item_id 
      inner join item_price_history on item_price.item_price_id = item_price_history.item_price_id
      inner join site_item on item.item_id = site_item.item_id and item_price.site_id = site_item.site_id
      inner join site on site_item.site_id = site.site_id
      where item_name.locale_id = {localeId}
    """ + 
    siteUser.map { "  and site.site_id = " + _.siteId }.getOrElse("") +
    """
      and not exists (
        select coalesce(metadata, 0) from site_item_numeric_metadata
        where item.item_id = site_item_numeric_metadata.item_id
        and site.site_id = site_item_numeric_metadata.site_id
        and site_item_numeric_metadata.metadata_type = 
      """ + SiteItemNumericMetadataType.HIDE.ordinal +
      """
        and site_item_numeric_metadata.metadata = 1
      )
      and item_description.locale_id = {localeId}
      and item_price_history.item_price_history_id = (
        select iph.item_price_history_id
        from item_price_history iph
        where
          iph.item_price_id = item_price.item_price_id and
          iph.valid_until > {now}
          order by iph.valid_until
          limit 1
      )
    """ +
    createQueryConditionSql(queryString)

    val columns = if (ItemListQueryColumnsToAdd.isEmpty) "*" else "*, " + ItemListQueryColumnsToAdd

    val sql = SQL(
      s"select $columns from item $sqlBody order by $orderBy limit {pageSize} offset {offset}"
    )

    val list = applyQueryString(queryString, sql)
      .on(
        'localeId -> locale.id,
        'pageSize -> pageSize,
        'offset -> page * pageSize,
        'now -> new Timestamp(now)
      ).as(
        itemListParser *
      ).map {e => {
        val itemId = e._1.id.get
        val metadata = ItemNumericMetadata.allById(itemId)
        val textMetadata = ItemTextMetadata.allById(itemId)
        val siteMetadata = SiteItemNumericMetadata.all(e._5.id.get, itemId)

        (e._1, e._2, e._3, e._5, e._4, metadata, siteMetadata, textMetadata)
      }}

    val countSql = SQL(
      "select count(*) from item" + sqlBody
    )

    val count = applyQueryString(queryString, countSql)
      .on(
        'localeId -> locale.id,
        'now -> new Timestamp(now)
      ).as(
        SqlParser.scalar[Long].single
      )

    PagedRecords(page, pageSize, (count + pageSize - 1) / pageSize, orderBy, list)
  }

  def createQueryConditionSql(q: QueryString): String = {
    val buf = new StringBuilder

    @tailrec def createQueryConditionSql(idx: Int): String = {
        if (idx < q.size) {
          buf.append(
            f"and (item_name.item_name like {query$idx%d} or item_description.description like {query$idx%d}) "
          )
          createQueryConditionSql(idx + 1)
        }
      else buf.toString
    }

    createQueryConditionSql(0)
  }

  def applyQueryString(queryString: QueryString, sql: SimpleSql[Row]): SimpleSql[Row] = {
    @tailrec
    def applyQueryString(idx: Int, queryString: QueryString, sql: SimpleSql[Row]): SimpleSql[Row] = 
      queryString.toList match {
        case List() => sql
        case head::tail => applyQueryString(
          idx + 1, QueryString(tail), sql.on(Symbol("query" + idx) -> ("%" + head + "%"))
        )
      }

    applyQueryString(0, queryString, sql)
  }

  def listBySite(
    site: Site, locale: LocaleInfo, queryString: String, page: Int = 0, pageSize: Int = 10,
    now: Long = System.currentTimeMillis
  )(
    implicit conn: Connection
  ): Seq[(Item, ItemName, ItemDescription, ItemPrice, ItemPriceHistory, Map[ItemNumericMetadataType, ItemNumericMetadata])] =
    listBySiteId(site.id.get, locale, queryString, page, pageSize, now)

  def listBySiteId(
    siteId: Long, locale: LocaleInfo, queryString: String, page: Int = 0, pageSize: Int = 10,
    now: Long = System.currentTimeMillis
  )(
    implicit conn: Connection
  ): Seq[
    (Item, ItemName, ItemDescription, ItemPrice, ItemPriceHistory, Map[ItemNumericMetadataType, ItemNumericMetadata])
  ]  =
    SQL(
      """
      select * from item
      inner join item_name on item.item_id = item_name.item_id
      inner join item_description on item.item_id = item_description.item_id
      inner join item_price on item.item_id = item_price.item_id
      inner join site_item on item.item_id = site_item.item_id and item_price.site_id = site_item.site_id
      inner join site on site.site_id = site_item.site_id
      where item_name.locale_id = {localeId}
      and item_description.locale_id = {localeId}
      and item_description.site_id = {siteId}
      and item_price.site_id = {siteId}
      and site_item.site_id = {siteId}
      and (item_name.item_name like {query} or item_description.description like {query})
      order by item_name.item_name
      limit {pageSize} offset {offset}
      """
    ).on(
      'localeId -> locale.id,
      'siteId -> siteId,
      'query -> ("%" + queryString + "%"),
      'pageSize -> pageSize,
      'offset -> page * pageSize
    ).as(
      itemParser *
    ).map { e => {
      val itemId = e._1.id.get
      val itemPriceId = e._4.id.get
      val priceHistory = ItemPriceHistory.at(itemPriceId, now)
      val metadata = ItemNumericMetadata.allById(itemId)
      (e._1, e._2, e._3, e._4, priceHistory, metadata)
    }}

  def createItem(prototype: CreateItem)(implicit conn: Connection) {
    val item = Item.createNew(prototype.categoryId)
    val name = ItemName.createNew(item, Map(LocaleInfo(prototype.localeId) -> prototype.itemName))
    val site = Site(prototype.siteId)
    val desc = ItemDescription.createNew(item, site, prototype.description)
    val price = ItemPrice.createNew(item, site)
    val tax = Tax(prototype.taxId)
    val priceHistory = ItemPriceHistory.createNew(price, tax, prototype.currency, prototype.price, prototype.costPrice, Until.Ever)
    val siteItem = SiteItem.createNew(site, item)
  }

  def changeCategory(itemId: Long, categoryId: Long)(implicit conn: Connection) {
    SQL(
      "update item set category_id = {categoryId} where item_id = {itemId}"
    ).on(
      'itemId -> itemId,
      'categoryId -> categoryId
    ).executeUpdate()
  }
}

object ItemName {
  val simple = {
    SqlParser.get[Long]("item_name.locale_id") ~
    SqlParser.get[Long]("item_name.item_id") ~
    SqlParser.get[String]("item_name.item_name") map {
      case localeId~itemId~name => ItemName(localeId, itemId, name)
    }
  }

  def createNew(
    item: Item, names: Map[LocaleInfo, String]
  )(implicit conn: Connection): Map[LocaleInfo, ItemName] = {
    names.transform { (k, v) => {
      SQL(
        """
        insert into item_name (item_name_id, locale_id, item_id, item_name)
        values (
          (select nextval('item_name_seq')),
          {localeId}, {itemId}, {itemName}
        )
        """
      ).on(
        'localeId -> k.id,
        'itemId -> item.id.get,
        'itemName -> v
      ).executeUpdate()
      
      ItemName(k.id, item.id.get, v)
    }}
  }

  def list(item: Item)(implicit conn: Connection): Map[LocaleInfo, ItemName] = list(item.id.get)

  def list(itemId: Long)(implicit conn: Connection): Map[LocaleInfo, ItemName] = {
    SQL(
      "select * from item_name where item_id = {itemId}"
    ).on(
      'itemId -> itemId
    ).as(simple *).map { e =>
      LocaleInfo(e.localeId) -> e
    }.toMap
  }

  def add(itemId: Long, localeId: Long, itemName: String)(implicit conn: Connection) {
    SQL(
      """
      insert into item_name (item_name_id, item_id, locale_id, item_name)
      values (
        (select nextval('item_name_seq')),
        {itemId}, {localeId}, {itemName}
      )
      """
    ).on(
      'itemId -> itemId,
      'localeId -> localeId,
      'itemName -> itemName
    ).executeUpdate()
  }

  def remove(id: Long)(implicit conn: Connection) {
    SQL(
      "delete from item_name where item_name_id = {id}"
    )
    .on(
      'id -> id
    ).executeUpdate()
  }

  def remove(itemId: Long, localeId: Long)(implicit conn: Connection) {
    SQL(
      """
      delete from item_name
      where item_id = {itemId}
      and locale_id = {localeId}
      and (select count(*) from item_name where item_id = {itemId}) > 1
      """
    )
    .on(
      'itemId -> itemId,
      'localeId -> localeId
    ).executeUpdate()
  }

  def update(itemId: Long, localeId: Long, itemName: String)(implicit conn: Connection) {
    SQL(
      """
      update item_name set item_name = {itemName}
      where item_id = {itemId} and locale_id = {localeId}
      """
    ).on(
      'itemName -> itemName,
      'itemId -> itemId,
      'localeId -> localeId
    ).executeUpdate()
  }
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

  def createNew(
    item: Item, site: Site, description: String
  )(implicit conn: Connection): ItemDescription = {
    SQL(
      """
      insert into item_description (item_description_id, locale_id, item_id, site_id, description)
      values (
        (select nextval('item_description_seq')),
        {localeId}, {itemId}, {siteId}, {description}
      )
      """
    ).on(
      'localeId -> site.localeId,
      'itemId -> item.id.get,
      'siteId -> site.id.get,
      'description -> description
    ).executeUpdate()

    ItemDescription(site.localeId, item.id.get, site.id.get, description)
  }

  def list(item: Item)(implicit conn: Connection): Seq[(Long, LocaleInfo, ItemDescription)] = list(item.id.get)

  def list(itemId: Long)(implicit conn: Connection): Seq[(Long, LocaleInfo, ItemDescription)] =
    SQL(
      """
      select * from item_description
      inner join site on site.site_id = item_description.site_id
      where item_id = {itemId}
      order by site.site_name, item_description.locale_id
      """
    ).on(
      'itemId -> itemId
    ).as(simple *).map { e =>
      (e.siteId, LocaleInfo(e.localeId), e)
    }.toSeq

  def add(siteId: Long, itemId: Long, localeId: Long, itemDescription: String)(implicit conn: Connection) {
    SQL(
      """
      insert into item_description (item_description_id, site_id, item_id, locale_id, description)
      values (
        (select nextval('item_description_seq')),
        {siteId}, {itemId}, {localeId}, {itemDescription}
      )
      """
    ).on(
      'siteId -> siteId,
      'itemId -> itemId,
      'localeId -> localeId,
      'itemDescription -> itemDescription
    ).executeUpdate()
  }

  def remove(siteId: Long, itemId: Long, localeId: Long)(implicit conn: Connection) {
    SQL(
      """
      delete from item_description
      where site_id = {siteId}
      and item_id = {itemId}
      and locale_id = {localeId}
      and (select count(*) from item_description where item_id = {itemId}) > 1
      """
    )
    .on(
      'siteId -> siteId,
      'itemId -> itemId,
      'localeId -> localeId
    ).executeUpdate()
  }

  def update(siteId: Long, itemId: Long, localeId: Long, itemDescription: String)(implicit conn: Connection) {
    SQL(
      """
      update item_description set description = {itemDescription}
      where site_id = {siteId} and item_id = {itemId} and locale_id = {localeId}
      """
    ).on(
      'itemDescription -> itemDescription,
      'siteId -> siteId,
      'itemId -> itemId,
      'localeId -> localeId
    ).executeUpdate()
  }
}

object ItemPrice {
  val simple = {
    SqlParser.get[Option[Long]]("item_price.item_price_id") ~
    SqlParser.get[Long]("item_price.site_id") ~
    SqlParser.get[Long]("item_price.item_id") map {
      case id~siteId~itemId => ItemPrice(id, siteId, itemId)
    }
  }

  def createNew(item: Item, site: Site)(implicit conn: Connection): ItemPrice =
    add(item.id.get, site.id.get)

  def add(itemId: Long, siteId: Long)(implicit conn: Connection): ItemPrice = {
    SQL(
      """
      insert into item_price (item_price_id, site_id, item_id)
      values ((select nextval('item_price_seq')), {siteId}, {itemId})
      """
    ).on(
      'siteId -> siteId,
      'itemId -> itemId
    ).executeUpdate()

    val itemPriceId = SQL("select currval('item_price_seq')").as(SqlParser.scalar[Long].single)

    ItemPrice(Some(itemPriceId), siteId, itemId)
  }

  def get(site: Site, item: Item)(implicit conn: Connection): Option[ItemPrice] = {
    SQL(
      "select * from item_price where item_id = {itemId} and site_id = {siteId}"
    ).on(
      'itemId -> item.id.get,
      'siteId -> site.id.get
    ).as(
      ItemPrice.simple.singleOpt
    )
  }

  def remove(itemId: Long, siteId: Long)(implicit conn: Connection) {
    SQL(
      "delete from item_price where item_id = {itemId} and site_id = {siteId}"
    ).on(
      'itemId -> itemId,
      'siteId -> siteId
    ).executeUpdate()
  }
}

object ItemPriceHistory {
  val simple = {
    SqlParser.get[Option[Long]]("item_price_history.item_price_history_id") ~
    SqlParser.get[Long]("item_price_history.item_price_id") ~
    SqlParser.get[Long]("item_price_history.tax_id") ~
    SqlParser.get[Long]("item_price_history.currency_id") ~
    SqlParser.get[java.math.BigDecimal]("item_price_history.unit_price") ~
    SqlParser.get[java.math.BigDecimal]("item_price_history.cost_price") ~
    SqlParser.get[java.util.Date]("item_price_history.valid_until") map {
      case id~itemPriceId~taxId~currencyId~unitPrice~costPrice~validUntil
        => ItemPriceHistory(id, itemPriceId, taxId, CurrencyInfo(currencyId), unitPrice, costPrice, validUntil.getTime)
    }
  }

  def createNew(
    itemPrice: ItemPrice, tax: Tax, currency: CurrencyInfo, unitPrice: BigDecimal, costPrice: BigDecimal, validUntil: Long
  )(implicit conn: Connection) : ItemPriceHistory = {
    SQL(
      """
      insert into item_price_history(
        item_price_history_id, item_price_id, tax_id, currency_id, unit_price, cost_price, valid_until
      ) values (
        (select nextval('item_price_history_seq')),
        {itemPriceId}, {taxId}, {currencyId}, {unitPrice}, {costPrice}, {validUntil}
      )
      """
    ).on(
      'itemPriceId -> itemPrice.id.get,
      'taxId -> tax.id.get,
      'currencyId -> currency.id,
      'unitPrice -> unitPrice.bigDecimal,
      'costPrice -> costPrice.bigDecimal,
      'validUntil -> new java.sql.Timestamp(validUntil)
    ).executeUpdate()

    val id = SQL("select currval('item_price_history_seq')").as(SqlParser.scalar[Long].single)

    ItemPriceHistory(Some(id), itemPrice.id.get, tax.id.get, currency, unitPrice, costPrice, validUntil)
  }

  def update(
    id: Long, taxId: Long, currencyId: Long, unitPrice: BigDecimal, costPrice: BigDecimal, validUntil: DateTime
  )(implicit conn: Connection) {
    SQL(
      """
      update item_price_history
      set tax_id = {taxId},
      currency_id = {currencyId},
      unit_price = {unitPrice},
      cost_price = {costPrice},
      valid_until = {validUntil}
      where item_price_history_id = {id}
      """
    ).on(
      'taxId -> taxId,
      'currencyId -> currencyId,
      'unitPrice -> unitPrice.bigDecimal,
      'costPrice -> costPrice.bigDecimal,
      'validUntil -> new java.sql.Timestamp(validUntil.getMillis),
      'id -> id
    ).executeUpdate()
  }

  def add(
    itemId: Long, siteId: Long, taxId: Long, currencyId: Long, 
    unitPrice: BigDecimal, costPrice: BigDecimal, validUntil: DateTime
  )(implicit conn: Connection) {
    val priceId = SQL(
      """
      select item_price_id from item_price
      where site_id = {siteId}
      and item_id = {itemId}
      """
    ).on(
      'siteId -> siteId,
      'itemId -> itemId
    ).as(SqlParser.scalar[Long].single)

    SQL(
      """
      insert into item_price_history
      (item_price_history_id, item_price_id, tax_id, currency_id, unit_price, cost_price, valid_until)
      values (
        (select nextval('item_price_history_seq')),
        {itemPriceId}, {taxId}, {currencyId}, {unitPrice}, {costPrice}, {validUntil}
      )
      """
    ).on(
      'itemPriceId -> priceId,
      'taxId -> taxId,
      'currencyId -> currencyId,
      'unitPrice -> unitPrice.bigDecimal,
      'costPrice -> costPrice.bigDecimal,
      'validUntil -> new java.sql.Timestamp(validUntil.getMillis)
    ).executeUpdate()
  }

  def list(itemPrice: ItemPrice)(implicit conn: Connection): Seq[ItemPriceHistory] = SQL(
    "select * from item_price_history where item_price_id = {itemPriceId}"
  ).on(
    'itemPriceId -> itemPrice.id.get
  ).as(
    ItemPriceHistory.simple *
  )

  def at(
    itemPriceId: Long, now: Long = System.currentTimeMillis
  )(implicit conn: Connection): ItemPriceHistory = SQL(
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

  def atBySiteAndItem(
    siteId: Long, itemId: Long, now: Long = System.currentTimeMillis
  )(implicit conn: Connection): ItemPriceHistory =
    SQL(
      """
      select * from item_price_history
      where item_price_id = (
        select item_price_id from item_price
        where item_price.item_id = {itemId} and item_price.site_id = {siteId}
      )
      and item_price_history.item_price_id = item_price_id
      and {now} < valid_until
      order by valid_until
      limit 1
      """
    ).on(
      'itemId -> itemId,
      'siteId -> siteId,
      'now -> new java.sql.Timestamp(now)
    ).as(
      ItemPriceHistory.simple.single
    )

  val withItemPrice = ItemPrice.simple~simple map {
    case price~priceHistory => (price, priceHistory)
  }

  def listByItemId(itemId: Long)(implicit conn: Connection): Seq[(ItemPrice, ItemPriceHistory)] =
    SQL(
      """
      select * from item_price_history
      inner join item_price on item_price_history.item_price_id = item_price.item_price_id
      inner join site on site.site_id = item_price.site_id
      where item_price.item_id = {itemId}
      order by site.site_name, item_price_history.valid_until
      """
    ).on(
      'itemId -> itemId
    ).as(
      withItemPrice *
    ).toSeq

  def remove(itemId: Long, siteId: Long, id: Long)(implicit conn: Connection) {
    SQL(
      """
      delete from item_price_history
      where item_price_history_id = {id}
      and (
        select count(*) from item_price_history
        inner join item_price on item_price_history.item_price_id = item_price.item_price_id
        where item_price.item_id = {itemId}
        and item_price.site_id = {siteId}
      ) > 1
      """
    ).on(
      'id -> id,
      'itemId -> itemId,
      'siteId -> siteId
    ).executeUpdate()
  }
}

object ItemNumericMetadata {
  val simple = {
    SqlParser.get[Option[Long]]("item_numeric_metadata.item_numeric_metadata_id") ~
    SqlParser.get[Long]("item_numeric_metadata.item_id") ~
    SqlParser.get[Int]("item_numeric_metadata.metadata_type") ~
    SqlParser.get[Long]("item_numeric_metadata.metadata") map {
      case id~itemId~metadata_type~metadata =>
        ItemNumericMetadata(id, itemId, ItemNumericMetadataType.byIndex(metadata_type), metadata)
    }
  }

  def createNew(
    item: Item, metadataType: ItemNumericMetadataType, metadata: Long
  )(implicit conn: Connection): ItemNumericMetadata = add(item.id.get, metadataType, metadata)

  def add(
    itemId: Long, metadataType: ItemNumericMetadataType, metadata: Long
  )(implicit conn: Connection): ItemNumericMetadata = {
    SQL(
      """
      insert into item_numeric_metadata(item_numeric_metadata_id, item_id, metadata_type, metadata)
      values (
        (select nextval('item_numeric_metadata_seq')),
        {itemId}, {metadataType}, {metadata}
      )
      """
    ).on(
      'itemId -> itemId,
      'metadataType -> metadataType.ordinal,
      'metadata -> metadata
    ).executeUpdate()

    val id = SQL("select currval('item_numeric_metadata_seq')").as(SqlParser.scalar[Long].single)

    ItemNumericMetadata(Some(id), itemId, ItemNumericMetadataType.byIndex(metadataType.ordinal), metadata)
  }

  def apply(
    item: Item, metadataType: ItemNumericMetadataType
  )(implicit conn: Connection): ItemNumericMetadata = SQL(
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

  def all(item: Item)(implicit conn: Connection): Map[ItemNumericMetadataType, ItemNumericMetadata] = allById(item.id.get)

  def allById(itemId: Long)(implicit conn: Connection): Map[ItemNumericMetadataType, ItemNumericMetadata] = SQL(
    "select * from item_numeric_metadata where item_id = {itemId} "
  ).on(
    'itemId -> itemId
  ).as(
    ItemNumericMetadata.simple *
  ).foldLeft(new HashMap[ItemNumericMetadataType, ItemNumericMetadata]) {
    (map, e) => map.updated(e.metadataType, e)
  }

  def update(itemId: Long, metadataType: ItemNumericMetadataType, metadata: Long)(implicit conn: Connection) {
    SQL(
      """
      update item_numeric_metadata set metadata = {metadata}
      where item_id = {itemId} and metadata_type = {metadataType}
      """
    ).on(
      'metadata -> metadata,
      'itemId -> itemId,
      'metadataType -> metadataType.ordinal
    ).executeUpdate()
  }

  def remove(itemId: Long, metadataType: Int)(implicit conn: Connection) {
    SQL(
      """
      delete from item_numeric_metadata
      where item_id = {itemId}
      and metadata_type = {metadataType}
      """
    ).on(
      'itemId -> itemId,
      'metadataType -> metadataType
    ).executeUpdate()
  }
}

object ItemTextMetadata {
  val simple = {
    SqlParser.get[Option[Long]]("item_text_metadata.item_text_metadata_id") ~
    SqlParser.get[Long]("item_text_metadata.item_id") ~
    SqlParser.get[Int]("item_text_metadata.metadata_type") ~
    SqlParser.get[String]("item_text_metadata.metadata") map {
      case id~itemId~metadata_type~metadata =>
        ItemTextMetadata(id, itemId, ItemTextMetadataType.byIndex(metadata_type), metadata)
    }
  }

  def createNew(
    item: Item, metadataType: ItemTextMetadataType, metadata: String
  )(implicit conn: Connection): ItemTextMetadata = add(item.id.get, metadataType, metadata)

  def add(
    itemId: Long, metadataType: ItemTextMetadataType, metadata: String
  )(implicit conn: Connection): ItemTextMetadata = {
    SQL(
      """
      insert into item_text_metadata(item_text_metadata_id, item_id, metadata_type, metadata)
      values (
        (select nextval('item_text_metadata_seq')),
        {itemId}, {metadataType}, {metadata}
      )
      """
    ).on(
      'itemId -> itemId,
      'metadataType -> metadataType.ordinal,
      'metadata -> metadata
    ).executeUpdate()

    val id = SQL("select currval('item_text_metadata_seq')").as(SqlParser.scalar[Long].single)

    ItemTextMetadata(Some(id), itemId, ItemTextMetadataType.byIndex(metadataType.ordinal), metadata)
  }

  def apply(
    item: Item, metadataType: ItemTextMetadataType
  )(implicit conn: Connection): ItemTextMetadata = SQL(
    """
    select * from item_text_metadata
    where item_id = {itemId}
    and metadata_type = {metadataType}
    """
  ).on(
    'itemId -> item.id.get,
    'metadataType -> metadataType.ordinal
  ).as(
    ItemTextMetadata.simple.single
  )

  def all(item: Item)(implicit conn: Connection): Map[ItemTextMetadataType, ItemTextMetadata] = allById(item.id.get)

  def allById(itemId: Long)(implicit conn: Connection): Map[ItemTextMetadataType, ItemTextMetadata] = SQL(
    "select * from item_text_metadata where item_id = {itemId} "
  ).on(
    'itemId -> itemId
  ).as(
    ItemTextMetadata.simple *
  ).foldLeft(new HashMap[ItemTextMetadataType, ItemTextMetadata]) {
    (map, e) => map.updated(e.metadataType, e)
  }

  def update(itemId: Long, metadataType: ItemTextMetadataType, metadata: String)(implicit conn: Connection) {
    SQL(
      """
      update item_text_metadata set metadata = {metadata}
      where item_id = {itemId} and metadata_type = {metadataType}
      """
    ).on(
      'metadata -> metadata,
      'itemId -> itemId,
      'metadataType -> metadataType.ordinal
    ).executeUpdate()
  }

  def remove(itemId: Long, metadataType: Int)(implicit conn: Connection) {
    SQL(
      """
      delete from item_text_metadata
      where item_id = {itemId}
      and metadata_type = {metadataType}
      """
    ).on(
      'itemId -> itemId,
      'metadataType -> metadataType
    ).executeUpdate()
  }
}

object SiteItemNumericMetadata {
  val simple = {
    SqlParser.get[Option[Long]]("site_item_numeric_metadata.site_item_numeric_metadata_id") ~
    SqlParser.get[Long]("site_item_numeric_metadata.item_id") ~
    SqlParser.get[Long]("site_item_numeric_metadata.site_id") ~
    SqlParser.get[Int]("site_item_numeric_metadata.metadata_type") ~
    SqlParser.get[Long]("site_item_numeric_metadata.metadata") map {
      case id~itemId~siteId~metadataType~metadata =>
        SiteItemNumericMetadata(id, itemId, siteId, SiteItemNumericMetadataType.byIndex(metadataType), metadata)
    }
  }

  def createNew(
    siteId: Long, itemId: Long, metadataType: SiteItemNumericMetadataType, metadata: Long
  )(implicit conn: Connection): SiteItemNumericMetadata = {
    SQL(
      """
      insert into site_item_numeric_metadata(
        site_item_numeric_metadata_id, site_id, item_id, metadata_type, metadata
      ) values (
        (select nextval('site_item_numeric_metadata_seq')),
        {siteId}, {itemId}, {metadataType}, {metadata}
      )
      """
    ).on(
      'siteId -> siteId,
      'itemId -> itemId,
      'metadataType -> metadataType.ordinal,
      'metadata -> metadata
    ).executeUpdate()

    val id = SQL("select currval('site_item_numeric_metadata_seq')").as(SqlParser.scalar[Long].single)

    SiteItemNumericMetadata(Some(id), siteId, itemId, metadataType, metadata)
  }

  def add(
    itemId: Long, siteId: Long, metadataType: SiteItemNumericMetadataType, metadata: Long
  )(implicit conn: Connection): SiteItemNumericMetadata = {
    SQL(
      """
      insert into site_item_numeric_metadata(
        site_item_numeric_metadata_id, item_id, site_id, metadata_type, metadata
      ) values (
        (select nextval('site_item_numeric_metadata_seq')),
        {itemId}, {siteId}, {metadataType}, {metadata}
      )
      """
    ).on(
      'itemId -> itemId,
      'siteId -> siteId,
      'metadataType -> metadataType.ordinal,
      'metadata -> metadata
    ).executeUpdate()

    val id = SQL("select currval('site_item_numeric_metadata_seq')").as(SqlParser.scalar[Long].single)

    SiteItemNumericMetadata(Some(id), itemId, siteId, SiteItemNumericMetadataType.byIndex(metadataType.ordinal), metadata)
  }

  def apply(
    siteId: Long, itemId: Long, metadataType: SiteItemNumericMetadataType
  )(implicit conn: Connection): SiteItemNumericMetadata = SQL(
    """
    select * from site_item_numeric_metadata
    where site_id = {siteId} and item_id = {itemId}
    and metadata_type = {metadataType}
    """
  ).on(
    'siteId -> siteId,
    'itemId -> itemId,
    'metadataType -> metadataType.ordinal
  ).as(
    SiteItemNumericMetadata.simple.single
  )

  def all(siteId: Long, itemId: Long)(implicit conn: Connection): Map[SiteItemNumericMetadataType, SiteItemNumericMetadata] = SQL(
    "select * from site_item_numeric_metadata where site_id = {siteId} and item_id = {itemId}"
  ).on(
    'siteId -> siteId,
    'itemId -> itemId
  ).as(
    SiteItemNumericMetadata.simple *
  ).foldLeft(new HashMap[SiteItemNumericMetadataType, SiteItemNumericMetadata]) {
    (map, e) => map.updated(e.metadataType, e)
  }

  def update(itemId: Long, siteId: Long, metadataType: SiteItemNumericMetadataType, metadata: Long)(implicit conn: Connection) {
    SQL(
      """
      update site_item_numeric_metadata set metadata = {metadata}
      where item_id = {itemId} and site_id = {siteId} and metadata_type = {metadataType}
      """
    ).on(
      'metadata -> metadata,
      'itemId -> itemId,
      'siteId -> siteId,
      'metadataType -> metadataType.ordinal
    ).executeUpdate()
  }

  def remove(itemId: Long, siteId: Long, metadataType: Int)(implicit conn: Connection) {
    SQL(
      """
      delete from site_item_numeric_metadata
      where item_id = {itemId} and site_id = {siteId} and metadata_type = {metadataType}
      """
    ).on(
      'itemId -> itemId,
      'siteId -> siteId,
      'metadataType -> metadataType
    ).executeUpdate()
  }

  def allById(
    itemId: Long
  )(implicit conn: Connection): Map[(Long, SiteItemNumericMetadataType), SiteItemNumericMetadata] = SQL(
    "select * from site_item_numeric_metadata where item_id = {itemId} "
  ).on(
    'itemId -> itemId
  ).as(
    SiteItemNumericMetadata.simple *
  ).foldLeft(new HashMap[(Long, SiteItemNumericMetadataType), SiteItemNumericMetadata]) {
    (map, e) => map.updated((e.siteId, e.metadataType), e)
  }
}

object SiteItem {
  val simple = {
    SqlParser.get[Long]("site_item.item_id") ~
    SqlParser.get[Long]("site_item.site_id") map {
      case itemId~siteId => SiteItem(itemId, siteId)
    }
  }

  val withSite = Site.simple ~ SiteItem.simple map {
    case site~siteItem => (site, siteItem)
  }

  def list(itemId: Long)(implicit conn: Connection): Seq[(Site, SiteItem)] =
    SQL(
      """
      select * from site_item
      inner join site on site_item.site_id = site.site_id
      where site_item.item_id = {itemId}
      """
    ).on(
      'itemId -> itemId
    ).as(
      withSite *
    )

  def createNew(site: Site, item: Item)(implicit conn: Connection): SiteItem = add(item.id.get, site.id.get)

  def add(itemId: Long, siteId: Long)(implicit conn: Connection): SiteItem = {
    SQL(
      "insert into site_item (item_id, site_id) values ({itemId}, {siteId})"
    ).on(
      'itemId -> itemId,
      'siteId -> siteId
    ).executeUpdate()

    SiteItem(itemId, siteId)
  }

  def remove(itemId: Long, siteId: Long)(implicit conn: Connection) {
    SQL(
      "delete from site_item where item_id = {itemId} and site_id = {siteId}"
    ).on(
      'itemId -> itemId,
      'siteId -> siteId
    ).executeUpdate()
  }
}

