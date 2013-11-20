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

case class ItemDetail(
  siteId: Long,
  itemId: Long,
  name: String,
  description: String,
  itemNumericMetadata: Map[ItemNumericMetadataType, ItemNumericMetadata],
  itemTextMetadata: Map[ItemTextMetadataType, ItemTextMetadata],
  siteItemNumericMetadata: Map[SiteItemNumericMetadataType, SiteItemNumericMetadata],
  price: BigDecimal,
  siteName: String
) extends NotNull

object ItemDetail {
  val nameDesc = {
    SqlParser.get[String]("item_name.item_name") ~
    SqlParser.get[String]("item_description.description") map {
      case name~description => (name, description)
    }
  }

  def show(
    siteId: Long, itemId: Long, locale: LocaleInfo, now: Long = System.currentTimeMillis
  )(implicit conn: Connection): ItemDetail = {
    val (name, description) = SQL(
      """
      select * from item
      inner join item_name
        on item_name.item_id = {itemId}
        and item_name.locale_id = {localeId}
      inner join item_description
        on item_description.item_id = {itemId}
        and item_description.locale_id = {localeId}
      where item.item_id = {itemId}
      """
    ).on(
      'itemId -> itemId,
      'localeId -> locale.id
    ).as(
      nameDesc.single
    )

    val priceHistory = ItemPriceHistory.atBySiteAndItem(siteId, itemId, now)

    ItemDetail(
      siteId, itemId,
      name, description,
      ItemNumericMetadata.allById(itemId),
      ItemTextMetadata.allById(itemId),
      SiteItemNumericMetadata.all(siteId, itemId),
      priceHistory.unitPrice,
      Site(siteId).name
    )
  }
}
