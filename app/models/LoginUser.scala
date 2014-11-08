package models

case class LoginUser(
  companyId: Option[String], userName: String, password: String, uri: String
) extends NotNull {
  lazy val compoundUserName: String = companyId.map(_ + "-").getOrElse("") + userName
}
