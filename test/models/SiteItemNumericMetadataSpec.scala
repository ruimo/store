package models

import org.specs2.mutable._

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

class SiteItemNumericMetadataSpec extends Specification {
  implicit def date2milli(d: java.sql.Date) = d.getTime

  "SiteItemNumericMetadata" should {
    "Can create new record" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          val cat1 = Category.createNew(
            Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
          )
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val item1 = Item.createNew(cat1)

          val rec1 = SiteItemNumericMetadata.createNew(
            site1.id.get, item1.id.get, SiteItemNumericMetadataType.HIDE, 1, 2L
          )

          SiteItemNumericMetadata.all(
            site1.id.get, item1.id.get
          ).size === 0

          doWith(
            SiteItemNumericMetadata.all(
              site1.id.get, item1.id.get, 1L
            )
          ) { map =>
            map.size === 1
            map(SiteItemNumericMetadataType.HIDE) === rec1
          }

          SiteItemNumericMetadata.all(
            site1.id.get, item1.id.get, 2L
          ).size === 0
        }
      }
    }

    "Can pick valid record" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          val cat1 = Category.createNew(
            Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
          )
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val item1 = Item.createNew(cat1)

          val rec1 = SiteItemNumericMetadata.createNew(
            site1.id.get, item1.id.get, SiteItemNumericMetadataType.HIDE, 1, 2L
          )
          val rec2 = SiteItemNumericMetadata.createNew(
            site1.id.get, item1.id.get, SiteItemNumericMetadataType.HIDE, 2, 4L
          )

          SiteItemNumericMetadata.all(
            site1.id.get, item1.id.get
          ).size === 0

          doWith(
            SiteItemNumericMetadata.all(
              site1.id.get, item1.id.get, 1L
            )
          ) { map =>
            map.size === 1
            map(SiteItemNumericMetadataType.HIDE) === rec1
          }

          doWith(
            SiteItemNumericMetadata.all(
              site1.id.get, item1.id.get, 2L
            )
          ) { map =>
            map.size === 1
            map(SiteItemNumericMetadataType.HIDE) === rec2
          }

          doWith(
            SiteItemNumericMetadata.all(
              site1.id.get, item1.id.get, 3L
            )
          ) { map =>
            map.size === 1
            map(SiteItemNumericMetadataType.HIDE) === rec2
          }

          SiteItemNumericMetadata.all(
            site1.id.get, item1.id.get, 4L
          ).size === 0
        }
      }
    }
  }
}

