package models

import helpers.{PasswordHash, TokenGenerator}
import java.security.MessageDigest
import com.google.common.primitives.Longs

case class FirstSetup(
  userName: String, firstName: String, lastName: String, email: String, password: String
) extends NotNull {
  def save(implicit tokenGenerator: TokenGenerator): StoreUser = {
    val salt = tokenGenerator.next
    val hash = PasswordHash.generate(password, salt)
    StoreUser.create(userName, firstName, lastName, email, hash, salt, UserRole.ADMIN)
  }
}

object FirstSetup {
  def fromForm(
    userName: String, firstName: String, lastName: String, email: String, passwords: (String, String)
  ): FirstSetup =
    FirstSetup(userName, firstName, lastName, email, passwords._1)

  def toForm(m: FirstSetup) = Some(
    m.userName, m.firstName, m.lastName, m.email, (m.password, m.password)
  )
}
