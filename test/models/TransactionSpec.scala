package models

import org.specs2.mutable._

import anorm._
import anorm.SqlParser
import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current
import java.sql.Date.{valueOf => date}
import collection.immutable

class TransactionSpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  "TransactionLog" should {
    "Can persist header." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          TestHelper.removePreloadedRecords()
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )
          val currency1 = CurrencyInfo.Jpy
          val now = 1234L

          val header = TransactionLogHeader.createNew(
            user1.id.get, currency1.id,
            BigDecimal(234), BigDecimal(345),
            TransactionType.NORMAL
          )

          val list = TransactionLogHeader.list()
          list.size === 1
          header === list(0)
        }
      }
    }

    "Can persist shipping." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          TestHelper.removePreloadedRecords()
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )
          val currency1 = CurrencyInfo.Jpy
          val now = 1234L

          val header = TransactionLogHeader.createNew(
            user1.id.get, currency1.id,
            BigDecimal(234), BigDecimal(345),
            TransactionType.NORMAL
          )

          val addr1 = Address.createNew(
            countryCode = CountryCode.JPN,
            firstName = "FirstName",
            lastName = "LastName",
            zip1 = "123",
            prefecture = JapanPrefecture.東京都,
            address1 = "Address1",
            address2 = "Address2",
            tel1 = "12345678"
          )

          val tranSite = TransactionLogSite.createNew(
            header.id.get, site1.id.get, BigDecimal(234), BigDecimal(345)
          )

          val shipping = TransactionLogShipping.createNew(
            tranSite.id.get, BigDecimal(9876), addr1.id.get, 1L, 1, 1L, 1, "boxName", date("2013-05-05")
          )

          val list = TransactionLogShipping.list()
          list.size === 1
          list(0) === shipping
        }
      }
    }

    "Can persist tax." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          TestHelper.removePreloadedRecords()

          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )
          val currency1 = CurrencyInfo.Jpy
          val now = 1234L

          val header = TransactionLogHeader.createNew(
            user1.id.get, currency1.id,
            BigDecimal(234), BigDecimal(345),
            TransactionType.NORMAL
          )
          
          val tranSite = TransactionLogSite.createNew(
            header.id.get, site1.id.get, BigDecimal(234), BigDecimal(345)
          )

          val tax = TransactionLogTax.createNew(
            tranSite.id.get,
            1234L, 2345L, TaxType.INNER_TAX,
            BigDecimal(5), BigDecimal(333), BigDecimal(222)
          )

          val list = TransactionLogTax.list()
          list.size === 1
          list(0) === tax
        }
      }
    }

    "Can persist item." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          TestHelper.removePreloadedRecords()

          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )
          val currency1 = CurrencyInfo.Jpy
          val now = 1234L

          val header = TransactionLogHeader.createNew(
            user1.id.get, currency1.id,
            BigDecimal(234), BigDecimal(345),
            TransactionType.NORMAL
          )

          val tranSite = TransactionLogSite.createNew(
            header.id.get, site1.id.get, BigDecimal(234), BigDecimal(345)
          )

          val cat = Category.createNew(Map(LocaleInfo.Ja -> "Category"))
          val item = Item.createNew(cat)

          val itemLog = TransactionLogItem.createNew(
            tranSite.id.get, item.id.get.id, 1234L, 234L, BigDecimal(456), BigDecimal(400), 123L
          )

          val list = TransactionLogItem.list()
          list.size === 1
          list(0) === itemLog
          list(0).itemId === item.id.get.id
          list(0).taxId === 123L
        }
      }
    }

    "Can persist whole transaction." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          val tax1 = Tax.createNew
          val tax2 = Tax.createNew
          val taxHistory1 = TaxHistory.createNew(tax1, TaxType.OUTER_TAX, BigDecimal("5"), date("9999-12-31"))
          val taxHistory2 = TaxHistory.createNew(tax2, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

          val user1 = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL, Some("companyName")
          )
          import models.LocaleInfo.{Ja}
          val site1 = Site.createNew(Ja, "商店1")
          val site2 = Site.createNew(Ja, "商店2")

          val cat1 = Category.createNew(Map(Ja -> "植木"))

          val item1 = Item.createNew(cat1)
          val item2 = Item.createNew(cat1)

          val name1 = ItemName.createNew(item1, Map(Ja -> "杉"))
          val name2 = ItemName.createNew(item2, Map(Ja -> "梅"))

          SiteItem.createNew(site1, item1)
          SiteItem.createNew(site2, item2)

          val desc1 = ItemDescription.createNew(item1, site1, "杉説明")
          val desc2 = ItemDescription.createNew(item2, site1, "梅説明")

          val price1 = ItemPrice.createNew(item1, site1)
          val price2 = ItemPrice.createNew(item2, site2)

          val ph1 = ItemPriceHistory.createNew(price1, tax1, CurrencyInfo.Jpy, BigDecimal(119), BigDecimal(100), date("9999-12-31"))
          val ph2 = ItemPriceHistory.createNew(price2, tax1, CurrencyInfo.Jpy, BigDecimal(59), BigDecimal(50), date("9999-12-31"))

          val cart1 = ShoppingCartItem.addItem(user1.id.get, site1.id.get, item1.id.get.id, 1)
          val cart2 = ShoppingCartItem.addItem(user1.id.get, site2.id.get, item2.id.get.id, 1)

          val itemClass1 = 1L

          val box1 = ShippingBox.createNew(site1.id.get, itemClass1, 10, "小箱")
          val box2 = ShippingBox.createNew(site2.id.get, itemClass1, 3, "小箱")
          val shipping1 = ShippingFee.createNew(box1.id.get, CountryCode.JPN, JapanPrefecture.東京都.code)
          val shipping2 = ShippingFee.createNew(box2.id.get, CountryCode.JPN, JapanPrefecture.東京都.code)
          val shipHis1 = ShippingFeeHistory.createNew(
            shipping1.id.get, tax2.id.get, BigDecimal(1234), date("9999-12-31")
          )
          val shipHis2 = ShippingFeeHistory.createNew(
            shipping2.id.get, tax2.id.get, BigDecimal(2345), date("9999-12-31")
          )

          val shippingTotal = ShippingFeeHistory.feeBySiteAndItemClass(
            CountryCode.JPN, JapanPrefecture.東京都.code,
            ShippingFeeEntries()
              .add(site1, itemClass1, 3)
              .add(site2, itemClass1, 5)
          )

          val cart = ShoppingCartItem.listItemsForUser(Ja, user1.id.get)
          val addr = Address.createNew(
            countryCode = CountryCode.JPN,
            firstName = "FirstName",
            lastName = "LastName",
            zip1 = "123",
            prefecture = JapanPrefecture.東京都,
            address1 = "Address1",
            address2 = "Address2",
            tel1 = "12345678"
          )

          val shippingDate = ShippingDate(
            Map(
              site1.id.get -> ShippingDateEntry(site1.id.get, date("2013-02-03")),
              site2.id.get -> ShippingDateEntry(site2.id.get, date("2013-05-03"))
            )
          )
          val persister = new TransactionPersister
          val tranNo = persister.persist(
            Transaction(user1.id.get, CurrencyInfo.Jpy, cart, addr, 
                        controllers.Shipping.shippingFee(addr, cart), shippingDate)
          )

          val ptran = persister.load(tranNo, Ja)
          ptran.header.id.get === tranNo
          ptran.header.userId === user1.id.get
          ptran.header.currencyId === CurrencyInfo.Jpy.id
          ptran.header.totalAmount === BigDecimal(119 + 59 + 1234 + 2345)
          ptran.header.taxAmount === BigDecimal(
            (119 + 59) * 5 / 100 +
            (1234 + 2345) * 5 / 105
          )
          ptran.header.transactionType === TransactionType.NORMAL
          ptran.siteTable.size == 2
          ptran.siteTable.contains(site1) === true
          ptran.siteTable.contains(site2) === true
          ptran.shippingTable.size === 2
          ptran.shippingTable(site1.id.get).size === 1
          val tranShipping1 = ptran.shippingTable(site1.id.get).head
          tranShipping1.amount === 1234
          tranShipping1.addressId === addr.id.get
          tranShipping1.itemClass === itemClass1
          tranShipping1.boxSize === 10
          tranShipping1.taxId === tax2.id.get
          tranShipping1.shippingDate === date("2013-02-03").getTime

          ptran.shippingTable(site2.id.get).size === 1
          val tranShipping2 = ptran.shippingTable(site2.id.get).head
          tranShipping2.amount === 2345
          tranShipping2.addressId === addr.id.get
          tranShipping2.itemClass === itemClass1
          tranShipping2.boxSize === 3
          tranShipping2.taxId === tax2.id.get
          tranShipping2.shippingDate === date("2013-05-03").getTime

          ptran.taxTable.size === 2
          val taxTable1 = ptran.taxTable(site1.id.get).foldLeft(immutable.LongMap[TransactionLogTax]()) {
            (map, e) => map.updated(e.taxId, e)
          }
          taxTable1.size === 2
          taxTable1(tax1.id.get).taxType === TaxType.OUTER_TAX
          taxTable1(tax1.id.get).rate === BigDecimal(5)
          taxTable1(tax1.id.get).targetAmount === BigDecimal(119)
          taxTable1(tax1.id.get).amount === BigDecimal(119 * 5 / 100)

          taxTable1(tax2.id.get).taxType === TaxType.INNER_TAX
          taxTable1(tax2.id.get).rate === BigDecimal(5)
          taxTable1(tax2.id.get).targetAmount === BigDecimal(1234)
          taxTable1(tax2.id.get).amount === BigDecimal(1234 * 5 / 105)

          val taxTable2 = ptran.taxTable(site2.id.get).foldLeft(immutable.LongMap[TransactionLogTax]()) {
            (map, e) => map.updated(e.taxId, e)
          }
          taxTable2.size === 2
          taxTable2(tax1.id.get).taxType === TaxType.OUTER_TAX
          taxTable2(tax1.id.get).rate === BigDecimal(5)
          taxTable2(tax1.id.get).targetAmount === BigDecimal(59)
          taxTable2(tax1.id.get).amount === BigDecimal(59 * 5 / 100)

          taxTable2(tax2.id.get).taxType === TaxType.INNER_TAX
          taxTable2(tax2.id.get).rate === BigDecimal(5)
          taxTable2(tax2.id.get).targetAmount === BigDecimal(2345)
          taxTable2(tax2.id.get).amount === BigDecimal(2345 * 5 / 105)

          ptran.itemTable.size === 2
          val itemTable1 = ptran.itemTable(site1.id.get).foldLeft(immutable.HashMap[ItemId, (ItemName, TransactionLogItem)]()) {
            (map, e) => map.updated(e._1.itemId, e)
          }
          itemTable1(item1.id.get)._1.name === "杉"
          itemTable1(item1.id.get)._2.itemId === item1.id.get.id
          itemTable1(item1.id.get)._2.itemPriceHistoryId === ph1.id.get
          itemTable1(item1.id.get)._2.quantity === 1
          itemTable1(item1.id.get)._2.amount === BigDecimal(119)
          
          val itemTable2 = ptran.itemTable(site2.id.get).foldLeft(immutable.HashMap[ItemId, (ItemName, TransactionLogItem)]()) {
            (map, e) => map.updated(e._1.itemId, e)
          }
          itemTable2(item2.id.get)._1.name === "梅"
          itemTable2(item2.id.get)._2.itemId === item2.id.get.id
          itemTable2(item2.id.get)._2.itemPriceHistoryId === ph2.id.get
          itemTable2(item2.id.get)._2.quantity === 1
          itemTable2(item2.id.get)._2.amount === BigDecimal(59)
        }
      }
    }

    "Site owners cannot modify transaction status of other site owneres." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          TestHelper.removePreloadedRecords()

          val currency1 = CurrencyInfo.Jpy
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.Ja, "商店2")
          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )
          val siteUser1 = SiteUser.createNew(user1.id.get, site1.id.get)
          val user2 = StoreUser.create(
            "userName2", "firstName2", Some("middleName2"), "lastName2", "email2",
            1L, 2L, UserRole.ADMIN, Some("companyName2")
          )
          val siteUser2 = SiteUser.createNew(user2.id.get, site2.id.get)

          val header = TransactionLogHeader.createNew(
            user1.id.get, currency1.id,
            BigDecimal(234), BigDecimal(345),
            TransactionType.NORMAL
          )

          val tranSite1 = TransactionLogSite.createNew(
            header.id.get, site1.id.get, BigDecimal(234), BigDecimal(345)
          )

          val tranSite2 = TransactionLogSite.createNew(
            header.id.get, site2.id.get, BigDecimal(345), BigDecimal(456)
          )

          val ship1 = TransactionShipStatus.createNew(
            tranSite1.id.get, TransactionStatus.ORDERED, System.currentTimeMillis, None
          )
          val ship2 = TransactionShipStatus.createNew(
            tranSite2.id.get, TransactionStatus.ORDERED, System.currentTimeMillis, None
          )

          TransactionShipStatus.byId(ship1.id.get).status === TransactionStatus.ORDERED
          TransactionShipStatus.byId(ship2.id.get).status === TransactionStatus.ORDERED

          TransactionShipStatus.update(
            Some(siteUser1), tranSite2.id.get, TransactionStatus.CANCELED
          )

          TransactionShipStatus.byId(ship1.id.get).status === TransactionStatus.ORDERED
          TransactionShipStatus.byId(ship2.id.get).status === TransactionStatus.ORDERED

          TransactionShipStatus.update(
            Some(siteUser1), tranSite1.id.get, TransactionStatus.CANCELED
          )

          TransactionShipStatus.byId(ship1.id.get).status === TransactionStatus.CANCELED
          TransactionShipStatus.byId(ship2.id.get).status === TransactionStatus.ORDERED
        }
      }
    }

    "Can retrieve transaction item." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          TestHelper.removePreloadedRecords()

          val currency1 = CurrencyInfo.Jpy
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )
          val siteUser1 = SiteUser.createNew(user1.id.get, site1.id.get)

          val header = TransactionLogHeader.createNew(
            user1.id.get, currency1.id,
            BigDecimal(234), BigDecimal(345),
            TransactionType.NORMAL
          )

          val tranSite1 = TransactionLogSite.createNew(
            header.id.get, site1.id.get, BigDecimal(234), BigDecimal(345)
          )

          import models.LocaleInfo.{Ja}
          val tax1 = Tax.createNew
          val cat1 = Category.createNew(Map(Ja -> "植木"))
          val item1 = Item.createNew(cat1)
          val item2 = Item.createNew(cat1)

          ItemNumericMetadata.createNew(item1, ItemNumericMetadataType.HEIGHT, 10L)
          ItemNumericMetadata.createNew(item2, ItemNumericMetadataType.HEIGHT, 20L)

          SiteItemNumericMetadata.createNew(
            site1.id.get, item1.id.get, SiteItemNumericMetadataType.STOCK, 123L
          )
          SiteItemNumericMetadata.createNew(
            site1.id.get, item2.id.get, SiteItemNumericMetadataType.STOCK, 234L
          )
          SiteItemNumericMetadata.createNew(
            site1.id.get, item1.id.get, SiteItemNumericMetadataType.SHIPPING_SIZE, 0L
          )
          SiteItemNumericMetadata.createNew(
            site1.id.get, item2.id.get, SiteItemNumericMetadataType.PROMOTION, 1L
          )

          val name1 = ItemName.createNew(item1, Map(Ja -> "杉"))
          val name2 = ItemName.createNew(item2, Map(Ja -> "梅"))
          
          val price1 = ItemPrice.createNew(item1, site1)
          val price2 = ItemPrice.createNew(item2, site1)

          val ph1 = ItemPriceHistory.createNew(price1, tax1, CurrencyInfo.Jpy, BigDecimal(119), BigDecimal(100), date("9999-12-31"))
          val ph2 = ItemPriceHistory.createNew(price2, tax1, CurrencyInfo.Jpy, BigDecimal(59), BigDecimal(50), date("9999-12-31"))

          val tranItem1 = TransactionLogItem.createNew(
            tranSite1.id.get, item1.id.get.id, price1.id.get, 3, BigDecimal(400 * 3), BigDecimal(300), 123L
          )
          val tranItem2 = TransactionLogItem.createNew(
            tranSite1.id.get, item2.id.get.id, price2.id.get, 5, BigDecimal(700 * 5), BigDecimal(400), 234L
          )

          val detail = TransactionDetail.show(tranSite1.id.get, Ja, Some(siteUser1))
          detail.size === 2
          detail(0).itemName === "杉"
          detail(0).unitPrice === BigDecimal(400)
          detail(0).costUnitPrice === BigDecimal(300)
          detail(0).quantity === 3
          detail(0).itemNumericMetadata.size === 1
          detail(0).itemNumericMetadata(ItemNumericMetadataType.HEIGHT).metadata === 10L
          detail(0).siteItemNumericMetadata.size == 2
          detail(0).siteItemNumericMetadata(SiteItemNumericMetadataType.STOCK).metadata === 123L
          detail(0).siteItemNumericMetadata(SiteItemNumericMetadataType.SHIPPING_SIZE).metadata === 0L

          detail(1).itemName === "梅"
          detail(1).unitPrice === BigDecimal(700)
          detail(1).costUnitPrice === BigDecimal(400)
          detail(1).quantity === 5
          detail(1).itemNumericMetadata.size === 1
          detail(1).itemNumericMetadata(ItemNumericMetadataType.HEIGHT).metadata === 20L
          detail(1).siteItemNumericMetadata.size == 2
          detail(1).siteItemNumericMetadata(SiteItemNumericMetadataType.STOCK).metadata === 234L
          detail(1).siteItemNumericMetadata(SiteItemNumericMetadataType.PROMOTION).metadata === 1L

          val site2 = Site.createNew(LocaleInfo.Ja, "商店2")
          val user2 = StoreUser.create(
            "userName2", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )
          val siteUser2 = SiteUser.createNew(user2.id.get, site2.id.get)

          // Other site owner cannot see other owners' transaction.
          TransactionDetail.show(tranSite1.id.get, Ja, Some(siteUser2)).size === 0

          // Super user can see all transaction.
          val detail2 = TransactionDetail.show(tranSite1.id.get, Ja, None)
          detail2.size === 2
          detail2(0).itemName === "杉"
          detail2(0).unitPrice === BigDecimal(400)
          detail2(0).quantity === 3

          detail2(1).itemName === "梅"
          detail2(1).unitPrice === BigDecimal(700)
          detail2(1).quantity === 5
        }
      }
    }

    "Can list shipping log by site." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          TestHelper.removePreloadedRecords()
          val currency1 = CurrencyInfo.Jpy
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")

          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )
          val siteUser1 = SiteUser.createNew(user1.id.get, site1.id.get)

          val header = TransactionLogHeader.createNew(
            user1.id.get, currency1.id,
            BigDecimal(234), BigDecimal(345),
            TransactionType.NORMAL
          )

          val tranSite1 = TransactionLogSite.createNew(
            header.id.get, site1.id.get, BigDecimal(234), BigDecimal(345)
          )

          val addr1 = Address.createNew(
            countryCode = CountryCode.JPN,
            firstName = "firstName1",
            lastName = "lastName1",
            zip1 = "zip1",
            zip2 = "zip2",
            prefecture = JapanPrefecture.東京都,
            address1 = "address1-1",
            address2 = "address1-2",
            tel1 = "tel1-1",
            comment = "comment1"
          )

          val tax = Tax.createNew

          val shipping1 = TransactionLogShipping.createNew(
            transactionSiteId = tranSite1.id.get,
            amount = BigDecimal(1000),
            addressId = addr1.id.get,
            itemClass = 1L,
            boxSize = 3,
            taxId = tax.id.get,
            boxCount = 1,
            boxName = "boxName",
            shippingDate = date("2012-12-31")
          )

          val shipping2 = TransactionLogShipping.createNew(
            transactionSiteId = tranSite1.id.get,
            amount = BigDecimal(2000),
            addressId = addr1.id.get,
            itemClass = 2L,
            boxSize = 5,
            taxId = tax.id.get,
            boxCount = 2,
            boxName = "boxName2",
            shippingDate = date("2011-12-31")
          )

          val list = TransactionLogShipping.listBySite(tranSite1.id.get)
          list.size === 2
          list(0) === shipping1
          list(1) === shipping2
        }
      }
    }
  }
}

