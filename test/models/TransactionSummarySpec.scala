package models

import org.specs2.mutable.Specification
import play.api.test.FakeApplication
import play.api.db.DB
import play.api.test._
import java.sql.Date.{valueOf => date}
import play.api.test.Helpers._
import play.api.Play.current
import com.ruimo.scoins.Scoping._

class TransactionSummarySpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  "TransactionSummary" should {
    "Can list summary" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          val tax1 = Tax.createNew
          val tax2 = Tax.createNew
          TaxHistory.createNew(tax1, TaxType.OUTER_TAX, BigDecimal("5"), date("9999-12-31"))
          TaxHistory.createNew(tax2, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

          val user1 = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL, Some("companyName")
          )
          val user2 = StoreUser.create(
            "name2", "first2", None, "last2", "email2", 123L, 234L, UserRole.NORMAL, Some("companyName2")
          )
          import models.LocaleInfo.Ja
          val site1 = Site.createNew(Ja, "商店1")
          val site2 = Site.createNew(Ja, "商店2")

          val cat1 = Category.createNew(Map(Ja -> "植木"))

          val item1 = Item.createNew(cat1)
          val item2 = Item.createNew(cat1)

          ItemName.createNew(item1, Map(Ja -> "杉"))
          ItemName.createNew(item2, Map(Ja -> "梅"))

          SiteItem.createNew(site1, item1)
          SiteItem.createNew(site2, item2)

          ItemDescription.createNew(item1, site1, "杉説明")
          ItemDescription.createNew(item2, site1, "梅説明")

          val price1 = ItemPrice.createNew(item1, site1)
          val price2 = ItemPrice.createNew(item2, site2)

          ItemPriceHistory.createNew(
            price1, tax1, CurrencyInfo.Jpy, BigDecimal(119), None, BigDecimal(100), date("9999-12-31")
          )
          ItemPriceHistory.createNew(
            price2, tax1, CurrencyInfo.Jpy, BigDecimal(59), None, BigDecimal(50), date("9999-12-31")
          )

          ShoppingCartItem.addItem(user1.id.get, site1.id.get, item1.id.get.id, 1)
          ShoppingCartItem.addItem(user1.id.get, site2.id.get, item2.id.get.id, 1)

          ShoppingCartItem.addItem(user2.id.get, site1.id.get, item1.id.get.id, 2)

          val itemClass1 = 1L

          val box1 = ShippingBox.createNew(site1.id.get, itemClass1, 10, "小箱")
          val box2 = ShippingBox.createNew(site2.id.get, itemClass1, 3, "小箱")
          val shipping1 = ShippingFee.createNew(box1.id.get, CountryCode.JPN, JapanPrefecture.東京都.code)
          val shipping2 = ShippingFee.createNew(box2.id.get, CountryCode.JPN, JapanPrefecture.東京都.code)
          ShippingFeeHistory.createNew(
            shipping1.id.get, tax2.id.get, BigDecimal(1234), date("9999-12-31")
          )
          ShippingFeeHistory.createNew(
            shipping2.id.get, tax2.id.get, BigDecimal(2345), date("9999-12-31")
          )

          ShippingFeeHistory.feeBySiteAndItemClass(
            CountryCode.JPN, JapanPrefecture.東京都.code,
            ShippingFeeEntries()
              .add(site1, itemClass1, 3)
              .add(site2, itemClass1, 5)
          )

          val cart1 = ShoppingCartItem.listItemsForUser(Ja, user1.id.get)
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

          val cart2 = ShoppingCartItem.listItemsForUser(Ja, user2.id.get)
          val addr2 = Address.createNew(
            countryCode = CountryCode.JPN,
            firstName = "FirstName2",
            lastName = "LastName2",
            zip1 = "123",
            prefecture = JapanPrefecture.東京都,
            address1 = "Address21",
            address2 = "Address22",
            tel1 = "1234567890"
          )

          val shippingDate1 = ShippingDate(
            Map(
              site1.id.get -> ShippingDateEntry(site1.id.get, date("2013-02-03")),
              site2.id.get -> ShippingDateEntry(site2.id.get, date("2013-05-03"))
            )
          )
          val shippingDate2 = ShippingDate(
            Map(
              site1.id.get -> ShippingDateEntry(site1.id.get, date("2013-02-04"))
            )
          )
          val persister = new TransactionPersister
          val tranNo1 = persister.persist(
            Transaction(user1.id.get, CurrencyInfo.Jpy, cart1, Some(addr1),
                        controllers.Shipping.shippingFee(addr1, cart1), shippingDate1)
          )

          val ptran1 = persister.load(tranNo1, Ja)
          val siteUser1 = SiteUser.createNew(user1.id.get, site1.id.get)
          val summary1 = TransactionSummary.list(Some(siteUser1.siteId)).records
          summary1.size === 1
          val entry1 = summary1.head
          entry1.transactionId === tranNo1
          entry1.transactionTime === ptran1.header.transactionTime
          entry1.totalAmount === BigDecimal(119 + 1234)
          entry1.address === Some(addr1)
          entry1.siteName === "商店1"
          entry1.shippingFee === BigDecimal(1234)
          entry1.status === TransactionStatus.ORDERED

          val sum1 = TransactionSummary.get(Some(siteUser1.siteId), entry1.transactionSiteId)
          sum1.isDefined === true

          val tranNo2 = persister.persist(
            Transaction(user2.id.get, CurrencyInfo.Jpy, cart2, Some(addr2),
                        controllers.Shipping.shippingFee(addr2, cart2), shippingDate2)
          )

          val ptran2 = persister.load(tranNo2, Ja)
          val siteUser2 = SiteUser.createNew(user1.id.get, site2.id.get)
          doWith(TransactionSummary.list(Some(siteUser1.siteId)).records) { s =>
            s.size === 2
            doWith(s(0)) { e =>
              e.transactionId === tranNo2
              e.transactionTime === ptran2.header.transactionTime
              e.totalAmount === BigDecimal(119 * 2 + 1234)
              e.address === Some(addr2)
              e.siteName === "商店1"
              e.shippingFee === BigDecimal(1234)
              e.status === TransactionStatus.ORDERED
            }

            doWith(s(1)) { e =>
              e.transactionId === tranNo1
              e.transactionTime === ptran1.header.transactionTime
              e.totalAmount === BigDecimal(119 + 1234)
              e.address === Some(addr1)
              e.siteName === "商店1"
              e.shippingFee === BigDecimal(1234)
              e.status === TransactionStatus.ORDERED
            }
          }

          doWith(TransactionSummary.list(Some(siteUser2.siteId)).records) { s =>
            s.size === 1
            doWith(s(0)) { e =>
              e.transactionId === tranNo1
              e.transactionTime === ptran1.header.transactionTime
              e.totalAmount === BigDecimal(59 + 2345)
              e.address === Some(addr1)
              e.siteName === "商店2"
              e.shippingFee === BigDecimal(2345)
              e.status === TransactionStatus.ORDERED
            }
          }

          doWith(TransactionSummary.list(storeUserId = Some(user1.id.get)).records) { s =>
            s.size === 2
            doWith(s.map { ele => (ele.siteName, ele) }.toMap) { map =>
              doWith(map("商店1")) { e =>
                e.transactionId === tranNo1
                e.transactionTime === ptran1.header.transactionTime
                e.totalAmount === BigDecimal(119 + 1234)
                e.address === Some(addr1)
                e.siteName === "商店1"
                e.shippingFee === BigDecimal(1234)
                e.status === TransactionStatus.ORDERED
              }

              doWith(map("商店2")) { e =>
                e.transactionId === tranNo1
                e.transactionTime === ptran1.header.transactionTime
                e.totalAmount === BigDecimal(59 + 2345)
                e.address === Some(addr1)
                e.siteName === "商店2"
                e.shippingFee === BigDecimal(2345)
                e.status === TransactionStatus.ORDERED
              }
            }
          }

          doWith(TransactionSummary.list(storeUserId = Some(user2.id.get)).records) { s =>
            s.size === 1
            doWith(s(0)) { e =>
              e.transactionId === tranNo2
              e.transactionTime === ptran2.header.transactionTime
              e.totalAmount === BigDecimal(119 * 2 + 1234)
              e.address === Some(addr2)
              e.siteName === "商店1"
              e.shippingFee === BigDecimal(1234)
              e.status === TransactionStatus.ORDERED
            }
          }

          doWith(TransactionSummary.list(tranId = Some(tranNo1)).records) { s =>
            s.size === 2
            doWith(s.map { ele => (ele.siteName, ele) }.toMap) { map =>
              doWith(map("商店1")) { e =>
                e.transactionId === tranNo1
                e.transactionTime === ptran1.header.transactionTime
                e.totalAmount === BigDecimal(119 + 1234)
                e.address === Some(addr1)
                e.siteName === "商店1"
                e.shippingFee === BigDecimal(1234)
                e.status === TransactionStatus.ORDERED
              }

              doWith(map("商店2")) { e =>
                e.transactionId === tranNo1
                e.transactionTime === ptran1.header.transactionTime
                e.totalAmount === BigDecimal(59 + 2345)
                e.address === Some(addr1)
                e.siteName === "商店2"
                e.shippingFee === BigDecimal(2345)
                e.status === TransactionStatus.ORDERED
              }
            }
          }

          doWith(TransactionSummary.list(tranId = Some(tranNo2)).records) { s =>
            s.size === 1
            doWith(s(0)) { e =>
              e.transactionId === tranNo2
              e.transactionTime === ptran2.header.transactionTime
              e.totalAmount === BigDecimal(119 * 2 + 1234)
              e.address === Some(addr2)
              e.siteName === "商店1"
              e.shippingFee === BigDecimal(1234)
              e.status === TransactionStatus.ORDERED
            }
          }
        }
      }
    }

    "Can listByPeriod summary" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          val tax1 = Tax.createNew
          val tax2 = Tax.createNew
          TaxHistory.createNew(tax1, TaxType.OUTER_TAX, BigDecimal("5"), date("9999-12-31"))
          TaxHistory.createNew(tax2, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

          val user1 = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL, Some("companyName")
          )
          val user2 = StoreUser.create(
            "name2", "first2", None, "last2", "email2", 123L, 234L, UserRole.NORMAL, Some("companyName2")
          )
          import models.LocaleInfo.Ja
          val site1 = Site.createNew(Ja, "商店1")
          val site2 = Site.createNew(Ja, "商店2")

          val cat1 = Category.createNew(Map(Ja -> "植木"))

          val item1 = Item.createNew(cat1)
          val item2 = Item.createNew(cat1)

          ItemName.createNew(item1, Map(Ja -> "杉"))
          ItemName.createNew(item2, Map(Ja -> "梅"))

          SiteItem.createNew(site1, item1)
          SiteItem.createNew(site2, item2)

          ItemDescription.createNew(item1, site1, "杉説明")
          ItemDescription.createNew(item2, site1, "梅説明")

          val price1 = ItemPrice.createNew(item1, site1)
          val price2 = ItemPrice.createNew(item2, site2)

          ItemPriceHistory.createNew(
            price1, tax1, CurrencyInfo.Jpy, BigDecimal(119), None, BigDecimal(100), date("9999-12-31")
          )
          ItemPriceHistory.createNew(
            price2, tax1, CurrencyInfo.Jpy, BigDecimal(59), None, BigDecimal(50), date("9999-12-31")
          )

          ShoppingCartItem.addItem(user1.id.get, site1.id.get, item1.id.get.id, 1)
          ShoppingCartItem.addItem(user1.id.get, site2.id.get, item2.id.get.id, 1)

          ShoppingCartItem.addItem(user2.id.get, site1.id.get, item1.id.get.id, 2)

          val itemClass1 = 1L

          val box1 = ShippingBox.createNew(site1.id.get, itemClass1, 10, "小箱")
          val box2 = ShippingBox.createNew(site2.id.get, itemClass1, 3, "小箱")
          val shipping1 = ShippingFee.createNew(box1.id.get, CountryCode.JPN, JapanPrefecture.東京都.code)
          val shipping2 = ShippingFee.createNew(box2.id.get, CountryCode.JPN, JapanPrefecture.東京都.code)
          ShippingFeeHistory.createNew(
            shipping1.id.get, tax2.id.get, BigDecimal(1234), date("9999-12-31")
          )
          ShippingFeeHistory.createNew(
            shipping2.id.get, tax2.id.get, BigDecimal(2345), date("9999-12-31")
          )

          ShippingFeeHistory.feeBySiteAndItemClass(
            CountryCode.JPN, JapanPrefecture.東京都.code,
            ShippingFeeEntries()
              .add(site1, itemClass1, 3)
              .add(site2, itemClass1, 5)
          )

          val cart1 = ShoppingCartItem.listItemsForUser(Ja, user1.id.get)
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

          val cart2 = ShoppingCartItem.listItemsForUser(Ja, user2.id.get)
          val addr2 = Address.createNew(
            countryCode = CountryCode.JPN,
            firstName = "FirstName2",
            lastName = "LastName2",
            zip1 = "123",
            prefecture = JapanPrefecture.東京都,
            address1 = "Address21",
            address2 = "Address22",
            tel1 = "1234567890"
          )

          val shippingDate1 = ShippingDate(
            Map(
              site1.id.get -> ShippingDateEntry(site1.id.get, date("2013-02-03")),
              site2.id.get -> ShippingDateEntry(site2.id.get, date("2013-05-03"))
            )
          )
          val shippingDate2 = ShippingDate(
            Map(
              site1.id.get -> ShippingDateEntry(site1.id.get, date("2013-02-04"))
            )
          )
          val persister = new TransactionPersister
          val tranNo1 = persister.persist(
            Transaction(
              user1.id.get, CurrencyInfo.Jpy, cart1, Some(addr1),
              controllers.Shipping.shippingFee(addr1, cart1), shippingDate1,
              now = date("2013-01-31")
            )
          )

          val tranNo2 = persister.persist(
            Transaction(
              user2.id.get, CurrencyInfo.Jpy, cart2, Some(addr2),
              controllers.Shipping.shippingFee(addr2, cart2), shippingDate2,
              now = date("2013-03-01")
            )
          )

          val ptran1 = persister.load(tranNo1, Ja)
          val ptran2 = persister.load(tranNo2, Ja)
          val siteUser1 = SiteUser.createNew(user1.id.get, site1.id.get)
          val siteUser2 = SiteUser.createNew(user1.id.get, site2.id.get)
          doWith(TransactionSummary.listByPeriod(siteId = Some(siteUser1.siteId), yearMonth = YearMonth(2013, 1))) { s =>
            s.size === 1
            doWith(s(0)) { e =>
              e.transactionId === tranNo1
              e.transactionTime === ptran1.header.transactionTime
              e.totalAmount === BigDecimal(119 + 1234)
              e.address === Some(addr1)
              e.siteName === "商店1"
              e.shippingFee === BigDecimal(1234)
              e.status === TransactionStatus.ORDERED
            }
          }

          doWith(TransactionSummary.listByPeriod(siteId = Some(siteUser1.siteId), yearMonth = YearMonth(2013, 3))) { s =>
            s.size === 1
            doWith(s(0)) { e =>
              e.transactionId === tranNo2
              e.transactionTime === ptran2.header.transactionTime
              e.totalAmount === BigDecimal(119 * 2 + 1234)
              e.address === Some(addr2)
              e.siteName === "商店1"
              e.shippingFee === BigDecimal(1234)
              e.status === TransactionStatus.ORDERED
            }
          }
        }
      }
    }
  }
}
