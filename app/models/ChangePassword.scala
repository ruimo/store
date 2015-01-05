package models

import anorm._
import anorm.SqlParser
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import java.sql.Connection
import helpers.RandomTokenGenerator
import helpers.{PasswordHash, TokenGenerator, RandomTokenGenerator}

case class ChangePassword(
  currentPassword: String,
  passwords: (String, String)
) {
  def changePassword(storeUserId: Long)(implicit conn: Connection): Boolean = {
    val salt = ChangePassword.tokenGenerator.next
    StoreUser.changePassword(storeUserId, PasswordHash.generate(passwords._1, salt), salt) != 0
  }
}

object ChangePassword {
  val tokenGenerator: TokenGenerator = RandomTokenGenerator()
}
