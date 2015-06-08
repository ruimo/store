package models

import org.specs2.mutable._

import anorm._
import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current
import anorm.Id
import com.ruimo.csv.CsvRecord
import com.ruimo.csv.CsvHeader
import com.ruimo.csv.CsvParseException
import scala.util.{Try, Failure, Success}
import com.ruimo.scoins.Scoping._
import helpers.PasswordHash

class UserSpec extends Specification {
  "User" should {
    "User count should be zero when table is empty" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          StoreUser.count === 0
        }}
      }
    }

    "User count should reflect the number of records in the table" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )
          StoreUser.count === 1
        }}
      }
    }

    "User can be queried by username" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )

          val user2 = StoreUser.create(
            "userName2", "firstName2", None, "lastName2", "email2",
            1L, 2L, UserRole.ADMIN, None
          )

          StoreUser.findByUserName("userName").get === user1
          StoreUser.findByUserName("userName2").get === user2
        }}
      }
    }

    "Can delete user" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )

          val user2 = StoreUser.create(
            "userName2", "firstName2", None, "lastName2", "email2",
            1L, 2L, UserRole.ADMIN, None
          )

          StoreUser.listUsers().records.size === 2
          StoreUser.delete(user2.id.get)
          val list = StoreUser.listUsers()
          list.records.size === 1
          list.records(0).user === user1
        }
      }
    }

    "Can list only employee" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          val user1 = StoreUser.create(
            "1-userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.NORMAL, Some("companyName")
          )

          val user2 = StoreUser.create(
            "2-userName2", "firstName2", None, "lastName2", "email2",
            1L, 2L, UserRole.NORMAL, None
          )

          doWith(StoreUser.listUsers().records) { records =>
            records.size === 2
            records(0).user === user1
            records(1).user === user2
          }

          doWith(StoreUser.listUsers(employeeSiteId = Some(1)).records) { records =>
            records.size === 1
            records(0).user === user1
          }
          
          doWith(StoreUser.listUsers(employeeSiteId = Some(2)).records) { records =>
            records.size === 1
            records(0).user === user2
          }
        }
      }
    }

    "Notification email user" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )

          val user2 = StoreUser.create(
            "userName2", "firstName2", None, "lastName2", "email2",
            1L, 2L, UserRole.ADMIN, None
          )

          val notification = OrderNotification.createNew(user2.id.get)

          val u1 = StoreUser.withSite(user1.id.get)
          u1.sendNoticeMail === false

          val u2 = StoreUser.withSite(user2.id.get)
          u2.sendNoticeMail === true

          val list = StoreUser.listUsers()
          list.records.size === 2
          list.records(0).user === user1
          list.records(0).sendNoticeMail === false

          list.records(1).user === user2
          list.records(1).sendNoticeMail === true
        }
      }
    }

    "listUsers list user ordered by user name" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          TestHelper.removePreloadedRecords()

          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )

          val user2 = StoreUser.create(
            "userName2", "firstName2", None, "lastName2", "email2",
            1L, 2L, UserRole.ADMIN, None
          )
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")

          val siteUser = SiteUser.createNew(user2.id.get, site1.id.get)

          val list = StoreUser.listUsers()
          list.records.size === 2
          list.records(0).user === user1
          list.records(0).siteUser === None
          list.records(0).sendNoticeMail === false

          list.records(1).user === user2
          list.records(1).siteUser.get === siteUser
          list.records(1).sendNoticeMail === false
        }
      }
    }

    "Can add users by csv" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          val header = CsvHeader("CompanyId", "EmployeeNo", "Password")
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.Ja, "商店2")

          StoreUser.maintainByCsv(Iterator(
            Success(CsvRecord(1, header, Vector(site1.id.get.toString, "01234567", "pass001"))),
            Success(CsvRecord(2, header, Vector(site1.id.get.toString, "98765432", "pass002")))
          ))

          doWith(StoreUser.listUsers().records) { records =>
            records.size === 2
            doWith(records.map {_.user}.map { r => (r.userName, r)}.toMap) { map =>
              doWith(map(site1.id.get.toString + "-01234567")) { rec =>
                rec.firstName === ""
                rec.middleName === None
                rec.lastName === ""
                rec.email === ""
                rec.deleted === false
                rec.userRole === UserRole.NORMAL
                rec.companyName === None
                PasswordHash.generate("pass001", rec.salt) === rec.passwordHash
              }

              doWith(map(site1.id.get.toString + "-98765432")) { rec =>
                rec.firstName === ""
                rec.middleName === None
                rec.lastName === ""
                rec.email === ""
                rec.deleted === false
                rec.userRole === UserRole.NORMAL
                rec.companyName === None
                PasswordHash.generate("pass002", rec.salt) === rec.passwordHash
              }
            }
          }
        }
      }
    }

    "Exception should be thrown if an error is found in csv" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          val header = CsvHeader("CompanyId", "EmployeeNo", "Password")
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.Ja, "商店2")

          StoreUser.maintainByCsv(Iterator(
            Success(CsvRecord(1, header, Vector(site1.id.get.toString, "01234567", "pass001"))),
            Failure(new CsvParseException("", new Exception, 1))
          )) must throwA[CsvParseException]

          // No record should changed.
          doWith(StoreUser.listUsers().records) { records =>
            records.size === 0
          }
        }
      }
    }

    "Exiting record should not be changed by csv" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          val header = CsvHeader("CompanyId", "EmployeeNo", "Password")
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.Ja, "商店2")

          // Existing record. This should not be changed.
          StoreUser.create(
            site1.id.get.toString + "-01234567", "first001", Some("middle001"), "last001",
            "email001", 123L, 234L, UserRole.NORMAL, Some("company001")
          )

          val (insCount, delCount) = StoreUser.maintainByCsv(Iterator(
            Success(CsvRecord(1, header, Vector(site1.id.get.toString, "01234567", "pass001"))),
            Success(CsvRecord(1, header, Vector(site1.id.get.toString, "98765432", "pass002")))
          ))

          insCount === 1
          delCount === 0

          doWith(StoreUser.listUsers().records) { records =>
            records.size === 2
            doWith(records.map {_.user}.map { r => (r.userName, r)}.toMap) { map =>
              doWith(map(site1.id.get.toString + "-01234567")) { rec =>
                rec.userName === site1.id.get.toString + "-01234567"
                rec.firstName === "first001"
                rec.middleName === Option("middle001")
                rec.lastName === "last001"
                rec.email === "email001"
                rec.passwordHash === 123L
                rec.salt === 234L
                rec.userRole === UserRole.NORMAL
                rec.companyName === Some("company001")
              }
            }
          }
        }
      }
    }

    "Non exiting record should not be deleted by csv" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          val header = CsvHeader("CompanyId", "EmployeeNo", "Password")
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.Ja, "商店2")

          // Existing record. This should not be deleted.
          StoreUser.create(
            site1.id.get.toString + "-01234567", "first001", Some("middle001"), "last001",
            "email001", 123L, 234L, UserRole.NORMAL, Some("company001")
          )

          // Existing record. This should not be changed.
          StoreUser.create(
            site1.id.get.toString + "-user002", "first002", Some("middle002"), "last002",
            "email002", 123L, 234L, UserRole.NORMAL, Some("company002")
          )

          val (insCount, delCount) = StoreUser.maintainByCsv(Iterator(
            Success(CsvRecord(1, header, Vector(site1.id.get.toString, "01234567", "pass001")))
          ))

          insCount === 0
          delCount === 1

          doWith(StoreUser.listUsers().records) { records =>
            records.size === 1
            doWith(records.map {_.user}.map { r => (r.userName, r)}.toMap) { map =>
              map.get(site1.id.get.toString + "-user002") === None
            }
          }
        }
      }
    }

    "Admin users should not be affected by csv" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          val header = CsvHeader("CompanyId", "EmployeeNo", "Password")
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.Ja, "商店2")

          // Existing record. This should be deleted.
          StoreUser.create(
            site1.id.get.toString + "-11111111", "first001", Some("middle001"), "last001",
            "email001", 123L, 234L, UserRole.NORMAL, Some("company001")
          )

          // Existing record. This should be deleted.
          StoreUser.create(
            site1.id.get.toString + "-22222222", "first002", Some("middle002"), "last002",
            "email002", 123L, 234L, UserRole.NORMAL, Some("company002")
          )

          // Existing record. This should not be changed.
          StoreUser.create(
            site1.id.get.toString + "-33333333", "first003", Some("middle003"), "last003",
            "email003", 123L, 234L, UserRole.NORMAL, Some("company003")
          )

          // Existing record. This should not be changed.
          StoreUser.create(
            site1.id.get.toString + "-44444444", "first004", Some("middle004"), "last004",
            "email004", 123L, 234L, UserRole.NORMAL, Some("company004")
          )

          // Existing record. This should not be changed because this is admin.
          StoreUser.create(
            site1.id.get.toString + "-55555555", "first005", Some("middle005"), "last005",
            "email005", 123L, 234L, UserRole.ADMIN, Some("company005")
          )

          val (insCount, delCount) = StoreUser.maintainByCsv(Iterator(
            Success(CsvRecord(1, header, Vector(site1.id.get.toString, "33333333", "pass003"))),
            Success(CsvRecord(1, header, Vector(site1.id.get.toString, "44444444", "pass004"))),
            Success(CsvRecord(1, header, Vector(site1.id.get.toString, "66666666", "pass006"))),
            Success(CsvRecord(1, header, Vector(site1.id.get.toString, "77777777", "pass007")))
          ))

          insCount === 2
          delCount === 2

          doWith(StoreUser.listUsers().records) { records =>
            records.size === 5
            doWith(records.map {_.user}.map { r => (r.userName, r)}.toMap) { map =>
              map.get(site1.id.get.toString + "-11111111") === None
              map.get(site1.id.get.toString + "-22222222") === None
              doWith(map(site1.id.get.toString + "-33333333")) { rec =>
                rec.userName === site1.id.get.toString + "-33333333"
                rec.firstName === "first003"
                rec.middleName === Option("middle003")
                rec.lastName === "last003"
                rec.email === "email003"
                rec.passwordHash === 123L
                rec.salt === 234L
                rec.userRole === UserRole.NORMAL
                rec.companyName === Some("company003")
              }

              doWith(map(site1.id.get.toString + "-44444444")) { rec =>
                rec.userName === site1.id.get.toString + "-44444444"
                rec.firstName === "first004"
                rec.middleName === Option("middle004")
                rec.lastName === "last004"
                rec.email === "email004"
                rec.passwordHash === 123L
                rec.salt === 234L
                rec.userRole === UserRole.NORMAL
                rec.companyName === Some("company004")
              }

              doWith(map(site1.id.get.toString + "-55555555")) { rec =>
                rec.userName === site1.id.get.toString + "-55555555"
                rec.firstName === "first005"
                rec.middleName === Option("middle005")
                rec.lastName === "last005"
                rec.email === "email005"
                rec.passwordHash === 123L
                rec.salt === 234L
                rec.userRole === UserRole.ADMIN
                rec.companyName === Some("company005")
              }

              doWith(map(site1.id.get.toString + "-66666666")) { rec =>
                rec.firstName === ""
                rec.middleName === None
                rec.lastName === ""
                rec.email === ""
                rec.deleted === false
                rec.userRole === UserRole.NORMAL
                rec.companyName === None
                PasswordHash.generate("pass006", rec.salt) === rec.passwordHash
              }

              doWith(map(site1.id.get.toString + "-77777777")) { rec =>
                rec.firstName === ""
                rec.middleName === None
                rec.lastName === ""
                rec.email === ""
                rec.deleted === false
                rec.userRole === UserRole.NORMAL
                rec.companyName === None
                PasswordHash.generate("pass007", rec.salt) === rec.passwordHash
              }
            }
          }
        }
      }
    }

    "Can filter csv" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn =>
          val header = CsvHeader("CompanyId", "EmployeeNo", "Password")
          val site1 = Site.createNew(LocaleInfo.Ja, "商店1")
          val site2 = Site.createNew(LocaleInfo.Ja, "商店2")
          val siteId = site1.id.get.toString

          // Existing record. This should not be affected.
          StoreUser.create(
            site2.id.get.toString + "-11111111", "first001", Some("middle001"), "last001",
            "email001", 123L, 234L, UserRole.NORMAL, Some("company001")
          )

          StoreUser.maintainByCsv(
            Iterator(
              Success(CsvRecord(1, header, Vector(site1.id.get.toString, "11111111", "pass001"))),
              Success(CsvRecord(2, header, Vector(site1.id.get.toString, "22222222", "pass002"))),
              Success(CsvRecord(3, header, Vector(site2.id.get.toString, "33333333", "pass003")))
            ),
            rec => rec('CompanyId) == siteId,
            Some("user_name like '" + siteId + "-%'")
          )

          doWith(StoreUser.listUsers().records) { records =>
            records.size === 3
            doWith(records.map {_.user}.map { r => (r.userName, r)}.toMap) { map =>
              map.get(site2.id.get.toString + "-33333333") === None
              doWith(map(site1.id.get.toString + "-11111111")) { rec =>
                rec.firstName === ""
                rec.middleName === None
                rec.lastName === ""
                rec.email === ""
                rec.deleted === false
                rec.userRole === UserRole.NORMAL
                rec.companyName === None
                PasswordHash.generate("pass001", rec.salt) === rec.passwordHash
              }
              doWith(map(site1.id.get.toString + "-22222222")) { rec =>
                rec.firstName === ""
                rec.middleName === None
                rec.lastName === ""
                rec.email === ""
                rec.deleted === false
                rec.userRole === UserRole.NORMAL
                rec.companyName === None
                PasswordHash.generate("pass002", rec.salt) === rec.passwordHash
              }
              doWith(map(site2.id.get.toString + "-11111111")) { rec =>
                rec.firstName === "first001"
                rec.middleName === Some("middle001")
                rec.lastName === "last001"
                rec.email === "email001"
                rec.deleted === false
                rec.userRole === UserRole.NORMAL
                rec.companyName === Some("company001")
              }
            }
          }
        }
      }
    }

    "Can change password" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          val user1 = StoreUser.create(
            "userName", "firstName", Some("middleName"), "lastName", "email",
            1L, 2L, UserRole.ADMIN, Some("companyName")
          )
          val user2 = StoreUser.create(
            "userName2", "firstName2", Some("middleName2"), "lastName2", "email2",
            111L, 222L, UserRole.ADMIN, Some("companyName2")
          )
          
          StoreUser.changePassword(user1.id.get, 123L, 234L)

          doWith(StoreUser(user1.id.get)) { u =>
            u.userName === "userName"
            u.passwordHash === 123L
            u.salt === 234L
          }
        }}
      }
    }
  }
}

