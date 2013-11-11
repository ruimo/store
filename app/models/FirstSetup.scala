package models

import helpers.{PasswordHash, TokenGenerator}
import java.security.MessageDigest
import java.sql.Connection

case class FirstSetup(
  userName: String, firstName: String, middleName: Option[String], lastName: String,
  email: String, password: String, companyName: String
) extends CreateUser with NotNull {
  val role = UserRole.ADMIN
}

object FirstSetup extends CreateUserObject {
  def fromForm(
    userName: String, firstName: String, middleName: Option[String], lastName: String, email: String, passwords: (String, String), companyName: String
  ): FirstSetup =
    FirstSetup(userName, firstName, middleName, lastName, email, passwords._1, companyName)
}
