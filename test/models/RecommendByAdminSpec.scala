package models

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current
import anorm.Id
import helpers.Helper._
import com.ruimo.scoins.Scoping._

class RecommendByAdminSpec extends Specification {
  "RecommendByAdmin" should {
    "Can create record and query single record" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()
        DB.withConnection { implicit conn =>
          RecommendByAdmin.count === 0

          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val cat1 = Category.createNew(
            Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
          )
          val item1 = Item.createNew(cat1)
          val rec1 = RecommendByAdmin.createNew(
            site1.id.get, item1.id.get, 123, true
          )
          val read1 = RecommendByAdmin(rec1.id.get)
          read1 === rec1

          RecommendByAdmin.count === 1

          val site2 = Site.createNew(LocaleInfo.Ja, "商店2")
          val cat2 = Category.createNew(
            Map(LocaleInfo.Ja -> "植木2", LocaleInfo.En -> "Plant2")
          )
          val item2 = Item.createNew(cat2)
          val rec2 = RecommendByAdmin(
            rec1.id,
            site2.id.get,
            item2.id.get,
            234,
            false
          )
          RecommendByAdmin.update(rec2)
          val read2 = RecommendByAdmin(rec1.id.get)
          read2 === rec2

          RecommendByAdmin.remove(rec2.id.get)
          RecommendByAdmin.count === 0
        }
      }
    }

    "Can list records" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()
        DB.withConnection { implicit conn =>
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val cat1 = Category.createNew(
            Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
          )
          val item1 = Item.createNew(cat1)
          val itemName1 = ItemName.createNew(item1, Map(LocaleInfo.Ja -> "Item1"))
          val site2 = Site.createNew(LocaleInfo.Ja, "商店2")
          val cat2 = Category.createNew(
            Map(LocaleInfo.Ja -> "植木2", LocaleInfo.En -> "Plant2")
          )
          val item2 = Item.createNew(cat2)
          val item3 = Item.createNew(cat1)

          val rec1 = RecommendByAdmin.createNew(site1.id.get, item1.id.get, 123, true)
          val rec2 = RecommendByAdmin.createNew(site2.id.get, item2.id.get, 100, true)
          val rec3 = RecommendByAdmin.createNew(site2.id.get, item3.id.get, 105, false)
          doWith(RecommendByAdmin.listByScore(showDisabled = false, locale = LocaleInfo.Ja)) { list =>
            list.records.size === 2
            list.records(0)._1 === rec1
            list.records(0)._2 === Some(itemName1(LocaleInfo.Ja))
            list.records(0)._3 === Some(site1)
            list.records(1)._1 === rec2
            list.records(1)._2 === None
            list.records(1)._3 === Some(site2)
          }

          doWith(RecommendByAdmin.listByScore(showDisabled = true, locale = LocaleInfo.Ja)) { list =>
            list.records.size === 3
            list.records(0)._1 === rec1
            list.records(0)._2 === Some(itemName1(LocaleInfo.Ja))
            list.records(0)._3 === Some(site1)
            list.records(1)._1 === rec3
            list.records(1)._2 === None
            list.records(1)._3 === Some(site2)
            list.records(2)._1 === rec2
            list.records(2)._2 === None
            list.records(2)._3 === Some(site2)
          }
        }
      }
    }

    "Hidden items should not be shown" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()
        DB.withConnection { implicit conn =>
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val cat1 = Category.createNew(
            Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
          )
          val item1 = Item.createNew(cat1)
          val itemName1 = ItemName.createNew(item1, Map(LocaleInfo.Ja -> "Item1"))
          val site2 = Site.createNew(LocaleInfo.Ja, "商店2")
          val cat2 = Category.createNew(
            Map(LocaleInfo.Ja -> "植木2", LocaleInfo.En -> "Plant2")
          )
          val item2 = Item.createNew(cat2)
          SiteItemNumericMetadata.createNew(
            site1.id.get, item1.id.get,
            SiteItemNumericMetadataType.HIDE,
            0
          )
          SiteItemNumericMetadata.createNew(
            site2.id.get, item2.id.get,
            SiteItemNumericMetadataType.HIDE,
            1
          )

          val rec1 = RecommendByAdmin.createNew(site1.id.get, item1.id.get, 123, true)
          val rec2 = RecommendByAdmin.createNew(site2.id.get, item2.id.get, 100, true)
          doWith(RecommendByAdmin.listByScore(showDisabled = false, locale = LocaleInfo.Ja)) { list =>
            list.records.size === 1
            list.records(0)._1 === rec1
            list.records(0)._2 === Some(itemName1(LocaleInfo.Ja))
            list.records(0)._3 === Some(site1)
          }
        }
      }
    }
  }
}
