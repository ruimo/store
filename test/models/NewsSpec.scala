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
  }
}
