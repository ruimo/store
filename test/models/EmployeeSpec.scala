package models

import org.specs2.mutable._

import com.ruimo.scoins.Scoping._
import anorm._
import anorm.SqlParser
import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current

class EmployeeSpec extends Specification {
  "Employee" should {
    "Can create employee." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.En, "Shop2")
          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.NORMAL, Some("companyName")
          )
          val user2 = StoreUser.create(
            "userName2", "firstName2", Some("middleName2"), "lastName2", "email2",
            1L, 2L, UserRole.NORMAL, Some("companyName2")
          )
          val user3 = StoreUser.create(
            "userName3", "firstName3", Some("middleName3"), "lastName3", "email3",
            1L, 2L, UserRole.NORMAL, Some("companyName3")
          )

          val e1 = Employee.createNew(site1.id.get, user1.id.get)
          val e2 = Employee.createNew(site1.id.get, user2.id.get)
          val e3 = Employee.createNew(site2.id.get, user3.id.get)

          doWith(Employee(e1.id.get.id)) { rec =>
            rec.siteId === site1.id.get
            rec.userId === user1.id.get
          }

          doWith(Employee(e2.id.get.id)) { rec =>
            rec.siteId === site1.id.get
            rec.userId === user2.id.get
          }

          doWith(Employee(e3.id.get.id)) { rec =>
            rec.siteId === site2.id.get
            rec.userId === user3.id.get
          }
        }
      }
    }

    "Can check if employee." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.NORMAL, Some("companyName")
          )
          val user2 = StoreUser.create(
            "userName2", "firstName2", Some("middleName2"), "lastName2", "email2",
            1L, 2L, UserRole.NORMAL, Some("companyName2")
          )
          val user3 = StoreUser.create(
            "userName3", "firstName3", Some("middleName3"), "lastName3", "email3",
            1L, 2L, UserRole.NORMAL, Some("companyName3")
          )

          val e1 = Employee.createNew(site1.id.get, user1.id.get)
          val e2 = Employee.createNew(site1.id.get, user2.id.get)

          doWith(Employee.getBelonging(user1.id.get).get) { rec =>
            rec.siteId === site1.id.get
            rec.userId === user1.id.get
          }

          doWith(Employee.getBelonging(user2.id.get).get) { rec =>
            rec.siteId === site1.id.get
            rec.userId === user2.id.get
          }

          Employee.getBelonging(user3.id.get) === None
        }
      }
    }
  }
}

