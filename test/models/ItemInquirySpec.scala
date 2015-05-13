package models

import org.specs2.mutable._

import anorm._
import anorm.SqlParser
import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current
import anorm.Id

class ItemInquirySpec extends Specification {
  "ItemInquiry" should {
    "Can create new record." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn =>
          import LocaleInfo._

          val site0 = Site.createNew(Ja, "Site00")
          val site1 = Site.createNew(Ja, "Site01")
          val cat0 = Category.createNew(Map(Ja -> "植木", En -> "Plant"))
          val cat1 = Category.createNew(Map(Ja -> "植木2", En -> "Plant2"))
          val item0 = Item.createNew(cat0)
          val item1 = Item.createNew(cat1)
          val user0 = StoreUser.create(
            "userName0", "firstName0", Some("middleName0"), "lastName0", "email0",
            1L, 2L, UserRole.ADMIN, Some("companyName0")
          )
          val user1 = StoreUser.create(
            "userName1", "firstName1", Some("middleName1"), "lastName1", "email1",
            2L, 3L, UserRole.NORMAL, Some("companyName1")
          )

          val rec0 = ItemInquiry.createNew(
            site0.id.get, item0.id.get, user0.id.get, ItemInquiryType.QUERY, "user0", "email0", 123L
          )
          val rec1 = ItemInquiry.createNew(
            site1.id.get, item1.id.get, user1.id.get, ItemInquiryType.QUERY, "user1", "email1", 234L
          )
          
          ItemInquiry(rec0.id.get) === rec0
          ItemInquiry(rec1.id.get) === rec1

          val fields = Map(
            'foo -> "Hello", 'bar -> "World"
          )
          ItemInquiryField.createNew(rec0.id.get, fields)
          ItemInquiryField.createNew(rec1.id.get, Map())

          ItemInquiryField(rec0.id.get) === fields
          ItemInquiryField(rec1.id.get).isEmpty === true
        }
      }
    }
  }
}
