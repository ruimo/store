package models

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current
import anorm._

class ConstraintHelperSpec extends Specification {
  "ConstraintHelper" should {
    "be able to retrieve column size (8 for LOCALE.LANG) " in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()
        val x = ConstraintHelper.getColumnSize(None,"LOCALE","LANG")
        x === 8
      }
    }

    "be able to follow column size change at runtime by purging cached stale values" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()
        DB.withConnection { implicit conn =>
          SQL("create table TEST_TABLE (id int primary key, HOGE varchar(8));").executeUpdate()
        }
        ConstraintHelper.getColumnSize(None,"TEST_TABLE","HOGE") === 8

        DB.withConnection { implicit conn =>
          SQL("alter table TEST_TABLE alter column HOGE varchar(16);").executeUpdate()
        }
        ConstraintHelper.refreshColumnSizes()
        ConstraintHelper.getColumnSize(None,"TEST_TABLE","HOGE") === 16
      }
    }
  }
}
