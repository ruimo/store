package models

import org.specs2.mutable._

import anorm._
import anorm.SqlParser
import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current
import java.util.Locale
import com.ruimo.scoins.Scoping._

import java.sql.Date.{valueOf => date}
import helpers.QueryString
import helpers.CategorySearchCondition
import com.ruimo.scoins.Scoping._

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
            price1, tax, CurrencyInfo.Jpy, BigDecimal(100), None, BigDecimal(90), date("2013-01-02")
          )
          ItemPriceHistory.createNew(
            price1, tax, CurrencyInfo.Jpy, BigDecimal(200), None, BigDecimal(190), date("9999-12-31")
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

    "Can create site item text metadata" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => {
          val cat1 = Category.createNew(
            Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
          )
          val item1 = Item.createNew(cat1)
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.Ja, "商店2")

          SiteItemTextMetadata.createNew(site1.id.get, item1.id.get, SiteItemTextMetadataType.PRICE_MEMO, "MEMO01")
          SiteItemTextMetadata.createNew(site2.id.get, item1.id.get, SiteItemTextMetadataType.PRICE_MEMO, "MEMO02")

          SiteItemTextMetadata(site1.id.get, item1.id.get, SiteItemTextMetadataType.PRICE_MEMO).metadata === "MEMO01"
          SiteItemTextMetadata(site2.id.get, item1.id.get, SiteItemTextMetadataType.PRICE_MEMO).metadata === "MEMO02"
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

    case class CreatedRecords(
      category1: Category, category2: Category
    )

    def storeItems(tax: Tax, site1: Site, site2: Site): CreatedRecords = {
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
          price1, tax, CurrencyInfo.Jpy, BigDecimal(100), None, BigDecimal(90), date("2013-01-02")
        )
        ItemPriceHistory.createNew(
          price1, tax, CurrencyInfo.Jpy, BigDecimal(101), None, BigDecimal(89), date("9999-12-31")
        )

        ItemPriceHistory.createNew(
          price2, tax, CurrencyInfo.Jpy, BigDecimal(300), None, BigDecimal(290), date("2013-01-03")
        )
        ItemPriceHistory.createNew(
          price2, tax, CurrencyInfo.Jpy, BigDecimal(301), None, BigDecimal(291), date("9999-12-31")
        )

        ItemPriceHistory.createNew(
          price3, tax, CurrencyInfo.Jpy, BigDecimal(500), None, BigDecimal(480), date("2013-01-04")
        )
        ItemPriceHistory.createNew(
          price3, tax, CurrencyInfo.Jpy, BigDecimal(501), None, BigDecimal(481), date("9999-12-31")
        )

        ItemPriceHistory.createNew(
          price4, tax, CurrencyInfo.Jpy, BigDecimal(1200), None, BigDecimal(1100), date("2013-01-05")
        )
        ItemPriceHistory.createNew(
          price4, tax, CurrencyInfo.Jpy, BigDecimal(1201), None, BigDecimal(1101), date("9999-12-31")
        )

        ItemPriceHistory.createNew(
          price5, tax, CurrencyInfo.Jpy, BigDecimal(2000), None, BigDecimal(1900), date("2013-01-06")
        )
        ItemPriceHistory.createNew(
          price5, tax, CurrencyInfo.Jpy, BigDecimal(2001), None, BigDecimal(1901), date("9999-12-31")
        )

        val height1 = ItemNumericMetadata.createNew(item1, ItemNumericMetadataType.HEIGHT, 100)
        val height2 = ItemNumericMetadata.createNew(item2, ItemNumericMetadataType.HEIGHT, 200)
        val height3 = ItemNumericMetadata.createNew(item3, ItemNumericMetadataType.HEIGHT, 300)
        val height4 = ItemNumericMetadata.createNew(item4, ItemNumericMetadataType.HEIGHT, 400)
        val height5 = ItemNumericMetadata.createNew(item5, ItemNumericMetadataType.HEIGHT, 500)

        CreatedRecords(cat1, cat2)
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

    "List item by category." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          import LocaleInfo._
          TestHelper.removePreloadedRecords()

          val tax = Tax.createNew

          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val cat1 = Category.createNew(Map(Ja -> "植木", En -> "Plant"))
          val cat2 = Category.createNew(parent = Some(cat1), names = Map(Ja -> "果樹", En -> "Fruit"))
        
          val item1 = Item.createNew(cat1)
          val item2 = Item.createNew(cat2)

          ItemName.createNew(item1, Map(Ja -> "杉", En -> "Cedar"))
          ItemName.createNew(item2, Map(Ja -> "梅", En -> "Ume"))

          SiteItem.createNew(site1, item1)
          SiteItem.createNew(site1, item2)

          ItemDescription.createNew(item1, site1, "杉説明")
          ItemDescription.createNew(item2, site1, "梅説明")

          val price1 = ItemPrice.createNew(item1, site1)
          val price2 = ItemPrice.createNew(item2, site1)

          ItemPriceHistory.createNew(
            price1, tax, CurrencyInfo.Jpy, BigDecimal(101), None, BigDecimal(89), date("9999-12-31")
          )
          ItemPriceHistory.createNew(
            price2, tax, CurrencyInfo.Jpy, BigDecimal(301), None, BigDecimal(291), date("9999-12-31")
          )

          // Since cat2 is a child of cat1, both item1 and item2 will be shown.
          val list1 = Item.list(
            locale = LocaleInfo.Ja, queryString = QueryString(), category = CategorySearchCondition(cat1.id.get)
          )
          doWith(list1.records) { recs =>
            recs.size === 2

            recs(0)._2.name === "杉"
            recs(1)._2.name === "梅"
          }
        }}
      }
    }

    "List item by category with supplemental category." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          import LocaleInfo._
          TestHelper.removePreloadedRecords()

          val tax = Tax.createNew

          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val cat1 = Category.createNew(Map(Ja -> "植木", En -> "Plant"))
          val cat2 = Category.createNew(Map(Ja -> "果樹", En -> "Fruit"))
        
          val item1 = Item.createNew(cat1)
          val item2 = Item.createNew(cat2)
          SupplementalCategory.createNew(item1.id.get, cat2.id.get)

          ItemName.createNew(item1, Map(Ja -> "杉", En -> "Cedar"))
          ItemName.createNew(item2, Map(Ja -> "梅", En -> "Ume"))

          SiteItem.createNew(site1, item1)
          SiteItem.createNew(site1, item2)

          ItemDescription.createNew(item1, site1, "杉説明")
          ItemDescription.createNew(item2, site1, "梅説明")

          val price1 = ItemPrice.createNew(item1, site1)
          val price2 = ItemPrice.createNew(item2, site1)

          ItemPriceHistory.createNew(
            price1, tax, CurrencyInfo.Jpy, BigDecimal(101), None, BigDecimal(89), date("9999-12-31")
          )
          ItemPriceHistory.createNew(
            price2, tax, CurrencyInfo.Jpy, BigDecimal(301), None, BigDecimal(291), date("9999-12-31")
          )

          // Since item1 has supplemental category(cat2), both item1 and item2 will be shown.
          val list1 = Item.list(
            locale = LocaleInfo.Ja, queryString = QueryString(), category = CategorySearchCondition(cat2.id.get)
          )
          doWith(list1.records) { recs =>
            recs.size === 2

            recs(0)._2.name === "杉"
            recs(1)._2.name === "梅"
          }
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
        
          val createdRecords = storeItems(tax, site1, site2)

          val time = date("2013-01-04").getTime

          doWith(Item.list(None, LocaleInfo.Ja, QueryString(), now = time)) { pages =>
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
          }

          // Specify category
          doWith(
            Item.list(None, LocaleInfo.Ja, QueryString(), CategorySearchCondition(createdRecords.category1.id.get), now = time)
          ) { pages =>
            pages.pageCount === 1
            pages.currentPage === 0
            pages.pageSize === 10
            val list1 = pages.records
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
          }

          // Specify site
          doWith(
            Item.list(None, LocaleInfo.Ja, QueryString(), CategorySearchCondition.Null, Some(site1.id.get), now = time)
          ) { pages =>
            pages.pageCount === 1
            pages.currentPage === 0
            pages.pageSize === 10
            val list1 = pages.records
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
          }
        }}
      }
    }

    "List item for maintenance." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          import LocaleInfo._
          TestHelper.removePreloadedRecords()

          val tax = Tax.createNew

          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.Ja, "商店2")
        
          storeItems(tax, site1, site2)

          val time = date("2013-01-04").getTime

          val pages = Item.listForMaintenance(
            siteUser = None,
            locale = LocaleInfo.Ja,
            queryString = QueryString(),
            now = time
          )
          pages.pageCount === 1
          pages.currentPage === 0
          pages.pageSize === 10
          val list1 = pages.records
          list1.size === 5

          list1(0)._2.get.name === "もみじ"
          list1(1)._2.get.name === "杉"
          list1(2)._2.get.name === "桃"
          list1(3)._2.get.name === "桜"
          list1(4)._2.get.name === "梅"

          list1(0)._3.get.description === "もみじ説明"
          list1(1)._3.get.description === "杉説明"
          list1(2)._3.get.description === "桃説明"
          list1(3)._3.get.description === "桜説明"
          list1(4)._3.get.description === "梅説明"

          doWith(list1(0)._5) { optPriceHistory =>
            optPriceHistory.get.taxId === tax.id.get
            optPriceHistory.get.currency === CurrencyInfo.Jpy
            optPriceHistory.get.unitPrice === BigDecimal(2000)
          }

          doWith(list1(1)._5) { optPriceHistory =>
            optPriceHistory.get.taxId === tax.id.get
            optPriceHistory.get.currency === CurrencyInfo.Jpy
            optPriceHistory.get.unitPrice === BigDecimal(101)
          }

          doWith(list1(2)._5) { optPriceHistory =>
            optPriceHistory.get.taxId === tax.id.get
            optPriceHistory.get.currency === CurrencyInfo.Jpy
            optPriceHistory.get.unitPrice === BigDecimal(1200)
          }

          doWith(list1(3)._5) { optPriceHistory =>
            optPriceHistory.get.taxId === tax.id.get
            optPriceHistory.get.currency === CurrencyInfo.Jpy
            optPriceHistory.get.unitPrice === BigDecimal(501)
          }

          doWith(list1(4)._5) { optPriceHistory =>
            optPriceHistory.get.taxId === tax.id.get
            optPriceHistory.get.currency === CurrencyInfo.Jpy
            optPriceHistory.get.unitPrice === BigDecimal(301)
          }

          list1(0)._6(ItemNumericMetadataType.HEIGHT).metadata === 500
          list1(1)._6(ItemNumericMetadataType.HEIGHT).metadata === 100
          list1(2)._6(ItemNumericMetadataType.HEIGHT).metadata === 400
          list1(3)._6(ItemNumericMetadataType.HEIGHT).metadata === 300
          list1(4)._6(ItemNumericMetadataType.HEIGHT).metadata === 200

          doWith(
            Item.listForMaintenance(
              siteUser = None,
              locale = LocaleInfo.En,
              queryString = QueryString(),
              now = time
            )
          ) { pages =>
            pages.pageCount === 1
            pages.currentPage === 0
            pages.pageSize === 10
            
            doWith(pages.records) { list =>
              list.size === 5
              
              list(0)._2.get.name === "Cedar"
              list(1)._2.get.name === "Cherry"
              list(2)._2.get.name === "Maple"
              list(3)._2.get.name === "Peach"
              list(4)._2.get.name === "Ume"

              list(0)._3 === None
              list(1)._3 === None
              list(2)._3 === None
              list(3)._3 === None
              list(4)._3 === None

              doWith(list(0)._5) { optPriceHistory =>
                optPriceHistory.get.taxId === tax.id.get
                optPriceHistory.get.currency === CurrencyInfo.Jpy
                optPriceHistory.get.unitPrice === BigDecimal(101)
              }

              doWith(list(1)._5) { optPriceHistory =>
                optPriceHistory.get.taxId === tax.id.get
                optPriceHistory.get.currency === CurrencyInfo.Jpy
                optPriceHistory.get.unitPrice === BigDecimal(501)
              }

              doWith(list(2)._5) { optPriceHistory =>
                optPriceHistory.get.taxId === tax.id.get
                optPriceHistory.get.currency === CurrencyInfo.Jpy
                optPriceHistory.get.unitPrice === BigDecimal(2000)
              }

              doWith(list(3)._5) { optPriceHistory =>
                optPriceHistory.get.taxId === tax.id.get
                optPriceHistory.get.currency === CurrencyInfo.Jpy
                optPriceHistory.get.unitPrice === BigDecimal(1200)
              }

              doWith(list(4)._5) { optPriceHistory =>
                optPriceHistory.get.taxId === tax.id.get
                optPriceHistory.get.currency === CurrencyInfo.Jpy
                optPriceHistory.get.unitPrice === BigDecimal(301)
              }

              list(0)._6(ItemNumericMetadataType.HEIGHT).metadata === 100
              list(1)._6(ItemNumericMetadataType.HEIGHT).metadata === 300
              list(2)._6(ItemNumericMetadataType.HEIGHT).metadata === 500
              list(3)._6(ItemNumericMetadataType.HEIGHT).metadata === 400
              list(4)._6(ItemNumericMetadataType.HEIGHT).metadata === 200
            }
          }
        }}
      }
    }

    "Can create sql for item query." in {
      Item.createQueryConditionSql(QueryString(List("Hello", "World")), CategorySearchCondition.Null, None) ===
        "and (item_name.item_name like {query0} or item_description.description like {query0}) " +
        "and (item_name.item_name like {query1} or item_description.description like {query1}) "

      Item.createQueryConditionSql(QueryString(List("Hello", "World")), CategorySearchCondition(123L), None) ===
        "and (item_name.item_name like {query0} or item_description.description like {query0}) " +
        "and (item_name.item_name like {query1} or item_description.description like {query1}) " +
        """
          and (
            item.category_id in (
              select descendant from category_path where ancestor in (123)
            )
            or exists (
              select descendant from category_path
              where ancestor in (
                select category_id from supplemental_category where item_id = item.item_id
              )
              and descendant in (123)
            )
          )
        """

      Item.createQueryConditionSql(QueryString(List()), CategorySearchCondition(123L), None) ===
        """
          and (
            item.category_id in (
              select descendant from category_path where ancestor in (123)
            )
            or exists (
              select descendant from category_path
              where ancestor in (
                select category_id from supplemental_category where item_id = item.item_id
              )
              and descendant in (123)
            )
          )
        """

      Item.createQueryConditionSql(QueryString(List()), CategorySearchCondition.Null, Some(234L)) ===
        "and site.site_id = 234 "
    }

    "Can get ite information from site id and item id." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          import LocaleInfo._
          val startTime = System.currentTimeMillis
          TestHelper.removePreloadedRecords()

          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.Ja, "商店2")

          val cat1 = Category.createNew(Map(Ja -> "植木", En -> "Plant"))
          val cat2 = Category.createNew(Map(Ja -> "果樹", En -> "Fruit"))
          
          val item1 = Item.createNew(cat1)
          val item2 = Item.createNew(cat2)
          
          val name1 = ItemName.createNew(item1, Map(LocaleInfo.Ja -> "杉", LocaleInfo.En -> "Cedar"))
          val name2 = ItemName.createNew(item2, Map(LocaleInfo.Ja -> "桃", LocaleInfo.En -> "Peach"))

          val siteItem1 = SiteItem.createNew(site1, item1)
          val siteItem2 = SiteItem.createNew(site1, item2)

          SiteItem.createNew(site2, item1)

          doWith(SiteItem.getWithSiteAndItem(site1.id.get, item1.id.get, Ja).get) { rec =>
            rec._1 === site1
            rec._2 === name1(Ja)
          }
          doWith(SiteItem.getWithSiteAndItem(site1.id.get, item1.id.get, En).get) { rec =>
            rec._1 === site1
            rec._2 === name1(En)
          }

          doWith(SiteItem.getWithSiteAndItem(site1.id.get, item2.id.get, Ja).get) { rec =>
            rec._1 === site1
            rec._2 === name2(Ja)
          }

          doWith(SiteItem.getWithSiteAndItem(site2.id.get, item1.id.get, Ja).get) { rec =>
            rec._1 === site2
            rec._2 === name1(Ja)
          }

          SiteItem.getWithSiteAndItem(site2.id.get, item2.id.get, Ja) === None

          val currentTime = System.currentTimeMillis
          doWith(SiteItem.list(item1.id.get)) { tbl =>
            tbl.size === 2
            tbl(0)._2.itemId.id === item1.id.get.id
            tbl(0)._2.created must be_>=(startTime)
            tbl(0)._2.created must be_<=(currentTime)

            tbl(1)._2.itemId.id === item1.id.get.id
            tbl(1)._2.created must be_>=(startTime)
            tbl(1)._2.created must be_<=(currentTime)
          }
        }
      }
    }
  }
}
