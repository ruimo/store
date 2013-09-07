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

        Item.list(siteId = 1, locale = LocaleInfo.Ja, queryString = "foo") === List()
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

      }
    }
  }
}
