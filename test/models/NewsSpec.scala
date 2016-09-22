package models

import org.specs2.mutable._

import anorm._
import anorm.SqlParser
import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current
import anorm.Id

class NewsSpec extends Specification {
  "News" should {
    "Can create site's record." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn =>
          val site = Site.createNew(LocaleInfo.Ja, "商店1")
          val news = News.createNew(Some(site.id.get), "contents01", 123L, 234L)
          news === News(news.id.get)
          val list = News.list()
          list.records.size === 1
          list.records(0)._1 === news
          list.records(0)._2 === Some(site)
        }
      }
    }

    "Can create admin's record." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn =>
          val news = News.createNew(None, "contents01", 123L, 234L)
          news === News(news.id.get)
          val list = News.list()
          list.records.size === 1
          list.records(0)._1 === news
          list.records(0)._2 === None
        }
      }
    }

    "Can order records." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn =>
          val site = Site.createNew(LocaleInfo.Ja, "商店1")
          val news = Vector(
            News.createNew(None, "contents01", releaseTime = 123L, 234L),
            News.createNew(site.id, "contents02", releaseTime = 234L, 222L),
            News.createNew(None, "contents03", releaseTime = 345L, 111L)
          )
          val list = News.list()
          list.records.size === 3
          list.records(0)._1 === news(2)
          list.records(0)._2 === None
          list.records(1)._1 === news(1)
          list.records(1)._2 === Some(site)
          list.records(2)._1 === news(0)
          list.records(2)._2 === None
        }
      }
    }
  }
}
