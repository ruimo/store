package models

import helpers.{PasswordHash, TokenGenerator}
import java.security.MessageDigest
import java.sql.Connection

case class CreateNormalUser(
  userName: String, firstName: String, middleName: Option[String], lastName: String, email: String, password: String
) extends CreateUser with NotNull {
  val role = UserRole.NORMAL
}

object CreateNormalUser extends CreateUserObject {
  def fromForm(
    userName: String, firstName: String, middleName: Option[String], lastName: String, email: String, passwords: (String, String)
  ): CreateNormalUser =
    CreateNormalUser(userName, firstName, middleName, lastName, email, passwords._1)
}
