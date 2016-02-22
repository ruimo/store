package models

import helpers.{PasswordHash, TokenGenerator}
import java.security.MessageDigest
import java.sql.Connection

case class CreateNormalUser(
  userName: String, firstName: String, middleName: Option[String], lastName: String,
  email: String, supplementalEmails: Seq[String],
  password: String, companyName: String
) extends CreateUser {
  val role = UserRole.NORMAL
}

object CreateNormalUser extends CreateUserObject {
  def fromForm(
    userName: String, firstName: String, middleName: Option[String], lastName: String,
    email: String, supplementalEmails: Seq[Option[String]], 
    passwords: (String, String), companyName: String
  ): CreateNormalUser =
    CreateNormalUser(
      userName, firstName, middleName, lastName, email, 
      supplementalEmails.filter(_.isDefined).map(_.get).toList,
      passwords._1, companyName
    )
}
