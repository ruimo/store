package models

import anorm._
import anorm.SqlParser
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import collection.immutable.{HashMap, IntMap}
import java.sql.Connection
import org.joda.time.DateTime

case class ItemDetail(
  siteId: Long,
  itemId: Long,
  name: String,
  description: String,
  itemNumericMetadata: Map[ItemNumericMetadataType, ItemNumericMetadata],
  itemTextMetadata: Map[ItemTextMetadataType, ItemTextMetadata],
  siteItemNumericMetadata: Map[SiteItemNumericMetadataType, SiteItemNumericMetadata],
  siteItemTextMetadata: Map[SiteItemTextMetadataType, SiteItemTextMetadata],
  price: BigDecimal,
  listPrice: Option[BigDecimal],
  siteName: String
)

object ItemDetail {
  val nameDesc = {
    SqlParser.get[String]("item_name.item_name") ~
    SqlParser.get[String]("item_description.description") map {
      case name~description => (name, description)
    }
  }

  def show(
    siteId: Long, itemId: Long, locale: LocaleInfo, now: Long = System.currentTimeMillis,
    itemPriceStrategy: ItemPriceStrategy
  )(implicit conn: Connection): Option[ItemDetail] = SQL(
      """
      select * from item
      inner join item_name
        on item_name.item_id = {itemId}
        and item_name.locale_id = {localeId}
      inner join item_description
        on item_description.item_id = {itemId}
        and site_id = {siteId}
        and item_description.locale_id = {localeId}
      where item.item_id = {itemId}
      """
    ).on(
      'itemId -> itemId,
      'siteId -> siteId,
      'localeId -> locale.id
    ).as(
      nameDesc.singleOpt
    ).map { t =>
      val priceHistory = ItemPriceHistory.atBySiteAndItem(siteId, ItemId(itemId), now)

      ItemDetail(
        siteId, itemId,
        t._1, t._2,
        ItemNumericMetadata.allById(ItemId(itemId)),
        ItemTextMetadata.allById(ItemId(itemId)),
        SiteItemNumericMetadata.all(siteId, ItemId(itemId)),
        SiteItemTextMetadata.all(siteId, ItemId(itemId)),
        itemPriceStrategy.price(ItemPriceStrategyInput(priceHistory)),
        priceHistory.listPrice,
        Site(siteId).name
      )
    }
}
