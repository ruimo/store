package models

import anorm._
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import java.sql.Connection

object PasswordDictionary {
  def isNaivePassword(password: String)(implicit conn: Connection) = 
    SQL(
      "select count(*) from password_dict where password = {password}"
    ).on(
      'password -> password
    ).as(SqlParser.scalar[Long].single) != 0
}
