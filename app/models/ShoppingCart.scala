package models

import anorm._
import anorm.{NotAssigned, Pk}
import anorm.SqlParser
import model.Until
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import collection.immutable.{HashMap, IntMap}
import java.sql.Connection
import play.api.data.Form
import org.joda.time.DateTime

case class ShoppingCartItem(
  id: Pk[Long] = NotAssigned, storeUserId: Long, sequenceNumber: Int,
  siteId: Long, itemId: Long, quantity: Int
) extends NotNull

object ShoppingCart {
  val simple = {
    SqlParser.get[Pk[Long]]("shopping_cart.shopping_cart_id") ~
    SqlParser.get[Long]("shopping_cart.store_user_id") ~
    SqlParser.get[Int]("shopping_cart.seq") ~
    SqlParser.get[Long]("shopping_cart.site_id") ~
    SqlParser.get[Long]("shopping_cart.item_id") ~
    SqlParser.get[Int]("shopping_cart.quantity") map {
      case id~userId~seq~siteId~itemId~quantity =>
        ShoppingCartItem(id, userId, seq, siteId, itemId, quantity)
    }
  }

  def addItem(userId: Long, siteId: Long, itemId: Long, quantity: Int)(implicit conn: Connection): ShoppingCartItem = {
    SQL(
      """
      insert into shopping_cart (shopping_cart_id, store_user_id, seq, site_id, item_id, quantity)
      values (
        (select nextval('shopping_cart_seq')),
        {userId},
        (select coalesce(max(seq), 0) + 1 from shopping_cart where store_user_id = {userId}),
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
    
    val id = SQL("select currval('shopping_cart_seq')").as(SqlParser.scalar[Long].single)
    val seq = SQL(
      "select seq from shopping_cart where shopping_cart_id = {id}"
    ).on('id -> id).as(SqlParser.scalar[Int].single)

    ShoppingCartItem(Id(id), userId, seq, siteId, itemId, quantity)
  }

  def remove(id: Long, userId: Long)(implicit conn: Connection): Int =
    SQL(
      """
      delete from shopping_cart
      where shopping_cart_id = {id} and store_user_id = {userId}
      """
    ).on(
      'id -> id,
      'userId -> userId
    ).executeUpdate()

  def removeForUser(userId: Long)(implicit conn: Connection) {
    SQL(
      "delete from shopping_cart where store_user_id = {id}"
    ).on(
      'id -> userId
    ).executeUpdate()
  }

  val listParser = ShoppingCart.simple~ItemName.simple~ItemDescription.simple~ItemPrice.simple~Site.simple map {
    case cart~itemName~itemDescription~itemPrice~site => (
      cart, itemName, itemDescription, itemPrice, site
    )
  }

  def listItemsForUser(
    locale: LocaleInfo, userId: Long, page: Int = 0, pageSize: Int = 10, now: Long = System.currentTimeMillis
  )(
    implicit conn: Connection
  ): Seq[(
    ShoppingCartItem, ItemName, ItemDescription, Site, ItemPriceHistory,
    Map[ItemNumericMetadataType, ItemNumericMetadata],
    Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]
  )] =
    SQL(
      """
      select * from shopping_cart
      inner join item_name on shopping_cart.item_id = item_name.item_id
      inner join item_description on shopping_cart.item_id = item_description.item_id
      inner join item_price on shopping_cart.item_id = item_price.item_id 
      inner join site_item on shopping_cart.item_id = site_item.item_id and item_price.site_id = site_item.site_id
      inner join site on site_item.site_id = site.site_id and shopping_cart.site_id = site.site_id
      where item_name.locale_id = {localeId}
      and item_description.locale_id = {localeId}
      and shopping_cart.store_user_id = {userId}
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
      val metadata = ItemNumericMetadata.allById(itemId)
      val siteMetadata = SiteItemNumericMetadata.all(e._5.id.get, itemId)

      (e._1, e._2, e._3, e._5, priceHistory, metadata, siteMetadata)
    }

  def changeQuantity(id: Long, userId: Long, quantity: Int)(implicit conn: Connection): Int = {
    SQL(
      """
      update shopping_cart set quantity = {quantity}
      where shopping_cart_id = {id} and store_user_id = {userId}
      """
    ).on(
      'quantity -> quantity,
      'id ->id,
      'userId -> userId
    ).executeUpdate()
  }

  def apply(id: Long)(implicit conn: Connection): ShoppingCartItem =
    SQL(
      "select * from shopping_cart where shopping_cart_id = {id}"
    ).on(
      'id -> id
    ).as(simple.single)
}
