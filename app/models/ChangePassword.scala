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
)
