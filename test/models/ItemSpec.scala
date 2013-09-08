package models

import org.specs2.mutable._

import anorm._
import anorm.{NotAssigned, Pk}
import anorm.SqlParser
import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current
import anorm.Id
import java.util.Locale

class ItemSpec extends Specification {
  "Item" should {
    "List item when empty." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        Item.listBySiteId(siteId = 1, locale = LocaleInfo.Ja, queryString = "foo") === List()
      }
    }

    "Item name." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()
        
        val cat1 = Category.createNew(
          Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
        )
        val item1 = Item.createNew(cat1)
        val names = ItemName.createNew(item1, Map(LocaleInfo.Ja -> "杉", LocaleInfo.En -> "Cedar"))

        names.size === 2
        names(LocaleInfo.Ja) === ItemName(LocaleInfo.Ja.id, item1.id.get, "杉")
        names(LocaleInfo.En) === ItemName(LocaleInfo.En.id, item1.id.get, "Cedar")

        val map = ItemName.list(item1)
        map.size === 2
        map(LocaleInfo.Ja) === ItemName(LocaleInfo.Ja.id, item1.id.get, "杉")
        map(LocaleInfo.En) === ItemName(LocaleInfo.En.id, item1.id.get, "Cedar")
      }
    }

    "item price." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        val cat1 = Category.createNew(
          Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
        )
        val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
        val item1 = Item.createNew(cat1)

        ItemPrice.get(site1, item1) === None

        val price1 = ItemPrice.createNew(site1, item1)
        val saved1 = ItemPrice.get(site1, item1).get
        saved1 === price1
      }
    }

    "Can get item price history." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        val cat1 = Category.createNew(
          Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
        )
        val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
        val item1 = Item.createNew(cat1)
        val price1 = ItemPrice.createNew(site1, item1)
        val tax = Tax.createNew()
      
        import java.sql.Date.{valueOf => date}
        implicit def date2milli(d: java.sql.Date) = d.getTime

        ItemPriceHistory.createNew(
          price1, tax, CurrencyInfo.Jpy, BigDecimal(100), date("2013-01-02")
        )
        ItemPriceHistory.createNew(
          price1, tax, CurrencyInfo.Jpy, BigDecimal(200), date("9999-12-31")
        )

        ItemPriceHistory.at(price1.id.get, date("2013-01-01")).unitPrice === BigDecimal(100)
        ItemPriceHistory.at(price1.id.get, date("2013-01-02")).unitPrice === BigDecimal(200)
        ItemPriceHistory.at(price1.id.get, date("2013-01-03")).unitPrice === BigDecimal(200)
      }
    }

    "Can get metadata" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        val cat1 = Category.createNew(
          Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
        )
        val item1 = Item.createNew(cat1)
        val item2 = Item.createNew(cat1)

        ItemNumericMetadata.createNew(item1, MetadataType.HEIGHT, 100)
        ItemNumericMetadata.createNew(item1, MetadataType.STOCK, 200)
        
        ItemNumericMetadata.createNew(item2, MetadataType.HEIGHT, 1000)
        ItemNumericMetadata.createNew(item2, MetadataType.STOCK, 2000)

        ItemNumericMetadata(item1, MetadataType.HEIGHT).metadata === 100
        ItemNumericMetadata(item1, MetadataType.STOCK).metadata === 200

        ItemNumericMetadata(item2, MetadataType.HEIGHT).metadata === 1000
        ItemNumericMetadata(item2, MetadataType.STOCK).metadata === 2000
      }
    }

    "List item." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        import LocaleInfo._

        TestHelper.removePreloadedRecords()
        val cat1 = Category.createNew(
          Map(Ja -> "植木", En -> "Plant")
        )
        val cat2 = Category.createNew(
          Map(Ja -> "果樹", En -> "Fruit")
        )

        val item1 = Item.createNew(cat1)
        val item2 = Item.createNew(cat2)
        val item3 = Item.createNew(cat1)
        val item4 = Item.createNew(cat2)
        val item5 = Item.createNew(cat1)

        val itemName1 = ItemName.createNew(item1, Map(Ja -> "杉", En -> "Cedar"))
        val itemName2 = ItemName.createNew(item2, Map(Ja -> "梅", En -> "Ume"))
        val itemName3 = ItemName.createNew(item3, Map(Ja -> "桜", En -> "Cherry"))
        val itemName4 = ItemName.createNew(item4, Map(Ja -> "桃", En -> "Peach"))
        val itemName5 = ItemName.createNew(item5, Map(Ja -> "もみじ", En -> "Maple"))

        val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
        val site2 = Site.createNew(LocaleInfo.En, "Shop2")
        
        SiteItem.createNew(site1, item1)
        SiteItem.createNew(site1, item3)
        SiteItem.createNew(site1, item5)

        SiteItem.createNew(site2, item2)
        SiteItem.createNew(site2, item4)

        val itemDesc1 = ItemDescription.createNew(item1, site1, "杉説明")
        val itemDesc2 = ItemDescription.createNew(item2, site2, "Ume description")
        val itemDesc3 = ItemDescription.createNew(item3, site1, "桜説明")
        val itemDesc4 = ItemDescription.createNew(item4, site2, "Cherry description")
        val itemDesc5 = ItemDescription.createNew(item5, site1, "もみじ説明")

        val price1 = ItemPrice.createNew(site1, item1)
        val price2 = ItemPrice.createNew(site2, item2)
        val price3 = ItemPrice.createNew(site1, item3)
        val price4 = ItemPrice.createNew(site2, item4)
        val price5 = ItemPrice.createNew(site1, item5)

        val tax = Tax.createNew()

        import java.sql.Date.{valueOf => date}
        implicit def date2milli(d: java.sql.Date) = d.getTime

        val priceHistory1 = ItemPriceHistory.createNew(
          price1, tax, CurrencyInfo.Jpy, BigDecimal(100), date("2013-01-02")
        )
        val priceHistory2 = ItemPriceHistory.createNew(
          price2, tax, CurrencyInfo.Jpy, BigDecimal(300), date("2013-01-03")
        )
        val priceHistory3 = ItemPriceHistory.createNew(
          price3, tax, CurrencyInfo.Jpy, BigDecimal(500), date("2013-01-04")
        )
        val priceHistory4 = ItemPriceHistory.createNew(
          price4, tax, CurrencyInfo.Jpy, BigDecimal(1200), date("2013-01-05")
        )
        val priceHistory5 = ItemPriceHistory.createNew(
          price5, tax, CurrencyInfo.Jpy, BigDecimal(2000), date("2013-01-06")
        )

        val height1 = ItemNumericMetadata.createNew(item1, MetadataType.HEIGHT, 100)
        val height2 = ItemNumericMetadata.createNew(item2, MetadataType.HEIGHT, 200)
        val height3 = ItemNumericMetadata.createNew(item3, MetadataType.HEIGHT, 300)
        val height4 = ItemNumericMetadata.createNew(item4, MetadataType.HEIGHT, 400)
        val height5 = ItemNumericMetadata.createNew(item5, MetadataType.HEIGHT, 500)

        val list1 = Item.list(site1, LocaleInfo.Ja, "")
      }
    }
  }
}
