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

class ShoppingCartSpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime

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
          val list1 = ShoppingCartItem.listItemsForUser(Ja, user1.id.get, 0, 10, time)
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
          val list2 = ShoppingCartItem.listItemsForUser(Ja, user2.id.get, 0, 10, time2)
          list2.size === 1
          list2(0).shoppingCartItem === cart3
          list2(0).itemName === name1(Ja)
          list2(0).itemDescription === desc1
          list2(0).site === site1
          list2(0).itemPriceHistory === ph1

          ShoppingCartItem.isAllCoupon(user1.id.get) === false
          ShoppingCartItem.isAllCoupon(user2.id.get) === false
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
          val list1 = ShoppingCartItem.listItemsForUser(Ja, user1.id.get, 0, 10, time)
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
          val list1 = ShoppingCartItem.listItemsForUser(Ja, user1.id.get, 0, 10, time)
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
          val list1 = ShoppingCartItem.listItemsForUser(Ja, user1.id.get, 0, 10, time)
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
          val list1 = ShoppingCartItem.listItemsForUser(Ja, user1.id.get, 0, 10, time)
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

          val list2 = ShoppingCartItem.listItemsForUser(Ja, user1.id.get, 0, 10, time)
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
          val list1 = ShoppingCartItem.listItemsForUser(Ja, user1.id.get, 0, 10, time)

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
          val list1 = ShoppingCartItem.listItemsForUser(Ja, user1.id.get, 0, 10, time)

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
          val list1 = ShoppingCartItem.listItemsForUser(Ja, user1.id.get, 0, 10, time)

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
  }
}
