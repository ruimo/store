package models

case class LoginUser(
  companyId: Option[String], userName: String, password: String, uri: String
) {
  lazy val compoundUserName: String = companyId.map(_ + "-").getOrElse("") + userName
}
