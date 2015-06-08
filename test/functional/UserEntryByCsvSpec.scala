package functional

import java.util.concurrent.TimeUnit
import play.api.test.Helpers._
import play.api.Play.current
import helpers.Helper._
import models._
import play.api.db.DB
import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import anorm._
import java.sql.Connection
import com.ruimo.scoins.Scoping._
import java.nio.file.{Files, Path}
import play.api.i18n.{Lang, Messages}
import org.openqa.selenium.By
import java.util.Arrays
import java.nio.charset.Charset
import helpers.PasswordHash

class UserEntryByCsvSpec extends Specification {
  "User entry by csv" should {
    "If file is not attached, error should be shown" in {
      val conf = inMemoryDatabase()
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        browser.goTo(
          "http://localhost:3333" + controllers.routes.UserMaintenance.startAddUsersByCsv().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.click("#submitUserScsv")
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.find(".globalErrorMessage").getText === Messages("file.not.found")

        SQL(
          "select count(*) from store_user"
        ).as(
          SqlParser.scalar[Long].single
        ) === 1
      }}      
    }

    "Empty csv is just ignored" in {
      val conf = inMemoryDatabase()
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val csvFile: Path = Files.createTempFile(null, ".csv")
        browser.goTo(
          "http://localhost:3333" + controllers.routes.UserMaintenance.startAddUsersByCsv().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.webDriver.findElement(By.id("usersCsv")).sendKeys(
          csvFile.toAbsolutePath.toString
        )
        browser.click("#submitUserScsv")
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("addUsersByCsv")
        browser.find(".message").getText === Messages("usersAreUpdated", 0, 0)

        SQL(
          "select count(*) from store_user"
        ).as(
          SqlParser.scalar[Long].single
        ) === 1
      }}      
    }

    "Users in csv should be registered" in {
      val conf = inMemoryDatabase()
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site = Site.createNew(LocaleInfo.Ja, "Site01")
        val csvFile: Path = Files.createTempFile(null, ".csv")
        Files.write(
          csvFile, 
          Arrays.asList(
            "CompanyId,EmployeeNo,Password",
            site.id.get + ",01234567,password0",
            ",98765432,password1"
          ),
          Charset.forName("Windows-31j")
        )
        browser.goTo(
          "http://localhost:3333" + controllers.routes.UserMaintenance.startAddUsersByCsv().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.webDriver.findElement(By.id("usersCsv")).sendKeys(
          csvFile.toAbsolutePath.toString
        )
        browser.click("#submitUserScsv")
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("addUsersByCsv")
        browser.find(".message").getText === Messages("usersAreUpdated", 2, 0)

        SQL(
          "select count(*) from store_user"
        ).as(
          SqlParser.scalar[Long].single
        ) === 3

        doWith(StoreUser.findByUserName(site.id.get + "-01234567").get) { user =>
          user.firstName === ""
          user.middleName === None
          user.lastName === ""
          user.email === ""
          user.passwordHash === PasswordHash.generate("password0", user.salt)
          user.deleted === false
          user.userRole === UserRole.NORMAL
          user.companyName === None

          doWith(Employee.getBelonging(user.id.get).get) { emp =>
            emp.siteId === site.id.get
            emp.userId === user.id.get
          }
        }

        doWith(StoreUser.findByUserName("98765432").get) { user =>
          user.firstName === ""
          user.middleName === None
          user.lastName === ""
          user.email === ""
          user.passwordHash === PasswordHash.generate("password1", user.salt)
          user.deleted === false
          user.userRole === UserRole.NORMAL
          user.companyName === None

          Employee.getBelonging(user.id.get) === None
        }
      }}      
    }

    "Users having invalid user name pattern should be detected." in {
      val conf = inMemoryDatabase() + ("normalUserNamePattern" -> "[0-9]{8}")
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site = Site.createNew(LocaleInfo.Ja, "Site01")
        val csvFile: Path = Files.createTempFile(null, ".csv")
        Files.write(
          csvFile, 
          Arrays.asList(
            "CompanyId,EmployeeNo,Password",
            site.id.get + ",12345678,password0",
            ",1234567,password1"
          ),
          Charset.forName("Windows-31j")
        )
        browser.goTo(
          "http://localhost:3333" + controllers.routes.UserMaintenance.startAddUsersByCsv().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.webDriver.findElement(By.id("usersCsv")).sendKeys(
          csvFile.toAbsolutePath.toString
        )
        browser.click("#submitUserScsv")
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("addUsersByCsv")
        browser.find(".globalErrorMessage").getText === Messages("normalUserNamePatternError") + "'1234567'"

        SQL(
          "select count(*) from store_user"
        ).as(
          SqlParser.scalar[Long].single
        ) === 1
      }}      
    }

    "Existing users should be skipped." in {
      val conf = inMemoryDatabase()
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site = Site.createNew(LocaleInfo.Ja, "Site01")
        val csvFile: Path = Files.createTempFile(null, ".csv")
        Files.write(
          csvFile, 
          Arrays.asList(
            "CompanyId,EmployeeNo,Password",
            site.id.get + ",11111111,password0",
            site.id.get + ",22222222,password0"
          ),
          Charset.forName("Windows-31j")
        )
        browser.goTo(
          "http://localhost:3333" + controllers.routes.UserMaintenance.startAddUsersByCsv().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.webDriver.findElement(By.id("usersCsv")).sendKeys(
          csvFile.toAbsolutePath.toString
        )
        browser.click("#submitUserScsv")
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("addUsersByCsv")
        browser.find(".message").getText === Messages("usersAreUpdated", 2, 0)

        SQL(
          "select count(*) from store_user"
        ).as(
          SqlParser.scalar[Long].single
        ) === 3
        SQL(
          "select count(*) from employee"
        ).as(
          SqlParser.scalar[Long].single
        ) === 2

        doWith(StoreUser.findByUserName(site.id.get + "-11111111").get) { user =>
          Employee.getBelonging(user.id.get).get.siteId === site.id.get
        }
        doWith(StoreUser.findByUserName(site.id.get + "-22222222").get) { user =>
          Employee.getBelonging(user.id.get).get.siteId === site.id.get
        }

        // 222 is deleted, 333 is added
        Files.write(
          csvFile, 
          Arrays.asList(
            "CompanyId,EmployeeNo,Password",
            site.id.get + ",11111111,password0",
            site.id.get + ",33333333,password0"
          ),
          Charset.forName("Windows-31j")
        )
        browser.goTo(
          "http://localhost:3333" + controllers.routes.UserMaintenance.startAddUsersByCsv().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.webDriver.findElement(By.id("usersCsv")).sendKeys(
          csvFile.toAbsolutePath.toString
        )
        browser.click("#submitUserScsv")
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.title === Messages("addUsersByCsv")
        browser.find(".message").getText === Messages("usersAreUpdated", 1, 1)
        
        SQL(
          "select count(*) from store_user"
        ).as(
          SqlParser.scalar[Long].single
        ) === 4
        SQL(
          "select count(*) from employee"
        ).as(
          SqlParser.scalar[Long].single
        ) === 3

        doWith(StoreUser.findByUserName(site.id.get + "-11111111").get) { user =>
          Employee.getBelonging(user.id.get).get.siteId === site.id.get
        }

        StoreUser.findByUserName(site.id.get + "-22222222") === None

        doWith(StoreUser.findByUserName(site.id.get + "-33333333").get) { user =>
          Employee.getBelonging(user.id.get).get.siteId === site.id.get
        }
      }}
    }

    "Users not in csv should be removed" in {
      val conf = inMemoryDatabase()
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site = Site.createNew(LocaleInfo.Ja, "Site01")
        val existingUser = StoreUser.create(
          userName = site.id.get + "-AAA",
          firstName = "", middleName = None, lastName = "",
          email = "", passwordHash = 0, salt = 0, userRole = UserRole.NORMAL, companyName = None
        )
        val csvFile: Path = Files.createTempFile(null, ".csv")
        Files.write(
          csvFile, 
          Arrays.asList(
            "CompanyId,EmployeeNo,Password",
            site.id.get + ",01234567,password0",
            ",98765432,password1"
          ),
          Charset.forName("Windows-31j")
        )
        browser.goTo(
          "http://localhost:3333" + controllers.routes.UserMaintenance.startAddUsersByCsv().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.webDriver.findElement(By.id("usersCsv")).sendKeys(
          csvFile.toAbsolutePath.toString
        )
        browser.click("#submitUserScsv")
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("addUsersByCsv")
        browser.find(".message").getText === Messages("usersAreUpdated", 2, 1)

        SQL(
          "select count(*) from store_user"
        ).as(
          SqlParser.scalar[Long].single
        ) === 4

        doWith(SQL("select * from store_user where user_name='" + site.id.get + "-AAA'").as(StoreUser.simple.single)) { user =>
          user.firstName === ""
          user.middleName === None
          user.lastName === ""
          user.email === ""
          user.passwordHash === 0
          user.salt === 0
          user.deleted === true
          user.userRole === UserRole.NORMAL
          user.companyName === None
        }

        doWith(StoreUser.findByUserName(site.id.get + "-01234567").get) { user =>
          user.firstName === ""
          user.middleName === None
          user.lastName === ""
          user.email === ""
          user.passwordHash === PasswordHash.generate("password0", user.salt)
          user.deleted === false
          user.userRole === UserRole.NORMAL
          user.companyName === None

          doWith(Employee.getBelonging(user.id.get).get) { emp =>
            emp.siteId === site.id.get
            emp.userId === user.id.get
          }
        }

        doWith(StoreUser.findByUserName("98765432").get) { user =>
          user.firstName === ""
          user.middleName === None
          user.lastName === ""
          user.email === ""
          user.passwordHash === PasswordHash.generate("password1", user.salt)
          user.deleted === false
          user.userRole === UserRole.NORMAL
          user.companyName === None
        }
      }}      
    }

    "If employee.csv.registration=true, user's company name is set to site name" in {
      val conf = inMemoryDatabase() + ("employee.csv.registration" -> true)
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site = Site.createNew(LocaleInfo.Ja, "Site01")
        val csvFile: Path = Files.createTempFile(null, ".csv")
        Files.write(
          csvFile, 
          Arrays.asList(
            "CompanyId,EmployeeNo,Password",
            site.id.get + ",01234567,password0",
            ",98765432,password1"
          ),
          Charset.forName("Windows-31j")
        )
        browser.goTo(
          "http://localhost:3333" + controllers.routes.UserMaintenance.startAddUsersByCsv().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.webDriver.findElement(By.id("usersCsv")).sendKeys(
          csvFile.toAbsolutePath.toString
        )
        browser.click("#submitUserScsv")
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("addUsersByCsv")
        browser.find(".message").getText === Messages("usersAreUpdated", 2, 0)

        SQL(
          "select count(*) from store_user"
        ).as(
          SqlParser.scalar[Long].single
        ) === 3

        doWith(StoreUser.findByUserName(site.id.get + "-01234567").get) { user =>
          user.firstName === ""
          user.middleName === None
          user.lastName === ""
          user.email === ""
          user.passwordHash === PasswordHash.generate("password0", user.salt)
          user.deleted === false
          user.userRole === UserRole.NORMAL
          user.companyName === Some(site.name)
        }

        doWith(StoreUser.findByUserName("98765432").get) { user =>
          user.firstName === ""
          user.middleName === None
          user.lastName === ""
          user.email === ""
          user.passwordHash === PasswordHash.generate("password1", user.salt)
          user.deleted === false
          user.userRole === UserRole.NORMAL
          user.companyName === None
        }
      }}      
    }

    "Duplicated user should be reported" in {
      val conf = inMemoryDatabase()
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.FIREFOX) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val user = loginWithTestUser(browser)
        val site = Site.createNew(LocaleInfo.Ja, "Site01")
        val csvFile: Path = Files.createTempFile(null, ".csv")
        Files.write(
          csvFile, 
          Arrays.asList(
            "CompanyId,EmployeeNo,Password",
            site.id.get + ",01234567,password0",
            site.id.get + ",01234567,password1"
          ),
          Charset.forName("Windows-31j")
        )
        browser.goTo(
          "http://localhost:3333" + controllers.routes.UserMaintenance.startAddUsersByCsv().url + "?lang=" + lang.code
        )
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()
        browser.webDriver.findElement(By.id("usersCsv")).sendKeys(
          csvFile.toAbsolutePath.toString
        )
        browser.click("#submitUserScsv")
        browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded()

        browser.title === Messages("addUsersByCsv")
        browser.find(".globalErrorMessage").getText === Messages("userNameDuplicated", "01234567")
      }}      
    }
  }
}
