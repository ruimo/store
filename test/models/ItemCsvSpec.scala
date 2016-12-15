package models

import com.ruimo.scoins.Zip
import java.nio.file.Paths
import java.sql.Connection
import org.specs2.mutable.{After, Specification}
import org.specs2.specification.Scope
import com.ruimo.scoins.PathUtil
import java.nio.file.Files

import scala.collection.immutable
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
import helpers.{CategoryIdSearchCondition, CategoryCodeSearchCondition}
import com.ruimo.scoins.Scoping._

class ItemCsvSpec extends Specification {
  "ItemCsv" should {
    "Can parse comman" in {
      ItemCsv.Command.get("") === Some(ItemCsv.CommentCommand)
      ItemCsv.Command.get("-1") === None
      ItemCsv.Command.get("0") === Some(ItemCsv.LocaleCommand)
      ItemCsv.Command.get("1") === Some(ItemCsv.ItemCommand)
      ItemCsv.Command.get("2") === None
      ItemCsv.Command.get("0: Locale") === Some(ItemCsv.LocaleCommand)
      ItemCsv.Command.get("1:Item") === Some(ItemCsv.ItemCommand)
      ItemCsv.Command.get("2:Item") === None
      ItemCsv.Command.get("0: Locale:FOO") === Some(ItemCsv.LocaleCommand)
    }

    "Can parse locale csv line" in {
      val line = List("0:言語", "1:日本語", "", "", "", "", "" , "", "", "", "", "", "", "", "","", "")
      val csvLine = ItemCsv.readCsvLine(1, line.iterator)
      csvLine === ItemCsv.LocaleCsvLine(1L)
    }

    "Can parse comment csv line" in {
      val line = List("", "C: Create\\nU: Update\\nD: Delete\r\n")
      val csvLine = ItemCsv.readCsvLine(1, line.iterator)
      csvLine === ItemCsv.CommentCsvLine
    }

    "Can parse item csv line" in {
      val line = List(
        "1: 商品",
        "C",
        "",
        "26:E&Gアカデミー",
        "1001:リゾート",
        "0:非クーポン",
        "商品1",
        "1000",
        "1000",
        "800",
        "1200",
        "1:JPY",
        "商品1説明",
        "",
        "",
        "3:商品を隠す/1/9999-12-31 23:59:59\r\n3:商品を隠す/0/2017-01-01 00:00:00",
        "0:価格メモ/メモ内容1\r\n1:定価メモ/メモ内容2",
        "item1000-01.jpg\r\nitem1000-02.jpg",
        "itemDetail.jpg"
      )
      val csvLine = ItemCsv.readCsvLine(1, line.iterator)
      csvLine === ItemCsv.ItemCsvLine(
        ItemCsv.CrudCreate, None, 26L, 1001L, false, "商品1", 1000L, BigDecimal(1000), Some(BigDecimal(800)), Some(BigDecimal(1200)),
        1L, "商品1説明", immutable.Seq(), immutable.Seq(),
        immutable.Seq(
          ItemCsv.SiteItemNumericMetadataField(
            SiteItemNumericMetadataType.HIDE,
            1L,
            java.sql.Timestamp.valueOf("9999-12-31 23:59:59").getTime()
          ),
          ItemCsv.SiteItemNumericMetadataField(
            SiteItemNumericMetadataType.HIDE,
            0L,
            java.sql.Timestamp.valueOf("2017-01-01 00:00:00").getTime()
          )
        ),
        immutable.Seq(
          ItemCsv.SiteItemTextMetadataField(
            SiteItemTextMetadataType.PRICE_MEMO,
            "メモ内容1"
          ),
          ItemCsv.SiteItemTextMetadataField(
            SiteItemTextMetadataType.LIST_PRICE_MEMO,
            "メモ内容2"
          )
        ),
        immutable.Seq("item1000-01.jpg", "item1000-02.jpg"),
        "itemDetail.jpg"
      )
    }

    "Can process csv line that has invalid command." in {
      val line = List(
        "100: 商品",
        "C",
        "",
        "26:E&Gアカデミー",
        "1001:リゾート",
        "0:非クーポン",
        "商品1",
        "1000",
        "1000",
        "800",
        "1200",
        "1:JPY",
        "商品1説明",
        "",
        "",
        "3:商品を隠す/1/9999-12-31 23:59:59\r\n3:商品を隠す/0/2017-01-01 00:00:00",
        "",
        "item1000-01.jpg\r\nitem1000-02.jpg",
        "itemDetail.jpg"
      )
      try {
        ItemCsv.readCsvLine(1, line.iterator)
        throw new java.lang.Error("Logic error.")
      }
      catch {
        case e: ItemCsv.InvalidColumnException =>
          e.value === "100: 商品"
      }
    }

    "Can process csv line that has invalid command format." in {
      val line = List(
        "商品",
        "C",
        "",
        "26:E&Gアカデミー",
        "1001:リゾート",
        "0:非クーポン",
        "商品1",
        "1000",
        "1000",
        "800",
        "1200",
        "1:JPY",
        "商品1説明",
        "",
        "",
        "3:商品を隠す/1/9999-12-31 23:59:59\r\n3:商品を隠す/0/2017-01-01 00:00:00",
        "",
        "item1000-01.jpg\r\nitem1000-02.jpg",
        "itemDetail.jpg"
      )
      try {
        ItemCsv.readCsvLine(1, line.iterator)
        throw new java.lang.Error("Logic error.")
      }
      catch {
        case e: ItemCsv.InvalidColumnException =>
          e.value === "商品"
      }
    }

    "Can process csv line that has invalid crud format." in {
      val line = List(
        "1: 商品",
        "T",
        "",
        "26:E&Gアカデミー",
        "1001:リゾート",
        "0:非クーポン",
        "商品1",
        "1000",
        "1000",
        "800",
        "1200",
        "1:JPY",
        "商品1説明",
        "",
        "",
        "3:商品を隠す/1/9999-12-31 23:59:59\r\n3:商品を隠す/0/2017-01-01 00:00:00",
        "",
        "item1000-01.jpg\r\nitem1000-02.jpg",
        "itemDetail.jpg"
      )
      try {
        ItemCsv.readCsvLine(1, line.iterator)
        throw new java.lang.Error("Logic error.")
      }
      catch {
        case e: ItemCsv.InvalidColumnException =>
          e.colNo === 2
      }
    }

    "Can process csv line that has invalid crud." in {
      val line = List(
        "1: 商品",
        "T",
        "",
        "26:E&Gアカデミー",
        "1001:リゾート",
        "0:非クーポン",
        "商品1",
        "1000",
        "1000",
        "800",
        "1200",
        "1:JPY",
        "商品1説明",
        "",
        "",
        "3:商品を隠す/1/9999-12-31 23:59:59\r\n3:商品を隠す/0/2017-01-01 00:00:00",
        "",
        "item1000-01.jpg\r\nitem1000-02.jpg",
        "itemDetail.jpg"
      )
      try {
        ItemCsv.readCsvLine(1, line.iterator)
        throw new java.lang.Error("Logic error.")
      }
      catch {
        case e: ItemCsv.InvalidColumnException =>
          e.value === "T"
      }
    }

    abstract class WithTempPictureDir extends Scope with After {
      val testDir = Files.createTempDirectory(null)
      val withTestItemPictureDir = Map(
        "item.picture.path" -> testDir.toFile.getAbsolutePath,
        "item.picture.fortest" -> true
      )

      def after = {
        PathUtil.deleteDir(testDir)
      }
    }

    "Can persist item csv line" in new WithTempPictureDir {
      new WithApplication(
        FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTestItemPictureDir)
      ) {
        PathUtil.withTempDir(None) { dir =>
          DB.withTransaction { implicit conn =>
            val cat = Category.createNew(
              Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
            )
            val site = Site.createNew(LocaleInfo.Ja, "商店1")
            val tax = Tax.createNew

            val csvText = s"""0:言語,1:日本語,,,,,,,,,,,,,,,,,
,"C: Create
U: Update
D: Delete
現在はCのみサポート",商品Id(C: Createの場合は空欄),店舗Id,カテゴリId,"0:非クーポン
1:クーポン",商品名,"1000:外税
1002:内税
1003:非課税",価格,原価,定価,通貨,詳細,"値
現在は未サポート","商品情報(文字列)
現在は未サポート",店舗固有データ,"店舗固有データ(文字列)
現在は未サポート",商品画像,詳細商品画像
1: 商品,C,,${site.id.get},${cat.id.get},0:非クーポン,商品1,${tax.id.get},1000,800,1200,1:JPY,商品1説明,,,"3:商品を隠す/0/9999-12-31 23:59:59
3:商品を隠す/1/2016-01-01 00:00:00","0:価格メモ/メモ内容
1:定価メモ/メモ内容2","item1000-01.jpg
item1000-02.jpg",itemDetail.jpg"""

            val csvFile = dir.resolve("items.csv")
            Files.write(csvFile, csvText.getBytes("Windows-31j"))

            val zipFile = dir.resolve("test.zip")
            Zip.deflate(zipFile, List(
              "items.csv" -> csvFile,
              "item1000-01.jpg" -> Paths.get("testdata/itemcsv/case001/item1000-01.jpg"),
              "item1000-02.jpg" -> Paths.get("testdata/itemcsv/case001/item1000-02.jpg"),
              "itemDetail.jpg" -> Paths.get("testdata/itemcsv/case001/itemDetail.jpg")
            ))

            controllers.ItemMaintenanceByCsv.processItemCsv(zipFile)
            val recs: PagedRecords[(
              Item, ItemName, ItemDescription, Site, ItemPriceHistory,
              Map[ItemNumericMetadataType, ItemNumericMetadata],
              Map[SiteItemNumericMetadataType, SiteItemNumericMetadata],
              Map[ItemTextMetadataType, ItemTextMetadata],
              Map[SiteItemTextMetadataType, SiteItemTextMetadata]
            )] = Item.list(
              locale = LocaleInfo.Ja,
              queryString = QueryString("")
            )

            doWith(recs.records) { recTbl =>
              recTbl.size === 1
              doWith(recTbl(0)) { rec =>
                val item = rec._1

                doWith(rec._2) { itemName =>
                  itemName.localeId === LocaleInfo.Ja.id
                  itemName.itemId === item.id.get
                  itemName.name === "商品1"
                }
                doWith(rec._3) { itemDesc =>
                  itemDesc.localeId === LocaleInfo.Ja.id
                  itemDesc.itemId === item.id.get
                  itemDesc.siteId === site.id.get
                }
                doWith(rec._4) { siteRec =>
                  siteRec.localeId === LocaleInfo.Ja.id
                  siteRec.name === "商店1"
                }
                doWith(rec._5) { itemPriceHis =>
                  itemPriceHis.taxId === tax.id.get
                  itemPriceHis.currency === CurrencyInfo.Jpy
                  itemPriceHis.unitPrice === BigDecimal(1000)
                  itemPriceHis.listPrice === Some(BigDecimal(1200))
                  itemPriceHis.costPrice === BigDecimal(800)
                  itemPriceHis.validUntil === Until.Ever
                }
                rec._6 === Map()
                doWith(rec._7(SiteItemNumericMetadataType.HIDE)) { md =>
                  md.itemId === item.id.get
                  md.siteId === site.id.get
                  md.metadataType === SiteItemNumericMetadataType.HIDE
                  md.metadata === 0L
                  md.validUntil === Until.Ever
                }
                rec._8 === Map()
                doWith(rec._9(SiteItemTextMetadataType.PRICE_MEMO)) { md =>
                  md.itemId === item.id.get
                  md.siteId === site.id.get
                  md.metadataType === SiteItemTextMetadataType.PRICE_MEMO
                  md.metadata === "メモ内容"
                }
                doWith(rec._9(SiteItemTextMetadataType.LIST_PRICE_MEMO)) { md =>
                  md.itemId === item.id.get
                  md.siteId === site.id.get
                  md.metadataType === SiteItemTextMetadataType.LIST_PRICE_MEMO
                  md.metadata === "メモ内容2"
                }
                Files.readAllLines(controllers.ItemPictures.toPath(item.id.get.id, 0)) ===
                Files.readAllLines(Paths.get("testdata/itemcsv/case001/item1000-01.jpg"))

                Files.readAllLines(controllers.ItemPictures.toPath(item.id.get.id, 1)) ===
                Files.readAllLines(Paths.get("testdata/itemcsv/case001/item1000-02.jpg"))

                Files.readAllLines(controllers.ItemPictures.toDetailPath(item.id.get.id)) ===
                Files.readAllLines(Paths.get("testdata/itemcsv/case001/itemDetail.jpg"))
              }
            }
          }
        }.get
      }
    }

    "Can persist item csv lines" in new WithTempPictureDir {
      new WithApplication(
        FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTestItemPictureDir)
      ) {
        PathUtil.withTempDir(None) { dir =>
          DB.withTransaction { implicit conn =>
            val cat = Category.createNew(
              Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
            )
            val site = Site.createNew(LocaleInfo.Ja, "商店1")
            val site2 = Site.createNew(LocaleInfo.Ja, "商店2")
            val tax = Tax.createNew

            val csvText = s"""0:言語,1:日本語,,,,,,,,,,,,,,,,,
,"C: Create
U: Update
D: Delete
現在はCのみサポート",商品Id(C: Createの場合は空欄),店舗Id,カテゴリId,"0:非クーポン
1:クーポン",商品名,"1000:外税
1002:内税
1003:非課税",価格,原価,定価,通貨,詳細,"値
現在は未サポート","商品情報(文字列)
現在は未サポート",店舗固有データ,"店舗固有データ(文字列)
現在は未サポート",商品画像,詳細商品画像
1: 商品,C,,${site.id.get},${cat.id.get},0:非クーポン,商品1,${tax.id.get},1000,800,1200,1:JPY,商品1説明,,,"3:商品を隠す/0/9999-12-31 23:59:59
3:商品を隠す/1/2016-01-01 00:00:00","0:価格メモ/メモ内容
1:定価メモ/メモ内容2","item1000-01.jpg
item1000-02.jpg",itemDetail.jpg
1: 商品,C,,${site2.id.get},${cat.id.get},0:非クーポン,商品2,${tax.id.get},2000,1600,2400,1:JPY,商品2説明,,,"3:商品を隠す/0/9999-12-31 23:59:59
3:商品を隠す/1/2016-02-01 00:00:00","0:価格メモ/メモ内容2-1
1:定価メモ/メモ内容2-2","item2000-01.jpg
item2000-02.jpg",itemDetail2.jpg"""

            val csvFile = dir.resolve("items.csv")
            Files.write(csvFile, csvText.getBytes("Windows-31j"))

            val zipFile = dir.resolve("test.zip")
            Zip.deflate(zipFile, List(
              "items.csv" -> csvFile,
              "item1000-01.jpg" -> Paths.get("testdata/itemcsv/case001/item1000-01.jpg"),
              "item1000-02.jpg" -> Paths.get("testdata/itemcsv/case001/item1000-02.jpg"),
              "itemDetail.jpg" -> Paths.get("testdata/itemcsv/case001/itemDetail.jpg"),
              "item2000-01.jpg" -> Paths.get("testdata/itemcsv/case001/item1000-01.jpg"),
              "item2000-02.jpg" -> Paths.get("testdata/itemcsv/case001/item1000-02.jpg"),
              "itemDetail2.jpg" -> Paths.get("testdata/itemcsv/case001/itemDetail.jpg")
            ))

            controllers.ItemMaintenanceByCsv.processItemCsv(zipFile)
            val recs: PagedRecords[(
              Item, ItemName, ItemDescription, Site, ItemPriceHistory,
              Map[ItemNumericMetadataType, ItemNumericMetadata],
              Map[SiteItemNumericMetadataType, SiteItemNumericMetadata],
              Map[ItemTextMetadataType, ItemTextMetadata],
              Map[SiteItemTextMetadataType, SiteItemTextMetadata]
            )] = Item.list(
              locale = LocaleInfo.Ja,
              queryString = QueryString("")
            )

            doWith(recs.records) { recTbl =>
              recTbl.size === 2
              doWith(recTbl(0)) { rec =>
                val item = rec._1

                doWith(rec._2) { itemName =>
                  itemName.localeId === LocaleInfo.Ja.id
                  itemName.itemId === item.id.get
                  itemName.name === "商品1"
                }
                doWith(rec._3) { itemDesc =>
                  itemDesc.localeId === LocaleInfo.Ja.id
                  itemDesc.itemId === item.id.get
                  itemDesc.siteId === site.id.get
                }
                doWith(rec._4) { siteRec =>
                  siteRec.localeId === LocaleInfo.Ja.id
                  siteRec.name === "商店1"
                }
                doWith(rec._5) { itemPriceHis =>
                  itemPriceHis.taxId === tax.id.get
                  itemPriceHis.currency === CurrencyInfo.Jpy
                  itemPriceHis.unitPrice === BigDecimal(1000)
                  itemPriceHis.listPrice === Some(BigDecimal(1200))
                  itemPriceHis.costPrice === BigDecimal(800)
                  itemPriceHis.validUntil === Until.Ever
                }
                rec._6 === Map()
                doWith(rec._7(SiteItemNumericMetadataType.HIDE)) { md =>
                  md.itemId === item.id.get
                  md.siteId === site.id.get
                  md.metadataType === SiteItemNumericMetadataType.HIDE
                  md.metadata === 0L
                  md.validUntil === Until.Ever
                }
                rec._8 === Map()
                doWith(rec._9(SiteItemTextMetadataType.PRICE_MEMO)) { md =>
                  md.itemId === item.id.get
                  md.siteId === site.id.get
                  md.metadataType === SiteItemTextMetadataType.PRICE_MEMO
                  md.metadata === "メモ内容"
                }
                doWith(rec._9(SiteItemTextMetadataType.LIST_PRICE_MEMO)) { md =>
                  md.itemId === item.id.get
                  md.siteId === site.id.get
                  md.metadataType === SiteItemTextMetadataType.LIST_PRICE_MEMO
                  md.metadata === "メモ内容2"
                }
                Files.readAllLines(controllers.ItemPictures.toPath(item.id.get.id, 0)) ===
                Files.readAllLines(Paths.get("testdata/itemcsv/case001/item1000-01.jpg"))

                Files.readAllLines(controllers.ItemPictures.toPath(item.id.get.id, 1)) ===
                Files.readAllLines(Paths.get("testdata/itemcsv/case001/item1000-02.jpg"))

                Files.readAllLines(controllers.ItemPictures.toDetailPath(item.id.get.id)) ===
                Files.readAllLines(Paths.get("testdata/itemcsv/case001/itemDetail.jpg"))
              }

              doWith(recTbl(1)) { rec =>
                val item = rec._1

                doWith(rec._2) { itemName =>
                  itemName.localeId === LocaleInfo.Ja.id
                  itemName.itemId === item.id.get
                  itemName.name === "商品2"
                }
                doWith(rec._3) { itemDesc =>
                  itemDesc.localeId === LocaleInfo.Ja.id
                  itemDesc.itemId === item.id.get
                  itemDesc.siteId === site2.id.get
                }
                doWith(rec._4) { siteRec =>
                  siteRec.localeId === LocaleInfo.Ja.id
                  siteRec.name === "商店2"
                }
                doWith(rec._5) { itemPriceHis =>
                  itemPriceHis.taxId === tax.id.get
                  itemPriceHis.currency === CurrencyInfo.Jpy
                  itemPriceHis.unitPrice === BigDecimal(2000)
                  itemPriceHis.listPrice === Some(BigDecimal(2400))
                  itemPriceHis.costPrice === BigDecimal(1600)
                  itemPriceHis.validUntil === Until.Ever
                }
                rec._6 === Map()
                doWith(rec._7(SiteItemNumericMetadataType.HIDE)) { md =>
                  md.itemId === item.id.get
                  md.siteId === site2.id.get
                  md.metadataType === SiteItemNumericMetadataType.HIDE
                  md.metadata === 0L
                  md.validUntil === Until.Ever
                }
                rec._8 === Map()
                doWith(rec._9(SiteItemTextMetadataType.PRICE_MEMO)) { md =>
                  md.itemId === item.id.get
                  md.siteId === site2.id.get
                  md.metadataType === SiteItemTextMetadataType.PRICE_MEMO
                  md.metadata === "メモ内容2-1"
                }
                doWith(rec._9(SiteItemTextMetadataType.LIST_PRICE_MEMO)) { md =>
                  md.itemId === item.id.get
                  md.siteId === site2.id.get
                  md.metadataType === SiteItemTextMetadataType.LIST_PRICE_MEMO
                  md.metadata === "メモ内容2-2"
                }
                Files.readAllLines(controllers.ItemPictures.toPath(item.id.get.id, 0)) ===
                Files.readAllLines(Paths.get("testdata/itemcsv/case001/item1000-01.jpg"))

                Files.readAllLines(controllers.ItemPictures.toPath(item.id.get.id, 1)) ===
                Files.readAllLines(Paths.get("testdata/itemcsv/case001/item1000-02.jpg"))

                Files.readAllLines(controllers.ItemPictures.toDetailPath(item.id.get.id)) ===
                Files.readAllLines(Paths.get("testdata/itemcsv/case001/itemDetail.jpg"))
              }
            }
          }
        }.get
      }
    }

    "Error should be reported if siteid is not found" in new WithTempPictureDir {
      new WithApplication(
        FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTestItemPictureDir)
      ) {
        PathUtil.withTempDir(None) { dir =>
          DB.withTransaction { implicit conn =>
            val cat = Category.createNew(
              Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
            )
            val site = Site.createNew(LocaleInfo.Ja, "商店1")
            val tax = Tax.createNew

            val csvText = s"""0:言語,1:日本語,,,,,,,,,,,,,,,,,
,"C: Create
U: Update
D: Delete
現在はCのみサポート",商品Id(C: Createの場合は空欄),店舗Id,カテゴリId,"0:非クーポン
1:クーポン",商品名,"1000:外税
1002:内税
1003:非課税",価格,原価,定価,通貨,詳細,"値
現在は未サポート","商品情報(文字列)
現在は未サポート",店舗固有データ,"店舗固有データ(文字列)
現在は未サポート",商品画像,詳細商品画像
1: 商品,C,,${site.id.get + 1},${cat.id.get},0:非クーポン,商品1,${tax.id.get},1000,800,1200,1:JPY,商品1説明,,,"3:商品を隠す/0/9999-12-31 23:59:59
3:商品を隠す/1/2016-01-01 00:00:00","0:価格メモ/メモ内容
1:定価メモ/メモ内容2","item1000-01.jpg
item1000-02.jpg",itemDetail.jpg"""

            val csvFile = dir.resolve("items.csv")
            Files.write(csvFile, csvText.getBytes("Windows-31j"))

            val zipFile = dir.resolve("test.zip")
            Zip.deflate(zipFile, List(
              "items.csv" -> csvFile,
              "item1000-01.jpg" -> Paths.get("testdata/itemcsv/case001/item1000-01.jpg"),
              "item1000-02.jpg" -> Paths.get("testdata/itemcsv/case001/item1000-02.jpg"),
              "itemDetail.jpg" -> Paths.get("testdata/itemcsv/case001/itemDetail.jpg")
            ))

            try {
              controllers.ItemMaintenanceByCsv.processItemCsv(zipFile)
              throw new AssertionError("Test fail")
            }
            catch {
              case e: ItemCsv.InvalidSiteException =>
                e.lineNo === 3
              case t: Throwable => throw new AssertionError("Test fail ", t)
            }
          }
        }.get
      }
    }

    "Error should be reported if categoryid is not found" in new WithTempPictureDir {
      new WithApplication(
        FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTestItemPictureDir)
      ) {
        PathUtil.withTempDir(None) { dir =>
          DB.withTransaction { implicit conn =>
            val cat = Category.createNew(
              Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
            )
            val site = Site.createNew(LocaleInfo.Ja, "商店1")
            val tax = Tax.createNew

            val csvText = s"""0:言語,1:日本語,,,,,,,,,,,,,,,,,
,"C: Create
U: Update
D: Delete
現在はCのみサポート",商品Id(C: Createの場合は空欄),店舗Id,カテゴリId,"0:非クーポン
1:クーポン",商品名,"1000:外税
1002:内税
1003:非課税",価格,原価,定価,通貨,詳細,"値
現在は未サポート","商品情報(文字列)
現在は未サポート",店舗固有データ,"店舗固有データ(文字列)
現在は未サポート",商品画像,詳細商品画像
1: 商品,C,,${site.id.get},${cat.id.get + 1},0:非クーポン,商品1,${tax.id.get},1000,800,1200,1:JPY,商品1説明,,,"3:商品を隠す/0/9999-12-31 23:59:59
3:商品を隠す/1/2016-01-01 00:00:00","0:価格メモ/メモ内容
1:定価メモ/メモ内容2","item1000-01.jpg
item1000-02.jpg",itemDetail.jpg"""

            val csvFile = dir.resolve("items.csv")
            Files.write(csvFile, csvText.getBytes("Windows-31j"))

            val zipFile = dir.resolve("test.zip")
            Zip.deflate(zipFile, List(
              "items.csv" -> csvFile,
              "item1000-01.jpg" -> Paths.get("testdata/itemcsv/case001/item1000-01.jpg"),
              "item1000-02.jpg" -> Paths.get("testdata/itemcsv/case001/item1000-02.jpg"),
              "itemDetail.jpg" -> Paths.get("testdata/itemcsv/case001/itemDetail.jpg")
            ))

            try {
              controllers.ItemMaintenanceByCsv.processItemCsv(zipFile)
              throw new AssertionError("Test fail")
            }
            catch {
              case e: ItemCsv.InvalidCategoryException =>
                e.lineNo === 3
              case t: Throwable => throw new AssertionError("Test fail ", t)
            }
          }
        }.get
      }
    }

    "Error should be reported if locale is not found" in new WithTempPictureDir {
      new WithApplication(
        FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTestItemPictureDir)
      ) {
        PathUtil.withTempDir(None) { dir =>
          DB.withTransaction { implicit conn =>
            val cat = Category.createNew(
              Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
            )
            val site = Site.createNew(LocaleInfo.Ja, "商店1")
            val tax = Tax.createNew

            val csvText = s"""0:言語,3:日本語,,,,,,,,,,,,,,,,,
,"C: Create
U: Update
D: Delete
現在はCのみサポート",商品Id(C: Createの場合は空欄),店舗Id,カテゴリId,"0:非クーポン
1:クーポン",商品名,"1000:外税
1002:内税
1003:非課税",価格,原価,定価,通貨,詳細,"値
現在は未サポート","商品情報(文字列)
現在は未サポート",店舗固有データ,"店舗固有データ(文字列)
現在は未サポート",商品画像,詳細商品画像
1: 商品,C,,${site.id.get},${cat.id.get + 1},0:非クーポン,商品1,${tax.id.get},1000,800,1200,1:JPY,商品1説明,,,"3:商品を隠す/0/9999-12-31 23:59:59
3:商品を隠す/1/2016-01-01 00:00:00","0:価格メモ/メモ内容
1:定価メモ/メモ内容2","item1000-01.jpg
item1000-02.jpg",itemDetail.jpg"""

            val csvFile = dir.resolve("items.csv")
            Files.write(csvFile, csvText.getBytes("Windows-31j"))

            val zipFile = dir.resolve("test.zip")
            Zip.deflate(zipFile, List(
              "items.csv" -> csvFile,
              "item1000-01.jpg" -> Paths.get("testdata/itemcsv/case001/item1000-01.jpg"),
              "item1000-02.jpg" -> Paths.get("testdata/itemcsv/case001/item1000-02.jpg"),
              "itemDetail.jpg" -> Paths.get("testdata/itemcsv/case001/itemDetail.jpg")
            ))

            try {
              controllers.ItemMaintenanceByCsv.processItemCsv(zipFile)
              throw new AssertionError("Test fail")
            }
            catch {
              case e: ItemCsv.InvalidLocaleException =>
                e.lineNo === 1
              case t: Throwable => throw new AssertionError("Test fail ", t)
            }
          }
        }.get
      }
    }

    "Error should be reported if currency is not found" in new WithTempPictureDir {
      new WithApplication(
        FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTestItemPictureDir)
      ) {
        PathUtil.withTempDir(None) { dir =>
          DB.withTransaction { implicit conn =>
            val cat = Category.createNew(
              Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
            )
            val site = Site.createNew(LocaleInfo.Ja, "商店1")
            val tax = Tax.createNew

            val csvText = s"""0:言語,1:日本語,,,,,,,,,,,,,,,,,
,"C: Create
U: Update
D: Delete
現在はCのみサポート",商品Id(C: Createの場合は空欄),店舗Id,カテゴリId,"0:非クーポン
1:クーポン",商品名,"1000:外税
1002:内税
1003:非課税",価格,原価,定価,通貨,詳細,"値
現在は未サポート","商品情報(文字列)
現在は未サポート",店舗固有データ,"店舗固有データ(文字列)
現在は未サポート",商品画像,詳細商品画像
1: 商品,C,,${site.id.get},${cat.id.get},0:非クーポン,商品1,${tax.id.get},1000,800,1200,3:JPY,商品1説明,,,"3:商品を隠す/0/9999-12-31 23:59:59
3:商品を隠す/1/2016-01-01 00:00:00","0:価格メモ/メモ内容
1:定価メモ/メモ内容2","item1000-01.jpg
item1000-02.jpg",itemDetail.jpg"""

            val csvFile = dir.resolve("items.csv")
            Files.write(csvFile, csvText.getBytes("Windows-31j"))

            val zipFile = dir.resolve("test.zip")
            Zip.deflate(zipFile, List(
              "items.csv" -> csvFile,
              "item1000-01.jpg" -> Paths.get("testdata/itemcsv/case001/item1000-01.jpg"),
              "item1000-02.jpg" -> Paths.get("testdata/itemcsv/case001/item1000-02.jpg"),
              "itemDetail.jpg" -> Paths.get("testdata/itemcsv/case001/itemDetail.jpg")
            ))

            try {
              controllers.ItemMaintenanceByCsv.processItemCsv(zipFile)
              throw new AssertionError("Test fail")
            }
            catch {
              case e: ItemCsv.InvalidCurrencyException =>
                e.lineNo === 3
              case t: Throwable => throw new AssertionError("Test fail ", t)
            }
          }
        }.get
      }
    }

    "Error should be reported if tax is not found" in new WithTempPictureDir {
      new WithApplication(
        FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTestItemPictureDir)
      ) {
        PathUtil.withTempDir(None) { dir =>
          DB.withTransaction { implicit conn =>
            val cat = Category.createNew(
              Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
            )
            val site = Site.createNew(LocaleInfo.Ja, "商店1")
            val tax = Tax.createNew

            val csvText = s"""0:言語,1:日本語,,,,,,,,,,,,,,,,,
,"C: Create
U: Update
D: Delete
現在はCのみサポート",商品Id(C: Createの場合は空欄),店舗Id,カテゴリId,"0:非クーポン
1:クーポン",商品名,"1000:外税
1002:内税
1003:非課税",価格,原価,定価,通貨,詳細,"値
現在は未サポート","商品情報(文字列)
現在は未サポート",店舗固有データ,"店舗固有データ(文字列)
現在は未サポート",商品画像,詳細商品画像
1: 商品,C,,${site.id.get},${cat.id.get},0:非クーポン,商品1,${tax.id.get + 1},1000,800,1200,1:JPY,商品1説明,,,"3:商品を隠す/0/9999-12-31 23:59:59
3:商品を隠す/1/2016-01-01 00:00:00","0:価格メモ/メモ内容
1:定価メモ/メモ内容2","item1000-01.jpg
item1000-02.jpg",itemDetail.jpg"""

            val csvFile = dir.resolve("items.csv")
            Files.write(csvFile, csvText.getBytes("Windows-31j"))

            val zipFile = dir.resolve("test.zip")
            Zip.deflate(zipFile, List(
              "items.csv" -> csvFile,
              "item1000-01.jpg" -> Paths.get("testdata/itemcsv/case001/item1000-01.jpg"),
              "item1000-02.jpg" -> Paths.get("testdata/itemcsv/case001/item1000-02.jpg"),
              "itemDetail.jpg" -> Paths.get("testdata/itemcsv/case001/itemDetail.jpg")
            ))

            try {
              controllers.ItemMaintenanceByCsv.processItemCsv(zipFile)
              throw new AssertionError("Test fail")
            }
            catch {
              case e: ItemCsv.InvalidTaxException =>
                e.lineNo === 3
              case t: Throwable => throw new AssertionError("Test fail ", t)
            }
          }
        }.get
      }
    }
  }
}

