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
import helpers.QueryString

class ItemSpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  "Item" should {
    "List item when empty." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => {
          Item.listBySiteId(siteId = 1, locale = LocaleInfo.Ja, queryString = "foo") === List()
        }}
      }
    }

    "Item name." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()
        
        DB.withConnection { implicit conn => {
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
        }}
      }
    }

    "item price." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => {
          val cat1 = Category.createNew(
            Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
          )
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val item1 = Item.createNew(cat1)
          
          ItemPrice.get(site1, item1) === None

          val price1 = ItemPrice.createNew(item1, site1)
          val saved1 = ItemPrice.get(site1, item1).get
          saved1 === price1
        }}
      }
    }

    "Can get item price history." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => {
          val cat1 = Category.createNew(
            Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
          )
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val item1 = Item.createNew(cat1)
          val price1 = ItemPrice.createNew(item1, site1)
          val tax = Tax.createNew
      
          import java.sql.Date.{valueOf => date}
          implicit def date2milli(d: java.sql.Date) = d.getTime

          ItemPriceHistory.createNew(
            price1, tax, CurrencyInfo.Jpy, BigDecimal(100), BigDecimal(90), date("2013-01-02")
          )
          ItemPriceHistory.createNew(
            price1, tax, CurrencyInfo.Jpy, BigDecimal(200), BigDecimal(190), date("9999-12-31")
          )

          ItemPriceHistory.at(price1.id.get, date("2013-01-01")).unitPrice === BigDecimal(100)
          ItemPriceHistory.at(price1.id.get, date("2013-01-02")).unitPrice === BigDecimal(200)
          ItemPriceHistory.at(price1.id.get, date("2013-01-03")).unitPrice === BigDecimal(200)
        }}
      }
    }

    "Can get metadata" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => {
          val cat1 = Category.createNew(
            Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
          )
          val item1 = Item.createNew(cat1)
          val item2 = Item.createNew(cat1)

          ItemNumericMetadata.createNew(item1, ItemNumericMetadataType.HEIGHT, 100)

          ItemNumericMetadata.createNew(item2, ItemNumericMetadataType.HEIGHT, 1000)

          ItemNumericMetadata(item1, ItemNumericMetadataType.HEIGHT).metadata === 100

          ItemNumericMetadata(item2, ItemNumericMetadataType.HEIGHT).metadata === 1000
        }}
      }
    }

    "Can get all metadata at once" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => {
          val cat1 = Category.createNew(
            Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
          )
          val item1 = Item.createNew(cat1)
          val item2 = Item.createNew(cat1)

          ItemNumericMetadata.createNew(item1, ItemNumericMetadataType.HEIGHT, 100)

          ItemNumericMetadata.createNew(item2, ItemNumericMetadataType.HEIGHT, 1000)

          val map1 = ItemNumericMetadata.all(item1)
          map1.size === 1
          map1(ItemNumericMetadataType.HEIGHT).metadata === 100

          val map2 = ItemNumericMetadata.all(item2)
          map2.size === 1
          map2(ItemNumericMetadataType.HEIGHT).metadata === 1000
        }}
      }
    }

    def storeItems(tax: Tax, site1: Site, site2: Site) {
      DB.withConnection { implicit conn => {
        import LocaleInfo._

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

        ItemName.createNew(item1, Map(Ja -> "杉", En -> "Cedar"))
        ItemName.createNew(item2, Map(Ja -> "梅", En -> "Ume"))
        ItemName.createNew(item3, Map(Ja -> "桜", En -> "Cherry"))
        ItemName.createNew(item4, Map(Ja -> "桃", En -> "Peach"))
        ItemName.createNew(item5, Map(Ja -> "もみじ", En -> "Maple"))

        SiteItem.createNew(site1, item1)
        SiteItem.createNew(site1, item3)
        SiteItem.createNew(site1, item5)

        SiteItem.createNew(site2, item2)
        SiteItem.createNew(site2, item4)

        ItemDescription.createNew(item1, site1, "杉説明")
        ItemDescription.createNew(item2, site2, "梅説明")
        ItemDescription.createNew(item3, site1, "桜説明")
        ItemDescription.createNew(item4, site2, "桃説明")
        ItemDescription.createNew(item5, site1, "もみじ説明")

        val price1 = ItemPrice.createNew(item1, site1)
        val price2 = ItemPrice.createNew(item2, site2)
        val price3 = ItemPrice.createNew(item3, site1)
        val price4 = ItemPrice.createNew(item4, site2)
        val price5 = ItemPrice.createNew(item5, site1)

        ItemPriceHistory.createNew(
          price1, tax, CurrencyInfo.Jpy, BigDecimal(100), BigDecimal(90), date("2013-01-02")
        )
        ItemPriceHistory.createNew(
          price1, tax, CurrencyInfo.Jpy, BigDecimal(101), BigDecimal(89), date("9999-12-31")
        )

        ItemPriceHistory.createNew(
          price2, tax, CurrencyInfo.Jpy, BigDecimal(300), BigDecimal(290), date("2013-01-03")
        )
        ItemPriceHistory.createNew(
          price2, tax, CurrencyInfo.Jpy, BigDecimal(301), BigDecimal(291), date("9999-12-31")
        )

        ItemPriceHistory.createNew(
          price3, tax, CurrencyInfo.Jpy, BigDecimal(500), BigDecimal(480), date("2013-01-04")
        )
        ItemPriceHistory.createNew(
          price3, tax, CurrencyInfo.Jpy, BigDecimal(501), BigDecimal(481), date("9999-12-31")
        )

        ItemPriceHistory.createNew(
          price4, tax, CurrencyInfo.Jpy, BigDecimal(1200), BigDecimal(1100), date("2013-01-05")
        )
        ItemPriceHistory.createNew(
          price4, tax, CurrencyInfo.Jpy, BigDecimal(1201), BigDecimal(1101), date("9999-12-31")
        )

        ItemPriceHistory.createNew(
          price5, tax, CurrencyInfo.Jpy, BigDecimal(2000), BigDecimal(1900), date("2013-01-06")
        )
        ItemPriceHistory.createNew(
          price5, tax, CurrencyInfo.Jpy, BigDecimal(2001), BigDecimal(1901), date("9999-12-31")
        )

        val height1 = ItemNumericMetadata.createNew(item1, ItemNumericMetadataType.HEIGHT, 100)
        val height2 = ItemNumericMetadata.createNew(item2, ItemNumericMetadataType.HEIGHT, 200)
        val height3 = ItemNumericMetadata.createNew(item3, ItemNumericMetadataType.HEIGHT, 300)
        val height4 = ItemNumericMetadata.createNew(item4, ItemNumericMetadataType.HEIGHT, 400)
        val height5 = ItemNumericMetadata.createNew(item5, ItemNumericMetadataType.HEIGHT, 500)
      }}
    }

    "List item by site." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          TestHelper.removePreloadedRecords()

          val tax = Tax.createNew

          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.Ja, "商店2")
        
          storeItems(tax, site1, site2)

          val time = date("2013-01-04").getTime
          val list1 = Item.listBySite(site1, LocaleInfo.Ja, "", now = time)
          list1.size === 3

          list1(0)._2.name === "もみじ"
          list1(1)._2.name === "杉"
          list1(2)._2.name === "桜"

          list1(0)._3.description === "もみじ説明"
          list1(1)._3.description === "杉説明"
          list1(2)._3.description === "桜説明"

          list1(0)._5.taxId === tax.id.get
          list1(0)._5.currency === CurrencyInfo.Jpy
          list1(0)._5.unitPrice === BigDecimal(2000)

          list1(1)._5.taxId === tax.id.get
          list1(1)._5.currency === CurrencyInfo.Jpy
          list1(1)._5.unitPrice === BigDecimal(101)

          list1(2)._5.taxId === tax.id.get
          list1(2)._5.currency === CurrencyInfo.Jpy
          list1(2)._5.unitPrice === BigDecimal(501)

          list1(0)._6(ItemNumericMetadataType.HEIGHT).metadata === 500
          list1(1)._6(ItemNumericMetadataType.HEIGHT).metadata === 100
          list1(2)._6(ItemNumericMetadataType.HEIGHT).metadata === 300
        }}
      }
    }

    "List item." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          import LocaleInfo._
          TestHelper.removePreloadedRecords()

          val tax = Tax.createNew

          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.Ja, "商店2")
        
          storeItems(tax, site1, site2)

          val time = date("2013-01-04").getTime

          val pages = Item.list(None, LocaleInfo.Ja, QueryString(), now = time)
          pages.pageCount === 1
          pages.currentPage === 0
          pages.pageSize === 10
          val list1 = pages.records
          list1.size === 5

          list1(0)._2.name === "もみじ"
          list1(1)._2.name === "杉"
          list1(2)._2.name === "桃"
          list1(3)._2.name === "桜"
          list1(4)._2.name === "梅"

          list1(0)._3.description === "もみじ説明"
          list1(1)._3.description === "杉説明"
          list1(2)._3.description === "桃説明"
          list1(3)._3.description === "桜説明"
          list1(4)._3.description === "梅説明"

          list1(0)._5.taxId === tax.id.get
          list1(0)._5.currency === CurrencyInfo.Jpy
          list1(0)._5.unitPrice === BigDecimal(2000)

          list1(1)._5.taxId === tax.id.get
          list1(1)._5.currency === CurrencyInfo.Jpy
          list1(1)._5.unitPrice === BigDecimal(101)

          list1(2)._5.taxId === tax.id.get
          list1(2)._5.currency === CurrencyInfo.Jpy
          list1(2)._5.unitPrice === BigDecimal(1200)

          list1(3)._5.taxId === tax.id.get
          list1(3)._5.currency === CurrencyInfo.Jpy
          list1(3)._5.unitPrice === BigDecimal(501)

          list1(4)._5.taxId === tax.id.get
          list1(4)._5.currency === CurrencyInfo.Jpy
          list1(4)._5.unitPrice === BigDecimal(301)

          list1(0)._6(ItemNumericMetadataType.HEIGHT).metadata === 500
          list1(1)._6(ItemNumericMetadataType.HEIGHT).metadata === 100
          list1(2)._6(ItemNumericMetadataType.HEIGHT).metadata === 400
          list1(3)._6(ItemNumericMetadataType.HEIGHT).metadata === 300
          list1(4)._6(ItemNumericMetadataType.HEIGHT).metadata === 200
        }}
      }
    }

    "Can create sql for item query." in {
      Item.createQueryConditionSql(QueryString(List("Hello", "World"))) ===
        "and (item_name.item_name like {query0} or item_description.description like {query0}) " +
        "and (item_name.item_name like {query1} or item_description.description like {query1}) "
    }
  }
}
