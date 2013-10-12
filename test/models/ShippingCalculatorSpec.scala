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

class ShippingCalculatorSpec extends Specification {
  "ShippingCalculator" should {
    "Earn empty map when no items." in {
      ShippingFeeEntries().bySiteAndItemClass.size === 0
    }

    "On item earns single entry." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn =>
          val site = Site.createNew(LocaleInfo.Ja, "商店1")
          val e = ShippingFeeEntries().add(site, 2, 5)

          e.bySiteAndItemClass.size === 1
          val byItemClass = e.bySiteAndItemClass(site)
          byItemClass.size === 1
          byItemClass(2) === 5
        }
      }
    }

    "Quantity should added." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn =>
          val site = Site.createNew(LocaleInfo.Ja, "商店1")

          val e = ShippingFeeEntries()
            .add(site, 2, 5)
            .add(site, 2, 10)

          e.bySiteAndItemClass.size === 1
          val byItemClass = e.bySiteAndItemClass(site)
          byItemClass.size === 1
          byItemClass(2) === 15
        }
      }
    }

    "Two item classes earn two entries." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn =>
          val site = Site.createNew(LocaleInfo.Ja, "商店1")

          val e = new ShippingFeeEntries()
            .add(site, 2, 5)
            .add(site, 2, 10)
            .add(site, 3, 10)

          e.bySiteAndItemClass.size === 1
          val byItemClass = e.bySiteAndItemClass(site)
          byItemClass.size === 2
          byItemClass(2) === 15
          byItemClass(3) === 10
        }
      }
    }

    "Two sites and two items classes earn four entries." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn =>
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.Ja, "商店2")

          val e = ShippingFeeEntries()
            .add(site1, 2, 5)
            .add(site1, 2, 10)
            .add(site1, 3, 10)
            .add(site2, 2, 3)
            .add(site2, 3, 2)

          e.bySiteAndItemClass.size === 2
          val byItemClass1 = e.bySiteAndItemClass(site1)
          byItemClass1.size === 2
          byItemClass1(2) === 15
          byItemClass1(3) === 10

          val byItemClass2 = e.bySiteAndItemClass(site2)
          byItemClass2.size === 2
          byItemClass2(2) === 3
          byItemClass2(3) === 2
        }
      }
    }
  }
}
