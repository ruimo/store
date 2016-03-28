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
import java.sql.Timestamp.{valueOf => timestamp}
import com.ruimo.scoins.Scoping._
import scala.collection.immutable

class ShoppingCartSpec extends Specification {
  implicit def date2milli(d: java.sql.Date): Long = d.getTime
  implicit def timestamp2milli(d: java.sql.Timestamp): Long = d.getTime

  "ShoppingCart" should {
    "addItem will assign sequence number." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          TestHelper.removePreloadedRecords()

          val tax = Tax.createNew
          val taxHistory = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

          val user1 = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL, Some("companyName")
          )
          val user2 = StoreUser.create(
            "name2", "first2", None, "last2", "email2", 987L, 765L, UserRole.NORMAL, None
          )

          ShoppingCartItem.quantityForUser(user1.id.get) === 0
          ShoppingCartItem.quantityForUser(user2.id.get) === 0

          import models.LocaleInfo.{Ja, En}
          val site1 = Site.createNew(Ja, "商店1")
          val site2 = Site.createNew(Ja, "商店2")

          val cat1 = Category.createNew(Map(Ja -> "植木", En -> "Plant"))
          val cat2 = Category.createNew(Map(Ja -> "果樹", En -> "Fruit"))

          val item1 = Item.createNew(cat1)
          val item2 = Item.createNew(cat2)

          val name1 = ItemName.createNew(item1, Map(Ja -> "杉", En -> "Cedar"))
          val name2 = ItemName.createNew(item2, Map(Ja -> "梅", En -> "Ume"))

          SiteItem.createNew(site1, item1)
          SiteItem.createNew(site2, item2)

          val desc1 = ItemDescription.createNew(item1, site1, "杉説明")
          val desc2 = ItemDescription.createNew(item2, site2, "梅説明")

          val price1 = ItemPrice.createNew(item1, site1)
          val price2 = ItemPrice.createNew(item2, site2)

          val ph1 = ItemPriceHistory.createNew(
            price1, tax, CurrencyInfo.Jpy, BigDecimal(100), None, BigDecimal(90), date("2013-01-02")
          )
          val ph2 = ItemPriceHistory.createNew(
            price1, tax, CurrencyInfo.Jpy, BigDecimal(101), None, BigDecimal(100), date("9999-12-31")
          )

          val ph3 = ItemPriceHistory.createNew(
            price2, tax, CurrencyInfo.Jpy, BigDecimal(300), None, BigDecimal(290), date("2013-01-03")
          )
          val ph4 = ItemPriceHistory.createNew(
            price2, tax, CurrencyInfo.Jpy, BigDecimal(301), None, BigDecimal(300), date("9999-12-31")
          )

          val cart1 = ShoppingCartItem.addItem(user1.id.get, site1.id.get, item1.id.get.id, 2)
          val cart2 = ShoppingCartItem.addItem(user1.id.get, site2.id.get, item2.id.get.id, 3)

          val cart3 = ShoppingCartItem.addItem(user2.id.get, site1.id.get, item1.id.get.id, 10)

          cart1.storeUserId === user1.id.get
          cart1.sequenceNumber === 1
          cart1.itemId === item1.id.get.id
          cart1.quantity === 2

          cart2.storeUserId === user1.id.get
          cart2.sequenceNumber === 2
          cart2.itemId === item2.id.get.id
          cart2.quantity === 3

          cart3.storeUserId === user2.id.get
          cart3.sequenceNumber === 1
          cart3.itemId === item1.id.get.id
          cart3.quantity === 10

          val time = date("2013-01-04").getTime
          val list1 = ShoppingCartItem.listItemsForUser(
            Ja, 
            LoginSession(user1, None, 0),
            0, 10, time
          )._1
          list1.size === 2
          list1(0).shoppingCartItem === cart1
          list1(0).itemName === name1(Ja)
          list1(0).itemDescription === desc1
          list1(0).site === site1
          list1(0).itemPriceHistory === ph2

          list1(1).shoppingCartItem === cart2
          list1(1).itemName === name2(Ja)
          list1(1).itemDescription === desc2
          list1(1).site === site2
          list1(1).itemPriceHistory === ph4

          val time2 = date("2013-01-01").getTime
          val list2 = ShoppingCartItem.listItemsForUser(
            Ja,
            LoginSession(user2, None, 0),
            0, 10, time2
          )._1
          list2.size === 1
          list2(0).shoppingCartItem === cart3
          list2(0).itemName === name1(Ja)
          list2(0).itemDescription === desc1
          list2(0).site === site1
          list2(0).itemPriceHistory === ph1

          ShoppingCartItem.isAllCoupon(user1.id.get) === false
          ShoppingCartItem.isAllCoupon(user2.id.get) === false

          ShoppingCartItem.quantityForUser(user1.id.get) === 5
          ShoppingCartItem.quantityForUser(user2.id.get) === 10
        }}
      }
    }

    "Only coupon." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          TestHelper.removePreloadedRecords()

          val tax = Tax.createNew
          val taxHistory = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

          val user1 = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL, Some("companyName")
          )

          import models.LocaleInfo.{Ja, En}
          val site1 = Site.createNew(Ja, "商店1")
          val site2 = Site.createNew(Ja, "商店2")

          val cat1 = Category.createNew(Map(Ja -> "植木", En -> "Plant"))
          val cat2 = Category.createNew(Map(Ja -> "果樹", En -> "Fruit"))

          val item1 = Item.createNew(cat1)
          val item2 = Item.createNew(cat2)

          Coupon.updateAsCoupon(item1.id.get)
          Coupon.updateAsCoupon(item2.id.get)

          val name1 = ItemName.createNew(item1, Map(Ja -> "杉", En -> "Cedar"))
          val name2 = ItemName.createNew(item2, Map(Ja -> "梅", En -> "Ume"))

          SiteItem.createNew(site1, item1)
          SiteItem.createNew(site2, item2)

          val desc1 = ItemDescription.createNew(item1, site1, "杉説明")
          val desc2 = ItemDescription.createNew(item2, site2, "梅説明")

          val price1 = ItemPrice.createNew(item1, site1)
          val price2 = ItemPrice.createNew(item2, site2)

          val ph1 = ItemPriceHistory.createNew(
            price1, tax, CurrencyInfo.Jpy, BigDecimal(100), None, BigDecimal(90), date("2013-01-02")
          )
          val ph2 = ItemPriceHistory.createNew(
            price1, tax, CurrencyInfo.Jpy, BigDecimal(101), None, BigDecimal(100), date("9999-12-31")
          )

          val ph3 = ItemPriceHistory.createNew(
            price2, tax, CurrencyInfo.Jpy, BigDecimal(300), None, BigDecimal(290), date("2013-01-03")
          )
          val ph4 = ItemPriceHistory.createNew(
            price2, tax, CurrencyInfo.Jpy, BigDecimal(301), None, BigDecimal(300), date("9999-12-31")
          )

          val cart1 = ShoppingCartItem.addItem(user1.id.get, site1.id.get, item1.id.get.id, 2)
          val cart2 = ShoppingCartItem.addItem(user1.id.get, site2.id.get, item2.id.get.id, 3)

          cart1.storeUserId === user1.id.get
          cart1.sequenceNumber === 1
          cart1.itemId === item1.id.get.id
          cart1.quantity === 2

          cart2.storeUserId === user1.id.get
          cart2.sequenceNumber === 2
          cart2.itemId === item2.id.get.id
          cart2.quantity === 3

          val time = date("2013-01-04").getTime
          val list1 = ShoppingCartItem.listItemsForUser(
            Ja,
            LoginSession(user1, None, 0),
            0, 10, time
          )._1
          list1.size === 2
          list1(0).shoppingCartItem === cart1
          list1(0).itemName === name1(Ja)
          list1(0).itemDescription === desc1
          list1(0).site === site1
          list1(0).itemPriceHistory === ph2

          list1(1).shoppingCartItem === cart2
          list1(1).itemName === name2(Ja)
          list1(1).itemDescription === desc2
          list1(1).site === site2
          list1(1).itemPriceHistory === ph4

          ShoppingCartItem.isAllCoupon(user1.id.get) === true
        }}
      }
    }

    "Coupon and non coupon." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          TestHelper.removePreloadedRecords()

          val tax = Tax.createNew
          val taxHistory = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

          val user1 = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL, Some("companyName")
          )

          import models.LocaleInfo.{Ja, En}
          val site1 = Site.createNew(Ja, "商店1")
          val site2 = Site.createNew(Ja, "商店2")

          val cat1 = Category.createNew(Map(Ja -> "植木", En -> "Plant"))
          val cat2 = Category.createNew(Map(Ja -> "果樹", En -> "Fruit"))

          val item1 = Item.createNew(cat1)
          val item2 = Item.createNew(cat2)

          Coupon.updateAsCoupon(item1.id.get)

          val name1 = ItemName.createNew(item1, Map(Ja -> "杉", En -> "Cedar"))
          val name2 = ItemName.createNew(item2, Map(Ja -> "梅", En -> "Ume"))

          SiteItem.createNew(site1, item1)
          SiteItem.createNew(site2, item2)

          val desc1 = ItemDescription.createNew(item1, site1, "杉説明")
          val desc2 = ItemDescription.createNew(item2, site2, "梅説明")

          val price1 = ItemPrice.createNew(item1, site1)
          val price2 = ItemPrice.createNew(item2, site2)

          val ph1 = ItemPriceHistory.createNew(
            price1, tax, CurrencyInfo.Jpy, BigDecimal(100), None, BigDecimal(90), date("2013-01-02")
          )
          val ph2 = ItemPriceHistory.createNew(
            price1, tax, CurrencyInfo.Jpy, BigDecimal(101), None, BigDecimal(100), date("9999-12-31")
          )

          val ph3 = ItemPriceHistory.createNew(
            price2, tax, CurrencyInfo.Jpy, BigDecimal(300), None, BigDecimal(290), date("2013-01-03")
          )
          val ph4 = ItemPriceHistory.createNew(
            price2, tax, CurrencyInfo.Jpy, BigDecimal(301), None, BigDecimal(300), date("9999-12-31")
          )

          val cart1 = ShoppingCartItem.addItem(user1.id.get, site1.id.get, item1.id.get.id, 2)
          val cart2 = ShoppingCartItem.addItem(user1.id.get, site2.id.get, item2.id.get.id, 3)

          cart1.storeUserId === user1.id.get
          cart1.sequenceNumber === 1
          cart1.itemId === item1.id.get.id
          cart1.quantity === 2

          cart2.storeUserId === user1.id.get
          cart2.sequenceNumber === 2
          cart2.itemId === item2.id.get.id
          cart2.quantity === 3

          val time = date("2013-01-04").getTime
          val list1 = ShoppingCartItem.listItemsForUser(
            Ja, 
            LoginSession(user1, None, 0),
            0, 10, time
          )._1
          list1.size === 2
          list1(0).shoppingCartItem === cart1
          list1(0).itemName === name1(Ja)
          list1(0).itemDescription === desc1
          list1(0).site === site1
          list1(0).itemPriceHistory === ph2

          list1(1).shoppingCartItem === cart2
          list1(1).itemName === name2(Ja)
          list1(1).itemDescription === desc2
          list1(1).site === site2
          list1(1).itemPriceHistory === ph4

          ShoppingCartItem.isAllCoupon(user1.id.get) === false
        }}
      }
    }

    "Coupon and non coupon. Non coupon item was coupon." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          TestHelper.removePreloadedRecords()

          val tax = Tax.createNew
          val taxHistory = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

          val user1 = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL, Some("companyName")
          )

          import models.LocaleInfo.{Ja, En}
          val site1 = Site.createNew(Ja, "商店1")
          val site2 = Site.createNew(Ja, "商店2")

          val cat1 = Category.createNew(Map(Ja -> "植木", En -> "Plant"))
          val cat2 = Category.createNew(Map(Ja -> "果樹", En -> "Fruit"))

          val item1 = Item.createNew(cat1)
          val item2 = Item.createNew(cat2)

          Coupon.updateAsCoupon(item1.id.get)
          Coupon.updateAsCoupon(item2.id.get)
          Coupon.update(item2.id.get, false)

          val name1 = ItemName.createNew(item1, Map(Ja -> "杉", En -> "Cedar"))
          val name2 = ItemName.createNew(item2, Map(Ja -> "梅", En -> "Ume"))

          SiteItem.createNew(site1, item1)
          SiteItem.createNew(site2, item2)

          val desc1 = ItemDescription.createNew(item1, site1, "杉説明")
          val desc2 = ItemDescription.createNew(item2, site2, "梅説明")

          val price1 = ItemPrice.createNew(item1, site1)
          val price2 = ItemPrice.createNew(item2, site2)

          val ph1 = ItemPriceHistory.createNew(
            price1, tax, CurrencyInfo.Jpy, BigDecimal(100), None, BigDecimal(90), date("2013-01-02")
          )
          val ph2 = ItemPriceHistory.createNew(
            price1, tax, CurrencyInfo.Jpy, BigDecimal(101), None, BigDecimal(100), date("9999-12-31")
          )

          val ph3 = ItemPriceHistory.createNew(
            price2, tax, CurrencyInfo.Jpy, BigDecimal(300), None, BigDecimal(290), date("2013-01-03")
          )
          val ph4 = ItemPriceHistory.createNew(
            price2, tax, CurrencyInfo.Jpy, BigDecimal(301), None, BigDecimal(300), date("9999-12-31")
          )

          val cart1 = ShoppingCartItem.addItem(user1.id.get, site1.id.get, item1.id.get.id, 2)
          val cart2 = ShoppingCartItem.addItem(user1.id.get, site2.id.get, item2.id.get.id, 3)

          cart1.storeUserId === user1.id.get
          cart1.sequenceNumber === 1
          cart1.itemId === item1.id.get.id
          cart1.quantity === 2

          cart2.storeUserId === user1.id.get
          cart2.sequenceNumber === 2
          cart2.itemId === item2.id.get.id
          cart2.quantity === 3

          val time = date("2013-01-04").getTime
          val list1 = ShoppingCartItem.listItemsForUser(
            Ja,
            LoginSession(user1, None, 0),
            0, 10, time
          )._1
          list1.size === 2
          list1(0).shoppingCartItem === cart1
          list1(0).itemName === name1(Ja)
          list1(0).itemDescription === desc1
          list1(0).site === site1
          list1(0).itemPriceHistory === ph2

          list1(1).shoppingCartItem === cart2
          list1(1).itemName === name2(Ja)
          list1(1).itemDescription === desc2
          list1(1).site === site2
          list1(1).itemPriceHistory === ph4

          ShoppingCartItem.isAllCoupon(user1.id.get) === false
        }}
      }
    }

    "addItem will increase quantity if same item already exists." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          TestHelper.removePreloadedRecords()

          val tax = Tax.createNew
          val taxHistory = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

          val user1 = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL, Some("companyName")
          )

          import models.LocaleInfo.{Ja, En}
          val site1 = Site.createNew(Ja, "商店1")
          val site2 = Site.createNew(Ja, "商店2")

          val cat1 = Category.createNew(Map(Ja -> "植木", En -> "Plant"))
          val cat2 = Category.createNew(Map(Ja -> "果樹", En -> "Fruit"))

          val item1 = Item.createNew(cat1)
          val item2 = Item.createNew(cat2)

          val name1 = ItemName.createNew(item1, Map(Ja -> "杉", En -> "Cedar"))
          val name2 = ItemName.createNew(item2, Map(Ja -> "梅", En -> "Ume"))

          SiteItem.createNew(site1, item1)
          SiteItem.createNew(site2, item2)

          val desc1 = ItemDescription.createNew(item1, site1, "杉説明")
          val desc2 = ItemDescription.createNew(item2, site2, "梅説明")

          val price1 = ItemPrice.createNew(item1, site1)
          val price2 = ItemPrice.createNew(item2, site2)

          val ph2 = ItemPriceHistory.createNew(
            price1, tax, CurrencyInfo.Jpy, BigDecimal(101), None, BigDecimal(90), date("9999-12-31")
          )
          val ph4 = ItemPriceHistory.createNew(
            price2, tax, CurrencyInfo.Jpy, BigDecimal(301), None, BigDecimal(290), date("9999-12-31")
          )

          val cart1 = ShoppingCartItem.addItem(user1.id.get, site1.id.get, item1.id.get.id, 2)
          val cart2 = ShoppingCartItem.addItem(user1.id.get, site2.id.get, item2.id.get.id, 10)

          cart1.storeUserId === user1.id.get
          cart1.sequenceNumber === 1
          cart1.itemId === item1.id.get.id
          cart1.quantity === 2

          cart2.storeUserId === user1.id.get
          cart2.sequenceNumber === 2
          cart2.itemId === item2.id.get.id
          cart2.quantity === 10

          val time = date("2013-01-04").getTime
          val list1 = ShoppingCartItem.listItemsForUser(
            Ja, 
            LoginSession(user1, None, 0),
            0, 10, time
          )._1
          list1.size === 2
          list1(0).shoppingCartItem === cart1
          list1(0).itemName === name1(Ja)
          list1(0).itemDescription === desc1
          list1(0).site === site1
          list1(0).itemPriceHistory === ph2

          list1(1).shoppingCartItem === cart2
          list1(1).itemName === name2(Ja)
          list1(1).itemDescription === desc2
          list1(1).site === site2
          list1(1).itemPriceHistory === ph4

          // Add same id as cart1. Will increase quantity.
          val cart3 = ShoppingCartItem.addItem(user1.id.get, site1.id.get, item1.id.get.id, 3)

          val list2 = ShoppingCartItem.listItemsForUser(
            Ja,
            LoginSession(user1, None, 0),
            0, 10, time
          )._1
          list2.size === 2
          cart3.quantity === 5
          list2(0).shoppingCartItem === cart3
          list2(0).itemName === name1(Ja)
          list2(0).itemDescription === desc1
          list2(0).site === site1
          list2(0).itemPriceHistory === ph2

          list2(1).shoppingCartItem === cart2
          list2(1).itemName === name2(Ja)
          list2(1).itemDescription === desc2
          list2(1).site === site2
          list2(1).itemPriceHistory === ph4
        }}
      }
    }

    "changeQuantity will change quantity." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          TestHelper.removePreloadedRecords()
          val tax = Tax.createNew

          val user1 = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL, Some("companyName")
          )

          import models.LocaleInfo.{Ja, En}
          val site1 = Site.createNew(Ja, "商店1")
          val cat1 = Category.createNew(Map(Ja -> "植木", En -> "Plant"))
          val item1 = Item.createNew(cat1)

          SiteItem.createNew(site1, item1)

          val cart1 = ShoppingCartItem.addItem(user1.id.get, site1.id.get, item1.id.get.id, 2)

          cart1.storeUserId === user1.id.get
          cart1.sequenceNumber === 1
          cart1.itemId === item1.id.get.id
          cart1.quantity === 2

          ShoppingCartItem.changeQuantity(cart1.id.get, user1.id.get, 5)
          ShoppingCartItem(cart1.id.get).quantity === 5
        }}
      }
    }
    
    "Tax amount equals zero if shopping cart is empty." in {
      ShoppingCartTotal(List()).taxAmount === BigDecimal(0)
    }

    "Tax amount is calculated for outer tax items." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          TestHelper.removePreloadedRecords()

          val tax1 = Tax.createNew
          val taxHistory = TaxHistory.createNew(tax1, TaxType.OUTER_TAX, BigDecimal("5"), date("9999-12-31"))

          val user1 = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL, Some("companyName")
          )
          import models.LocaleInfo.{Ja}
          val site1 = Site.createNew(Ja, "商店1")

          val cat1 = Category.createNew(Map(Ja -> "植木"))

          val item1 = Item.createNew(cat1)
          val item2 = Item.createNew(cat1)

          val name1 = ItemName.createNew(item1, Map(Ja -> "杉"))
          val name2 = ItemName.createNew(item2, Map(Ja -> "梅"))

          SiteItem.createNew(site1, item1)
          SiteItem.createNew(site1, item2)

          val desc1 = ItemDescription.createNew(item1, site1, "杉説明")
          val desc2 = ItemDescription.createNew(item2, site1, "梅説明")

          val price1 = ItemPrice.createNew(item1, site1)
          val price2 = ItemPrice.createNew(item2, site1)

          val ph1 = ItemPriceHistory.createNew(
            price1, tax1, CurrencyInfo.Jpy, BigDecimal(101), None, BigDecimal(90), date("9999-12-31")
          )
          val ph2 = ItemPriceHistory.createNew(
            price2, tax1, CurrencyInfo.Jpy, BigDecimal(301), None, BigDecimal(290), date("9999-12-31")
          )

          val cart1 = ShoppingCartItem.addItem(user1.id.get, site1.id.get, item1.id.get.id, 2)
          val cart2 = ShoppingCartItem.addItem(user1.id.get, site1.id.get, item2.id.get.id, 3)

          val time = date("2013-01-04").getTime
          val list1 = ShoppingCartItem.listItemsForUser(
            Ja,
            LoginSession(user1, None, 0),
            0, 10, time
          )._1

          list1.size === 2
          list1.taxTotal === BigDecimal((101 * 2 + 301 * 3) * 5 / 100)
          list1.taxByType(TaxType.OUTER_TAX) === BigDecimal((101 * 2 + 301 * 3) * 5 / 100)
          list1.taxByType.get(TaxType.INNER_TAX) === None
          list1.taxByType.get(TaxType.NON_TAX) === None
        }}
      }
    }

    "Tax amount is calculated by tax id." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          TestHelper.removePreloadedRecords()

          val tax1 = Tax.createNew
          val tax2 = Tax.createNew
          val taxHistory1 = TaxHistory.createNew(tax1, TaxType.OUTER_TAX, BigDecimal("5"), date("9999-12-31"))
          val taxHistory2 = TaxHistory.createNew(tax2, TaxType.OUTER_TAX, BigDecimal("5"), date("9999-12-31"))

          val user1 = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL, Some("companyName")
          )
          import models.LocaleInfo.{Ja}
          val site1 = Site.createNew(Ja, "商店1")

          val cat1 = Category.createNew(Map(Ja -> "植木"))

          val item1 = Item.createNew(cat1)
          val item2 = Item.createNew(cat1)

          val name1 = ItemName.createNew(item1, Map(Ja -> "杉"))
          val name2 = ItemName.createNew(item2, Map(Ja -> "梅"))

          SiteItem.createNew(site1, item1)
          SiteItem.createNew(site1, item2)

          val desc1 = ItemDescription.createNew(item1, site1, "杉説明")
          val desc2 = ItemDescription.createNew(item2, site1, "梅説明")

          val price1 = ItemPrice.createNew(item1, site1)
          val price2 = ItemPrice.createNew(item2, site1)

          val ph1 = ItemPriceHistory.createNew(
            price1, tax1, CurrencyInfo.Jpy, BigDecimal(119), None, BigDecimal(100), date("9999-12-31")
          )
          val ph2 = ItemPriceHistory.createNew(
            price2, tax2, CurrencyInfo.Jpy, BigDecimal(59), None, BigDecimal(50), date("9999-12-31")
          )

          val cart1 = ShoppingCartItem.addItem(user1.id.get, site1.id.get, item1.id.get.id, 1)
          val cart2 = ShoppingCartItem.addItem(user1.id.get, site1.id.get, item2.id.get.id, 1)

          val time = date("2013-01-04").getTime
          val list1 = ShoppingCartItem.listItemsForUser(
            Ja,
            LoginSession(user1, None, 0),
            0, 10, time
          )._1

          list1.size === 2
          list1.taxTotal === BigDecimal((119 * 5 / 100) + (59 * 5 /100))
          list1.taxByType(TaxType.OUTER_TAX) === BigDecimal((119 * 5 / 100) + (59 * 5 /100))
          list1.taxByType.get(TaxType.INNER_TAX) === None
          list1.taxByType.get(TaxType.NON_TAX) === None
        }}
      }
    }

    "Inner tax and outer tax amount is calculated." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          TestHelper.removePreloadedRecords()

          val tax1 = Tax.createNew
          val tax2 = Tax.createNew
          val taxHistory1 = TaxHistory.createNew(tax1, TaxType.OUTER_TAX, BigDecimal("5"), date("9999-12-31"))
          val taxHistory2 = TaxHistory.createNew(tax2, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))

          val user1 = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL, Some("companyName")
          )
          import models.LocaleInfo.{Ja}
          val site1 = Site.createNew(Ja, "商店1")

          val cat1 = Category.createNew(Map(Ja -> "植木"))

          val item1 = Item.createNew(cat1)
          val item2 = Item.createNew(cat1)

          val name1 = ItemName.createNew(item1, Map(Ja -> "杉"))
          val name2 = ItemName.createNew(item2, Map(Ja -> "梅"))

          SiteItem.createNew(site1, item1)
          SiteItem.createNew(site1, item2)

          val desc1 = ItemDescription.createNew(item1, site1, "杉説明")
          val desc2 = ItemDescription.createNew(item2, site1, "梅説明")

          val price1 = ItemPrice.createNew(item1, site1)
          val price2 = ItemPrice.createNew(item2, site1)

          val ph1 = ItemPriceHistory.createNew(
            price1, tax1, CurrencyInfo.Jpy, BigDecimal(119), None, BigDecimal(100), date("9999-12-31")
          )
          val ph2 = ItemPriceHistory.createNew(
            price2, tax2, CurrencyInfo.Jpy, BigDecimal(59), None, BigDecimal(50), date("9999-12-31")
          )

          val cart1 = ShoppingCartItem.addItem(user1.id.get, site1.id.get, item1.id.get.id, 1)
          val cart2 = ShoppingCartItem.addItem(user1.id.get, site1.id.get, item2.id.get.id, 1)

          val time = date("2013-01-04").getTime
          val list1 = ShoppingCartItem.listItemsForUser(
            Ja,
            LoginSession(user1, None, 0),
            0, 10, time
          )._1

          list1.size === 2
          list1.taxTotal === BigDecimal((59 * 5 / 105) + (119 * 5 / 100))
          list1.taxByType(TaxType.OUTER_TAX) === BigDecimal(119 * 5 / 100)
          list1.taxByType(TaxType.INNER_TAX) === BigDecimal(59 * 5 /100)
          list1.taxByType.get(TaxType.NON_TAX) === None
        }}
      }
    }

    "Can calculate total by site." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          TestHelper.removePreloadedRecords()

          import LocaleInfo.{Ja, En}

          val site1 = Site.createNew(Ja, "商店1")
          val site2 = Site.createNew(Ja, "商店2")

          val cat1 = Category.createNew(Map(Ja -> "植木"))

          val item1 = Item.createNew(cat1)
          val item2 = Item.createNew(cat1)

          val name1 = ItemName.createNew(item1, Map(Ja -> "杉", En -> "Cedar"))
          val name2 = ItemName.createNew(item2, Map(Ja -> "梅", En -> "Ume"))

          SiteItem.createNew(site1, item1)
          SiteItem.createNew(site2, item2)

          val desc1 = ItemDescription.createNew(item1, site1, "杉説明")
          val desc2 = ItemDescription.createNew(item2, site1, "梅説明")
      
          val price1 = ItemPrice.createNew(item1, site1)
          val price2 = ItemPrice.createNew(item2, site1)

          val tax1 = Tax.createNew
          val ph1 = ItemPriceHistory.createNew(
            price1, tax1, CurrencyInfo.Jpy, BigDecimal(119), None, BigDecimal(100), date("9999-12-31")
          )
          val ph2 = ItemPriceHistory.createNew(
            price2, tax1, CurrencyInfo.Jpy, BigDecimal(59), None, BigDecimal(50), date("9999-12-31")
          )

          val user1 = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL, Some("companyName")
          )

          val cart1 = ShoppingCartItem.addItem(user1.id.get, site1.id.get, item1.id.get.id, 1)
          val cart2 = ShoppingCartItem.addItem(user1.id.get, site1.id.get, item2.id.get.id, 1)

          val taxHistory1 = TaxHistory.createNew(tax1, TaxType.OUTER_TAX, BigDecimal("5"), date("9999-12-31"))

          val e1 = ShoppingCartTotalEntry(
            ShoppingCartItem(
              id = None,
              storeUserId = user1.id.get,
              sequenceNumber = 1,
              siteId = site1.id.get,
              itemId = item1.id.get.id,
              quantity = 4
            ),
            name1(Ja), desc1, site1, ph1, taxHistory1, Map(), Map(), Map()
          )

          val e2 = ShoppingCartTotalEntry(
            ShoppingCartItem(
              id = None,
              storeUserId = user1.id.get,
              sequenceNumber = 2,
              siteId = site2.id.get,
              itemId = item2.id.get.id,
              quantity = 4
            ),
            name2(Ja), desc2, site2, ph2, taxHistory1, Map(), Map(), Map()
          )

          val total = ShoppingCartTotal(
            List(e1, e2)
          )

          val bySite = total.bySite
          bySite.size === 2
          bySite(site1).table.size === 1
          bySite(site1).table(0) === e1
          bySite(site2).table.size === 1
          bySite(site2).table(0) === e2
        }}
      }
    }

    "Can store shipping date." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          TestHelper.removePreloadedRecords()
          import LocaleInfo.{Ja, En}

          val site1 = Site.createNew(Ja, "商店1")
          val user1 = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL, Some("companyName")
          )

          // Insert
          ShoppingCartShipping.updateOrInsert(user1.id.get, site1.id.get, date("2013-12-31"))
          date("2013-12-31").getTime === ShoppingCartShipping.find(user1.id.get, site1.id.get)

          // Update
          ShoppingCartShipping.updateOrInsert(user1.id.get, site1.id.get, date("2013-12-30"))
          date("2013-12-30").getTime === ShoppingCartShipping.find(user1.id.get, site1.id.get)

          // Clear
          ShoppingCartShipping.clear(user1.id.get)

          SQL(
            "select count(*) from shopping_cart_shipping where store_user_id = {userId}"
          ).on(
            'userId -> user1.id.get
          ).as(
            SqlParser.scalar[Long].single
          ) === 0
        }
      }}
    }

    "Can remove expired items." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          val tax = Tax.createNew
          val taxHistory = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))
          val user1 = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL, Some("companyName")
          )
          import models.LocaleInfo.Ja
          val site1 = Site.createNew(Ja, "商店1")
          val site2 = Site.createNew(Ja, "商店2")

          val cat1 = Category.createNew(Map(Ja -> "植木"))
          val cat2 = Category.createNew(Map(Ja -> "果樹"))

          val item1 = Item.createNew(cat1)
          val item2 = Item.createNew(cat2)
          val item3 = Item.createNew(cat1)
          val item4 = Item.createNew(cat2)

          val name1 = ItemName.createNew(item1, Map(Ja -> "杉"))
          val name2 = ItemName.createNew(item2, Map(Ja -> "梅"))
          val name3 = ItemName.createNew(item3, Map(Ja -> "杉2"))
          val name4 = ItemName.createNew(item4, Map(Ja -> "梅3"))

          SiteItem.createNew(site1, item1)
          SiteItem.createNew(site2, item2)
          SiteItem.createNew(site1, item3)
          SiteItem.createNew(site2, item4)

          val desc1 = ItemDescription.createNew(item1, site1, "杉説明")
          val desc2 = ItemDescription.createNew(item2, site2, "梅説明")
          val desc3 = ItemDescription.createNew(item3, site1, "杉説明2")
          val desc4 = ItemDescription.createNew(item4, site2, "梅説明2")

          val price1 = ItemPrice.createNew(item1, site1)
          val price2 = ItemPrice.createNew(item2, site2)
          val price3 = ItemPrice.createNew(item3, site1)
          val price4 = ItemPrice.createNew(item4, site2)

          val ph1 = ItemPriceHistory.createNew(
            price1, tax, CurrencyInfo.Jpy, BigDecimal(100), None, BigDecimal(90),
            timestamp("2013-01-02 00:00:00")
          )
          val ph2 = ItemPriceHistory.createNew(
            price2, tax, CurrencyInfo.Jpy, BigDecimal(101), None, BigDecimal(100),
            timestamp("2013-01-02 00:00:01")
          )
          val ph3 = ItemPriceHistory.createNew(
            price3, tax, CurrencyInfo.Jpy, BigDecimal(102), None, BigDecimal(110),
            timestamp("2013-01-02 00:00:02")
          )
          val ph4 = ItemPriceHistory.createNew(
            price4, tax, CurrencyInfo.Jpy, BigDecimal(103), None, BigDecimal(120),
            date("9999-12-31")
          )

          val cart1 = ShoppingCartItem.addItem(user1.id.get, site1.id.get, item1.id.get.id, 1)
          val cart2 = ShoppingCartItem.addItem(user1.id.get, site2.id.get, item2.id.get.id, 1)
          val cart3 = ShoppingCartItem.addItem(user1.id.get, site1.id.get, item3.id.get.id, 1)
          val cart4 = ShoppingCartItem.addItem(user1.id.get, site2.id.get, item4.id.get.id, 1)

          doWith(
            ShoppingCartItem.listItemsForUser(
              Ja, 
              LoginSession(user1, None, 0),
              0, 100, timestamp("2013-01-02 00:00:01")
            )
          ) { t =>
            t._2.size === 2
          }
          doWith(
            ShoppingCartItem.listItemsForUser(
              Ja, 
              LoginSession(user1, None, 0),
              0, 100, timestamp("2013-01-02 00:00:00")
            )
          ) { t =>
            t._2.size === 1
          }

          ShoppingCartItem.removeExpiredItems(user1.id.get, timestamp("2013-01-02 00:00:00")) === 1
          doWith(
            ShoppingCartItem.listItemsForUser(
              Ja, 
              LoginSession(user1, None, 0),
              0, 100, timestamp("2013-01-02 00:00:00")
            )
          ) { t =>
            t._2.size === 0
            doWith(t._1.table) { total =>
              total.size === 3
              total(0).shoppingCartItem.sequenceNumber === 2
              total(1).shoppingCartItem.sequenceNumber === 3
              total(2).shoppingCartItem.sequenceNumber === 4
            }
          }

          ShoppingCartItem.removeExpiredItems(user1.id.get, timestamp("2013-01-02 00:00:02")) === 2
          doWith(
            ShoppingCartItem.listItemsForUser(
              Ja,
              LoginSession(user1, None, 0),
              0, 100, timestamp("2013-01-02 00:00:00")
            )
          ) { t =>
            t._2.size === 0
            doWith(t._1.table) { total =>
              total.size === 1
              total(0).shoppingCartItem.sequenceNumber === 4
            }
          }

          ShoppingCartItem.addItem(user1.id.get, site1.id.get, item1.id.get.id, 1)
          ShoppingCartItem.addItem(user1.id.get, site2.id.get, item2.id.get.id, 1)
          ShoppingCartItem.addItem(user1.id.get, site1.id.get, item3.id.get.id, 1)

          doWith(
            ShoppingCartItem.listItemsForUser(
              Ja,
              LoginSession(user1, None, 0),
              0, 100, timestamp("2013-01-01 23:59:59")
            )
          ) { t =>
            t._2.size === 0
            t._1.table.size === 4
          }

          // Delete item_price_history record of item4
          SQL(
            "delete from item_price_history where item_price_history_id = {id}"
          ).on(
            'id -> ph4.id.get
          ).executeUpdate() === 1

          doWith(
            ShoppingCartItem.listItemsForUser(
              Ja,
              LoginSession(user1, None, 0),
              0, 100, timestamp("2013-01-01 23:59:59")
            )
          ) { t =>
            t._2.size === 1
            doWith(t._1.table) { total =>
              t._1.table.size === 3
              total(0).shoppingCartItem.sequenceNumber === 5
              total(1).shoppingCartItem.sequenceNumber === 6
              total(2).shoppingCartItem.sequenceNumber === 7
            }
          }
        }}
      }
    }

    "Can examine quantity by item" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          import models.LocaleInfo.{Ja, En}
          TestHelper.removePreloadedRecords()
          val site1 = Site.createNew(Ja, "商店1")
          val site2 = Site.createNew(Ja, "商店2")
          val site3 = Site.createNew(Ja, "商店3")
          val cat1 = Category.createNew(Map(Ja -> "植木"))
          val item1 = Item.createNew(cat1)
          val item2 = Item.createNew(cat1)
          val item3 = Item.createNew(cat1)
          val name1 = ItemName.createNew(item1, Map(Ja -> "杉", En -> "Cedar"))
          val name2 = ItemName.createNew(item2, Map(Ja -> "梅", En -> "Ume"))
          val name3 = ItemName.createNew(item3, Map(Ja -> "松", En -> "Pine"))
          SiteItem.createNew(site1, item1)
          SiteItem.createNew(site2, item1)
          SiteItem.createNew(site2, item2)
          SiteItem.createNew(site3, item3)
          val desc1 = ItemDescription.createNew(item1, site1, "杉説明")
          val desc21 = ItemDescription.createNew(item2, site1, "梅1説明")
          val desc22 = ItemDescription.createNew(item2, site2, "梅2説明")
          val desc3 = ItemDescription.createNew(item3, site1, "松説明")
      
          val price1 = ItemPrice.createNew(item1, site1)
          val price21 = ItemPrice.createNew(item1, site2)
          val price22 = ItemPrice.createNew(item2, site2)
          val price3 = ItemPrice.createNew(item3, site1)

          val tax = Tax.createNew
          val taxHistory = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))
          val ph1 = ItemPriceHistory.createNew(
            price1, tax, CurrencyInfo.Jpy, BigDecimal(119), None, BigDecimal(100), date("9999-12-31")
          )
          val ph21 = ItemPriceHistory.createNew(
            price21, tax, CurrencyInfo.Jpy, BigDecimal(59), None, BigDecimal(50), date("9999-12-31")
          )
          val ph22 = ItemPriceHistory.createNew(
            price22, tax, CurrencyInfo.Jpy, BigDecimal(59), None, BigDecimal(50), date("9999-12-31")
          )
          val ph3 = ItemPriceHistory.createNew(
            price3, tax, CurrencyInfo.Jpy, BigDecimal(59), None, BigDecimal(50), date("9999-12-31")
          )
          val user1 = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL, Some("companyName")
          )

          val e1 = ShoppingCartTotalEntry(
            ShoppingCartItem(
              id = None,
              storeUserId = user1.id.get,
              sequenceNumber = 1,
              siteId = site1.id.get,
              itemId = item1.id.get.id,
              quantity = 2
            ),
            name1(Ja), desc1, site1, ph1, taxHistory, Map(), Map(), Map()
          )
          val e21 = ShoppingCartTotalEntry(
            ShoppingCartItem(
              id = None,
              storeUserId = user1.id.get,
              sequenceNumber = 2,
              siteId = site2.id.get,
              itemId = item1.id.get.id,
              quantity = 4
            ),
            name1(Ja), desc21, site2, ph21, taxHistory, Map(), Map(), Map()
          )
          val e22 = ShoppingCartTotalEntry(
            ShoppingCartItem(
              id = None,
              storeUserId = user1.id.get,
              sequenceNumber = 3,
              siteId = site2.id.get,
              itemId = item2.id.get.id,
              quantity = 6
            ),
            name2(Ja), desc22, site2, ph22, taxHistory, Map(), Map(), Map()
          )
          val e3 = ShoppingCartTotalEntry(
            ShoppingCartItem(
              id = None,
              storeUserId = user1.id.get,
              sequenceNumber = 4,
              siteId = site3.id.get,
              itemId = item3.id.get.id,
              quantity = 8
            ),
            name3(Ja), desc3, site3, ph3, taxHistory, Map(), Map(), Map()
          )
          val e4 = ShoppingCartTotalEntry(
            ShoppingCartItem(
              id = None,
              storeUserId = user1.id.get,
              sequenceNumber = 5,
              siteId = site3.id.get,
              itemId = item3.id.get.id,
              quantity = 12
            ),
            name3(Ja), desc3, site3, ph3, taxHistory, Map(), Map(), Map()
          )

          val total = ShoppingCartTotal(e1 :: e21 :: e22 :: e3 :: e4 :: Nil)
          val q = total.quantityBySiteItem

          q.size === 4
          q(site1.id.get -> item1.id.get.id) === 2
          q(site2.id.get -> item1.id.get.id) === 4
          q(site2.id.get -> item2.id.get.id) === 6
          q(site3.id.get -> item3.id.get.id) === 8 + 12
        }}
      }
    }

    "Can detect items exceed stock" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          import models.LocaleInfo.{Ja, En}
          val site1 = Site.createNew(Ja, "商店1")
          val site2 = Site.createNew(Ja, "商店2")
          val cat1 = Category.createNew(Map(Ja -> "植木"))
          val item1 = Item.createNew(cat1)
          val item2 = Item.createNew(cat1)
          val name1 = ItemName.createNew(item1, Map(Ja -> "杉", En -> "Cedar"))
          val name2 = ItemName.createNew(item2, Map(Ja -> "梅", En -> "Ume"))

          SiteItem.createNew(site1, item1)
          SiteItem.createNew(site2, item1)
          SiteItem.createNew(site2, item2)
      
          val desc1 = ItemDescription.createNew(item1, site1, "杉説明")
          val desc21 = ItemDescription.createNew(item1, site2, "杉説明2")
          val desc22 = ItemDescription.createNew(item2, site2, "梅説明2")

          val price1 = ItemPrice.createNew(item1, site1)
          val price21 = ItemPrice.createNew(item1, site2)
          val price22 = ItemPrice.createNew(item2, site2)

          val tax = Tax.createNew
          val taxHistory = TaxHistory.createNew(tax, TaxType.INNER_TAX, BigDecimal("5"), date("9999-12-31"))
          val ph1 = ItemPriceHistory.createNew(
            price1, tax, CurrencyInfo.Jpy, BigDecimal(119), None, BigDecimal(100), date("9999-12-31")
          )
          val ph21 = ItemPriceHistory.createNew(
            price21, tax, CurrencyInfo.Jpy, BigDecimal(119), None, BigDecimal(100), date("9999-12-31")
          )
          val ph22 = ItemPriceHistory.createNew(
            price22, tax, CurrencyInfo.Jpy, BigDecimal(119), None, BigDecimal(100), date("9999-12-31")
          )
          val user1 = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL, Some("companyName")
          )
          val user2 = StoreUser.create(
            "name2", "first2", None, "last2", "email2", 123L, 234L, UserRole.NORMAL, Some("companyName")
          )

/*
          |-------+-------+-------+----------+----------|
          | user  | site  | item  | cart seq | quantity |
          |-------+-------+-------+----------+----------|
          | user1 | site1 | item1 | #1       |        2 |
          | user1 | site1 | item1 | #2       |        4 |
          | user1 | site2 | item1 | #3       |        6 |
          | user1 | site2 | item2 | #4       |        8 |
          | user2 | site1 | item1 | #1       |        8 |
          |-------+-------+-------+----------+----------|

          |-------+-------+-----------|
          | site  | item  | max stock |
          |-------+-------+-----------|
          | site1 | item1 |         5 |
          |-------+-------+-----------|
*/

          ShoppingCartItem.addItem(user1.id.get, site1.id.get, item1.id.get.id, 2)
          ShoppingCartItem.addItem(user1.id.get, site1.id.get, item1.id.get.id, 4)
          ShoppingCartItem.addItem(user1.id.get, site2.id.get, item1.id.get.id, 6)
          ShoppingCartItem.addItem(user1.id.get, site2.id.get, item2.id.get.id, 8)
          ShoppingCartItem.addItem(user2.id.get, site1.id.get, item1.id.get.id, 8)

          ShoppingCartItem.itemsExceedStock(user1.id.get, Ja).size === 0

          SiteItemNumericMetadata.createNew(site1.id.get, item1.id.get, SiteItemNumericMetadataType.STOCK, 5)
          val result: immutable.Map[(ItemId, Long), (String, String, Int, Long)] = 
            ShoppingCartItem.itemsExceedStock(user1.id.get, Ja)

          result.size === 1
          result(item1.id.get -> site1.id.get) === (site1.name, name1(Ja).name, 6, 5)
        }
      }}
    }
  }
}
