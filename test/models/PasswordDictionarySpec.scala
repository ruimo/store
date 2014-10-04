package models

import org.specs2.mutable._

import anorm._
import anorm.SqlParser
import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current
import anorm.Id

class PasswordDictionarySpec extends Specification {
  "Password dictionary spec" should {
    "Invalid password is detected" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withConnection { implicit conn => {
          PasswordDictionary.isNaivePassword("password") === false

          SQL(
            " insert into password_dict (password) values ('password')"
          ).executeUpdate()
          PasswordDictionary.isNaivePassword("password") === true
        }}
      }
    }
  }
}

