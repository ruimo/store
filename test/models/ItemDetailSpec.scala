package models

import org.specs2.mutable._

import anorm._
import anorm.SqlParser
import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current
import java.util.Locale
import java.sql.Date.{valueOf => date}
import com.ruimo.scoins.Scoping._

class ItemDetailSpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  "ItemDetail" should {
    "Get simple case." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => {
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val cat1 = Category.createNew(
            Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
          )
          val item1 = Item.createNew(cat1)
          val names = ItemName.createNew(item1, Map(LocaleInfo.Ja -> "杉", LocaleInfo.En -> "Cedar"))
          val desc1 = ItemDescription.createNew(item1, site1, "杉説明")
          val price1 = ItemPrice.createNew(item1, site1)

          val tax = Tax.createNew

          ItemPriceHistory.createNew(
            price1, tax, CurrencyInfo.Jpy, BigDecimal(100), None, BigDecimal(90), date("2013-01-02")
          )
          ItemPriceHistory.createNew(
            price1, tax, CurrencyInfo.Jpy, BigDecimal(200), None, BigDecimal(190), date("9999-12-31")
          )

          val detail = ItemDetail.show(
            site1.id.get, item1.id.get.id, LocaleInfo.Ja, date("2013-01-01"),
            UnitPriceStrategy
          ).get
          detail.name === "杉"
          detail.description === "杉説明"
          detail.itemNumericMetadata.isEmpty === true
          detail.siteItemNumericMetadata.isEmpty == true
          detail.price === BigDecimal(100)
          detail.siteName === "商店1"

          val detail2 = ItemDetail.show(
            site1.id.get, item1.id.get.id, LocaleInfo.Ja, date("2013-01-02"),
            UnitPriceStrategy
          ).get
          detail2.name === "杉"
          detail2.description === "杉説明"
          detail2.itemNumericMetadata.isEmpty === true
          detail2.siteItemNumericMetadata.isEmpty == true
          detail2.price === BigDecimal(200)
          detail2.siteName === "商店1"

          ItemNumericMetadata.createNew(item1, ItemNumericMetadataType.HEIGHT, 100)
          SiteItemNumericMetadata.createNew(site1.id.get, item1.id.get, SiteItemNumericMetadataType.STOCK, 123L)          
          val detail3 = ItemDetail.show(
            site1.id.get, item1.id.get.id, LocaleInfo.Ja, date("2013-01-02"),
            UnitPriceStrategy
          ).get
          detail3.name === "杉"
          detail3.description === "杉説明"
          detail3.itemNumericMetadata.size === 1
          detail3.itemNumericMetadata(ItemNumericMetadataType.HEIGHT).metadata === 100L
          detail3.siteItemNumericMetadata.size == 1
          detail3.siteItemNumericMetadata(SiteItemNumericMetadataType.STOCK).metadata === 123L
          detail3.price === BigDecimal(200)
          detail3.siteName === "商店1"
        }}
      }
    }

    "More than one store can sell the same item." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()
        DB.withConnection { implicit conn =>
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.Ja, "商店2")
          val cat1 = Category.createNew(
            Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
          )
          val item1 = Item.createNew(cat1)
          val names = ItemName.createNew(item1, Map(LocaleInfo.Ja -> "杉", LocaleInfo.En -> "Cedar"))
          val desc1 = ItemDescription.createNew(item1, site1, "杉説明1")
          val desc2 = ItemDescription.createNew(item1, site2, "杉説明2")
          val price1 = ItemPrice.createNew(item1, site1)
          val price2 = ItemPrice.createNew(item1, site2)

          val tax = Tax.createNew
          ItemPriceHistory.createNew(
            price1, tax, CurrencyInfo.Jpy, BigDecimal(200), None, BigDecimal(190), date("9999-12-31")
          )
          ItemPriceHistory.createNew(
            price2, tax, CurrencyInfo.Jpy, BigDecimal(300), None, BigDecimal(290), date("9999-12-31")
          )

          doWith(
            ItemDetail.show(
              site1.id.get, item1.id.get.id, LocaleInfo.Ja, date("2013-01-01"),
              UnitPriceStrategy
            ).get
          ) { detail =>
            detail.name === "杉"
            detail.description === "杉説明1"
            detail.itemNumericMetadata.isEmpty === true
            detail.siteItemNumericMetadata.isEmpty == true
            detail.price === BigDecimal(200)
            detail.siteName === "商店1"
          }

          doWith(
            ItemDetail.show(
              site2.id.get, item1.id.get.id, LocaleInfo.Ja, date("2013-01-01"),
              UnitPriceStrategy
            ).get
          ) { detail =>
            detail.name === "杉"
            detail.description === "杉説明2"
            detail.itemNumericMetadata.isEmpty === true
            detail.siteItemNumericMetadata.isEmpty == true
            detail.price === BigDecimal(300)
            detail.siteName === "商店2"
          }
        }
      }
    }
  }
}
