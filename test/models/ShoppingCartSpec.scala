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
import java.sql.Date.{valueOf => date}

class ShoppingCartSpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  "ShoppingCart" should {
    "addItem will assign sequence number." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          TestHelper.removePreloadedRecords()

          val tax = Tax.createNew

          val user1 = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL
          )
          val user2 = StoreUser.create(
            "name2", "first2", None, "last2", "email2", 987L, 765L, UserRole.NORMAL
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

          val ph1 = ItemPriceHistory.createNew(price1, tax, CurrencyInfo.Jpy, BigDecimal(100), date("2013-01-02"))
          val ph2 = ItemPriceHistory.createNew(price1, tax, CurrencyInfo.Jpy, BigDecimal(101), date("9999-12-31"))

          val ph3 = ItemPriceHistory.createNew(price2, tax, CurrencyInfo.Jpy, BigDecimal(300), date("2013-01-03"))
          val ph4 = ItemPriceHistory.createNew(price2, tax, CurrencyInfo.Jpy, BigDecimal(301), date("9999-12-31"))

          val cart1 = ShoppingCartItem.addItem(user1.id.get, site1.id.get, item1.id.get, 2)
          val cart2 = ShoppingCartItem.addItem(user1.id.get, site2.id.get, item2.id.get, 3)

          val cart3 = ShoppingCartItem.addItem(user2.id.get, site1.id.get, item1.id.get, 10)

          cart1.storeUserId === user1.id.get
          cart1.sequenceNumber === 1
          cart1.itemId === item1.id.get
          cart1.quantity === 2

          cart2.storeUserId === user1.id.get
          cart2.sequenceNumber === 2
          cart2.itemId === item2.id.get
          cart2.quantity === 3

          cart3.storeUserId === user2.id.get
          cart3.sequenceNumber === 1
          cart3.itemId === item1.id.get
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
        }}
      }
    }

    "changeQuantity will change quantity." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          TestHelper.removePreloadedRecords()
          val tax = Tax.createNew

          val user1 = StoreUser.create(
            "name1", "first1", None, "last1", "email1", 123L, 234L, UserRole.NORMAL
          )

          import models.LocaleInfo.{Ja, En}
          val site1 = Site.createNew(Ja, "商店1")
          val cat1 = Category.createNew(Map(Ja -> "植木", En -> "Plant"))
          val item1 = Item.createNew(cat1)

          SiteItem.createNew(site1, item1)

          val cart1 = ShoppingCartItem.addItem(user1.id.get, site1.id.get, item1.id.get, 2)

          cart1.storeUserId === user1.id.get
          cart1.sequenceNumber === 1
          cart1.itemId === item1.id.get
          cart1.quantity === 2

          ShoppingCartItem.changeQuantity(cart1.id.get, user1.id.get, 5)
          ShoppingCartItem(cart1.id.get).quantity === 5
        }}
      }
    }
  }
}

