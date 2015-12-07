package models

import org.specs2.mutable._

import com.ruimo.scoins.Scoping._
import anorm._
import anorm.SqlParser
import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current

class WebSnippetSpec extends Specification {
  "WebSnippet" should {
    "Can create new record." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()
        DB.withConnection { implicit conn => {
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.En, "Shop2")

          WebSnippet.createNew(site1.id.get, "code01", "site11", 111L, 3)
          WebSnippet.createNew(site1.id.get, "code01", "site12", 111L, 3)
          WebSnippet.createNew(site1.id.get, "code01", "site13", 111L, 3)

          WebSnippet.createNew(site1.id.get, "code01", "site14", 111L, 3) must throwA(new MaxWebSnippetCountException(site1.id.get))

          WebSnippet.createNew(site2.id.get, "code01", "site21", 111L, 2)
          WebSnippet.createNew(site2.id.get, "code01", "site22", 111L, 2)

          WebSnippet.createNew(site2.id.get, "code01", "site23", 111L, 2) must throwA(new MaxWebSnippetCountException(site2.id.get))
        }}
      }
    }

    "Can list newest of every site." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()
        DB.withConnection { implicit conn => {
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.En, "Shop2")

          WebSnippet.createNew(site1.id.get, "code01", "site11", 111L)
          WebSnippet.createNew(site1.id.get, "code01", "site12", 122L)
          WebSnippet.createNew(site1.id.get, "code02", "site13", 133L)

          WebSnippet.createNew(site2.id.get, "code01", "site21", 120L)
          WebSnippet.createNew(site2.id.get, "code02", "site22", 130L)
          WebSnippet.createNew(site2.id.get, "code02", "site23", 140L)

          val list: Seq[WebSnippet] = WebSnippet.listNewerBySite(Some("code01"), 1)
          list.size === 2
          doWith(list(0)) { w =>
            w.siteId === site1.id.get
            w.contentCode === "code01"
            w.content === "site12"
            w.updatedTime === 122L
          }
          doWith(list(1)) { w =>
            w.siteId === site2.id.get
            w.contentCode === "code01"
            w.content === "site21"
            w.updatedTime === 120L
          }
        }}
      }
    }
  }
}
