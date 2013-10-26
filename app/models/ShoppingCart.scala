package models

import anorm._
import anorm.{NotAssigned, Pk}
import anorm.SqlParser
import model.Until
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import collection.immutable.{LongMap, HashSet, HashMap, IntMap}
import java.sql.Connection
import play.api.data.Form
import org.joda.time.DateTime

case class ShoppingCartTotalEntry(
  shoppingCartItem: ShoppingCartItem,
  itemName: ItemName,
  itemDescription: ItemDescription,
  site: Site,
  itemPriceHistory: ItemPriceHistory,
  taxHistory: TaxHistory,
  itemNumericMetadata: Map[ItemNumericMetadataType, ItemNumericMetadata],
  siteItemNumericMetadata: Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]
) extends NotNull {
  lazy val unitPrice: BigDecimal = itemPriceHistory.unitPrice
  lazy val quantity: Int = shoppingCartItem.quantity
  lazy val itemPrice: BigDecimal = unitPrice * quantity
}

case class ShoppingCartTotal(
  table: Seq[ShoppingCartTotalEntry] = List()
) extends NotNull {
  def +(e: ShoppingCartTotalEntry) = ShoppingCartTotal(table :+ e)
  lazy val size: Int = table.size
  lazy val isEmpty: Boolean = table.isEmpty
  lazy val notEmpty: Boolean = (! table.isEmpty)
  lazy val quantity: Int = table.foldLeft(0)(_ + _.quantity)
  lazy val total: BigDecimal = table.foldLeft(BigDecimal(0))(_ + _.itemPrice) // Excluding outer tax
  lazy val sites: Seq[Site] = table.foldLeft(new HashSet[Site])(_ + _.site).toSeq
  lazy val taxTotal: BigDecimal = taxByType.values.foldLeft(BigDecimal(0))(_ + _)
  lazy val taxHistoryById: LongMap[TaxHistory] = table.foldLeft(LongMap[TaxHistory]()) {
    (sum, e) => sum.updated(e.taxHistory.taxId, e.taxHistory)
  }
  lazy val sumByTaxId = table.foldLeft(LongMap().withDefaultValue(BigDecimal(0))) {
    (sum, e) => sum.updated(
      e.taxHistory.taxId,
      e.itemPriceHistory.unitPrice * e.shoppingCartItem.quantity + sum(e.taxHistory.taxId)
    )
  }
  lazy val taxByType: Map[TaxType, BigDecimal] = {
    sumByTaxId.foldLeft(HashMap[TaxType, BigDecimal]().withDefaultValue(BigDecimal(0))) {
      (sum, e) => {
        val taxHistory = taxHistoryById(e._1)
        val taxType = taxHistory.taxType
        sum.updated(taxType, sum(taxType) + taxHistory.taxAmount(e._2))
      }
    }
  }
  lazy val taxAmount: BigDecimal = taxByType.values.foldLeft(BigDecimal(0)){_ + _}
  lazy val bySite: Map[Site, ShoppingCartTotal] =
    table.foldLeft(
      HashMap[Site, Vector[ShoppingCartTotalEntry]]()
        .withDefaultValue(Vector[ShoppingCartTotalEntry]())
    ) { (map, e) =>
      map.updated(e.site, map(e.site).+:(e))
    }.mapValues(e => ShoppingCartTotal(e.toSeq))
  def apply(index: Int): ShoppingCartTotalEntry = table(index)
}

case class ShoppingCart(
  items: Seq[ShoppingCartItem]
) extends NotNull

case class ShoppingCartItem(
  id: Pk[Long] = NotAssigned, storeUserId: Long, sequenceNumber: Int,
  siteId: Long, itemId: Long, quantity: Int
) extends NotNull

case class ShoppingCartShipping(
  id: Pk[Long] = NotAssigned,
  storeUserId: Long,
  siteId: Long,
  shippingDate: Long
) extends NotNull

object ShoppingCartItem {
  val simple = {
    SqlParser.get[Pk[Long]]("shopping_cart_item.shopping_cart_item_id") ~
    SqlParser.get[Long]("shopping_cart_item.store_user_id") ~
    SqlParser.get[Int]("shopping_cart_item.seq") ~
    SqlParser.get[Long]("shopping_cart_item.site_id") ~
    SqlParser.get[Long]("shopping_cart_item.item_id") ~
    SqlParser.get[Int]("shopping_cart_item.quantity") map {
      case id~userId~seq~siteId~itemId~quantity =>
        ShoppingCartItem(id, userId, seq, siteId, itemId, quantity)
    }
  }

  def sites(userId: Long)(implicit conn: Connection): Seq[Long] = SQL(
    """
    select distinct site_id from shopping_cart_item
    where store_user_id = {userId}
    """
  ).on(
    'userId -> userId
  ).as(
    SqlParser.scalar[Long] *
  )

  def addItem(userId: Long, siteId: Long, itemId: Long, quantity: Int)(implicit conn: Connection): ShoppingCartItem = {
    SQL(
      """
      insert into shopping_cart_item (shopping_cart_item_id, store_user_id, seq, site_id, item_id, quantity)
      values (
        (select nextval('shopping_cart_item_seq')),
        {userId},
        (select coalesce(max(seq), 0) + 1 from shopping_cart_item where store_user_id = {userId}),
        {siteId},
        {itemId},
        {quantity}
      )
      """
    ).on(
      'userId ->userId,
      'siteId -> siteId,
      'itemId -> itemId,
      'quantity -> quantity
    ).executeUpdate()
    
    val id = SQL("select currval('shopping_cart_item_seq')").as(SqlParser.scalar[Long].single)
    val seq = SQL(
      "select seq from shopping_cart_item where shopping_cart_item_id = {id}"
    ).on('id -> id).as(SqlParser.scalar[Int].single)

    ShoppingCartItem(Id(id), userId, seq, siteId, itemId, quantity)
  }

  def remove(id: Long, userId: Long)(implicit conn: Connection): Int =
    SQL(
      """
      delete from shopping_cart_item
      where shopping_cart_item_id = {id} and store_user_id = {userId}
      """
    ).on(
      'id -> id,
      'userId -> userId
    ).executeUpdate()

  def removeForUser(userId: Long)(implicit conn: Connection) {
    SQL(
      "delete from shopping_cart_item where store_user_id = {id}"
    ).on(
      'id -> userId
    ).executeUpdate()
  }

  val listParser = ShoppingCartItem.simple~ItemName.simple~ItemDescription.simple~ItemPrice.simple~Site.simple map {
    case cart~itemName~itemDescription~itemPrice~site => (
      cart, itemName, itemDescription, itemPrice, site
    )
  }

  def listItemsForUser(
    locale: LocaleInfo, userId: Long, page: Int = 0, pageSize: Int = 100, now: Long = System.currentTimeMillis
  )(
    implicit conn: Connection
  ): ShoppingCartTotal = ShoppingCartTotal(
    SQL(
      """
      select * from shopping_cart_item
      inner join item_name on shopping_cart_item.item_id = item_name.item_id
      inner join item_description on shopping_cart_item.item_id = item_description.item_id
      inner join item_price on shopping_cart_item.item_id = item_price.item_id 
      inner join site_item on shopping_cart_item.item_id = site_item.item_id and item_price.site_id = site_item.site_id
      inner join site on site_item.site_id = site.site_id and shopping_cart_item.site_id = site.site_id
      where item_name.locale_id = {localeId}
      and item_description.locale_id = {localeId}
      and shopping_cart_item.store_user_id = {userId}
      order by seq
      limit {pageSize} offset {offset}
      """
    ).on(
      'localeId -> locale.id,
      'userId -> userId,
      'pageSize -> pageSize,
      'offset -> page * pageSize
    ).as(
      listParser *
    ).map { e =>
      val itemId = e._1.itemId
      val itemPriceId = e._4.id.get
      val priceHistory = ItemPriceHistory.at(itemPriceId, now)
      val taxHistory = TaxHistory.at(priceHistory.taxId, now)
      val metadata = ItemNumericMetadata.allById(itemId)
      val siteMetadata = SiteItemNumericMetadata.all(e._5.id.get, itemId)

      ShoppingCartTotalEntry(e._1, e._2, e._3, e._5, priceHistory, taxHistory, metadata, siteMetadata)
    }
  )

  def changeQuantity(id: Long, userId: Long, quantity: Int)(implicit conn: Connection): Int = {
    SQL(
      """
      update shopping_cart_item set quantity = {quantity}
      where shopping_cart_item_id = {id} and store_user_id = {userId}
      """
    ).on(
      'quantity -> quantity,
      'id ->id,
      'userId -> userId
    ).executeUpdate()
  }

  def apply(id: Long)(implicit conn: Connection): ShoppingCartItem =
    SQL(
      "select * from shopping_cart_item where shopping_cart_item_id = {id}"
    ).on(
      'id -> id
    ).as(simple.single)
}

object ShoppingCartShipping {
  val simple = {
    SqlParser.get[Pk[Long]]("shopping_cart_shipping.shopping_cart_shipping_id") ~
    SqlParser.get[Long]("shopping_cart_shipping.store_user_id") ~
    SqlParser.get[Long]("shopping_cart_shipping.site_id") ~
    SqlParser.get[java.util.Date]("shopping_cart_shipping.shipping_date") map {
      case id~userId~siteId~shippingDate =>
        ShoppingCartShipping(id, userId, siteId, shippingDate.getTime)
    }
  }

  // Not atomic.
  def updateOrInsert(userId: Long, siteId: Long, shippingDate: Long)(implicit conn: Connection) {
    val updateCount = SQL(
      """
      update shopping_cart_shipping
      set shipping_date = {shippingDate}
      where store_user_id = {userId} and site_id = {siteId}
      """
    ).on(
      'shippingDate -> new java.sql.Timestamp(shippingDate),
      'userId -> userId,
      'siteId -> siteId
    ).executeUpdate()

    if (updateCount == 0) {
      SQL(
        """
        insert into shopping_cart_shipping (
          shopping_cart_shipping_id, store_user_id, site_id, shipping_date
        ) values (
          (select nextval('shopping_cart_shipping_seq')),
          {userId}, {siteId}, {shippingDate}
        )
        """
      ).on(
        'shippingDate -> new java.sql.Timestamp(shippingDate),
        'userId -> userId,
        'siteId -> siteId
      ).executeUpdate()
    }
  }

  def find(userId: Long, siteId: Long)(implicit conn: Connection): Long =
    SQL(
      """
      select shipping_date from shopping_cart_shipping
      where store_user_id = {userId} and site_id = {siteId}
      """
    ).on(
      'userId -> userId,
      'siteId -> siteId
    ).as(
      SqlParser.scalar[java.util.Date].single
    ).getTime

  def clear(userId: Long)(implicit conn: Connection): Unit =
    SQL(
      """
      delete from shopping_cart_shipping
      where store_user_id = {userId}
      """
    ).on(
      'userId -> userId
    ).executeUpdate()
}
