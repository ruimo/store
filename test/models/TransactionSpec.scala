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
import java.sql.Date.{valueOf => date}
import collection.immutable.LongMap

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
            1L, 2L, UserRole.ADMIN
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
            1L, 2L, UserRole.ADMIN
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
            tranSite.id.get, BigDecimal(9876), addr1.id.get, 1L, 1, 1L
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
            1L, 2L, UserRole.ADMIN
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
            1L, 2L, UserRole.ADMIN
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
            tranSite.id.get, item.id.get, 1234L, 234L, BigDecimal(456)
          )

          val list = TransactionLogItem.list()
          list.size === 1
          list(0) === itemLog
          list(0).itemId === item.id.get
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
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL
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

          val ph1 = ItemPriceHistory.createNew(price1, tax1, CurrencyInfo.Jpy, BigDecimal(119), date("9999-12-31"))
          val ph2 = ItemPriceHistory.createNew(price2, tax1, CurrencyInfo.Jpy, BigDecimal(59), date("9999-12-31"))

          val cart1 = ShoppingCartItem.addItem(user1.id.get, site1.id.get, item1.id.get, 1)
          val cart2 = ShoppingCartItem.addItem(user1.id.get, site2.id.get, item2.id.get, 1)

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

          val persister = new TransactionPersister
          val tranNo = persister.persist(
            Transaction(user1.id.get, CurrencyInfo.Jpy, cart, addr, 
                        controllers.Shipping.shippingFee(addr, cart))
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

          ptran.shippingTable(site2.id.get).size === 1
          val tranShipping2 = ptran.shippingTable(site2.id.get).head
          tranShipping2.amount === 2345
          tranShipping2.addressId === addr.id.get
          tranShipping2.itemClass === itemClass1
          tranShipping2.boxSize === 3
          tranShipping2.taxId === tax2.id.get

          ptran.taxTable.size === 2
          val taxTable1 = ptran.taxTable(site1.id.get).foldLeft(LongMap[TransactionLogTax]()) {
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

          val taxTable2 = ptran.taxTable(site2.id.get).foldLeft(LongMap[TransactionLogTax]()) {
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
          val itemTable1 = ptran.itemTable(site1.id.get).foldLeft(LongMap[(ItemName, TransactionLogItem)]()) {
            (map, e) => map.updated(e._1.itemId, e)
          }
          itemTable1(item1.id.get)._1.name === "杉"
          itemTable1(item1.id.get)._2.itemId === item1.id.get
          itemTable1(item1.id.get)._2.itemPriceHistoryId === ph1.id.get
          itemTable1(item1.id.get)._2.quantity === 1
          itemTable1(item1.id.get)._2.amount === BigDecimal(119)
          
          val itemTable2 = ptran.itemTable(site2.id.get).foldLeft(LongMap[(ItemName, TransactionLogItem)]()) {
            (map, e) => map.updated(e._1.itemId, e)
          }
          itemTable2(item2.id.get)._1.name === "梅"
          itemTable2(item2.id.get)._2.itemId === item2.id.get
          itemTable2(item2.id.get)._2.itemPriceHistoryId === ph2.id.get
          itemTable2(item2.id.get)._2.quantity === 1
          itemTable2(item2.id.get)._2.amount === BigDecimal(59)

          val siteUser1 = SiteUser.createNew(user1.id.get, site1.id.get)
          val summary1 = TransactionSummary.list(Some(siteUser1))
          summary1.size === 1
          val entry1 = summary1.head
          entry1.transactionId === tranNo
          entry1.transactionTime === ptran.header.transactionTime
          entry1.totalAmount === BigDecimal(119 + 1234)
          entry1.address === addr
          entry1.siteName === "商店1"
          entry1.outerTax === BigDecimal(119 * 5 / 100)
          entry1.shippingFee === BigDecimal(1234)
          entry1.status === TransactionStatus.ORDERED

          val siteUser2 = SiteUser.createNew(user1.id.get, site2.id.get)
          val summary2 = TransactionSummary.list(Some(siteUser2))
          summary2.size === 1
          val entry2 = summary2.head
          entry2.transactionId === tranNo
          entry2.transactionTime === ptran.header.transactionTime
          entry2.totalAmount === BigDecimal(59 + 2345)
          entry2.address === addr
          entry2.siteName === "商店2"
          entry2.outerTax === BigDecimal(59 * 5 / 100)
          entry2.shippingFee === BigDecimal(2345)
          entry2.status === TransactionStatus.ORDERED
        }
      }      
    }
  }
}

