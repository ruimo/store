package models

import helpers.{PasswordHash, TokenGenerator}
import java.security.MessageDigest
import com.google.common.primitives.Longs
import java.sql.Connection

case class FirstSetup(
  userName: String, firstName: String, middleName: Option[String], lastName: String, email: String, password: String
) extends NotNull {
  def save(implicit tokenGenerator: TokenGenerator, conn: Connection): StoreUser = {
    val salt = tokenGenerator.next
    val hash = PasswordHash.generate(password, salt)
    StoreUser.create(userName, firstName, middleName, lastName, email, hash, salt, UserRole.ADMIN)
  }
}

object FirstSetup {
  def fromForm(
    userName: String, firstName: String, middleName: Option[String], lastName: String, email: String, passwords: (String, String)
  ): FirstSetup =
    FirstSetup(userName, firstName, middleName, lastName, email, passwords._1)

  def toForm(m: FirstSetup) = Some(
    m.userName, m.firstName, m.middleName, m.lastName, m.email, (m.password, m.password)
  )
}
