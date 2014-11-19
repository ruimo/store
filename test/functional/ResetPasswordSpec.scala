package functional

import play.api.test._
import play.api.test.Helpers._
import play.api.Play.current
import java.sql.Connection

import helpers.Helper._

import org.specs2.mutable.Specification
import play.api.test.{Helpers, TestServer, FakeApplication}
import play.api.i18n.{Lang, Messages}
import play.api.db.DB
import play.api.test.TestServer
import play.api.test.FakeApplication
import helpers.Helper.disableMailer
import helpers.{PasswordHash, TokenGenerator, RandomTokenGenerator}
import models.StoreUser
import models.UserRole

class ResetPasswordSpec extends Specification {
  val conf = inMemoryDatabase() ++ disableMailer

  "Reset password" should {
    "Can reset password" in {
      val app = FakeApplication(additionalConfiguration = conf)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        implicit val lang = Lang("ja")
        val salt = RandomTokenGenerator().next
        val hash = PasswordHash.generate("password", salt)

        val user = StoreUser.create(
          "userName", "firstName", Some("middleName"), "lastName", "email",
          hash, salt, UserRole.NORMAL, Some("companyName")
        )

        
      }}
    }
  }
}
