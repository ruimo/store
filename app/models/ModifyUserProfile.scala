package models

import java.sql.Connection
import helpers.PasswordHash

case class ModifyUserProfile(
  firstName: String,
  middleName: Option[String],
  lastName: String,
  email: String,
  password: String
) {
  def save(login: LoginSession)(implicit conn: Connection) {
    val salt = EntryUserRegistration.tokenGenerator.next
    val stretchCount: Int = StoreUser.PasswordHashStretchCount()
    val passwordHash = PasswordHash.generate(password, salt, stretchCount)

    StoreUser.update(
      login.storeUser.copy(
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        email = email,
        salt = salt,
        passwordHash = passwordHash,
        stretchCount = stretchCount
      )
    )
  }
}

object ModifyUserProfile {
  def apply(login: LoginSession): ModifyUserProfile = ModifyUserProfile(
    login.storeUser.firstName,
    login.storeUser.middleName,
    login.storeUser.lastName,
    login.storeUser.email,
    ""
  )
}
