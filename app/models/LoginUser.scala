package models

case class LoginUser(
  userName: String, password: String, uri: String
) extends NotNull
