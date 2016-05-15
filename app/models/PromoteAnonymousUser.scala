package models

import java.sql.Connection
import play.api.Play.current

case class PromoteAnonymousUser(
  userName: String,
  passwords: (String, String)
) {
  def isNaivePassword(implicit conn: Connection): Boolean =
    PasswordDictionary.isNaivePassword(passwords._1)

  def update(login: LoginSession)(implicit conn: Connection): Boolean =
    ExceptionMapper.mapException(
      login.storeUser.promoteAnonymousUser(userName, passwords._1)
    )
}
