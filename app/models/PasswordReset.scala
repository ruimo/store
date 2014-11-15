package models

case class PasswordReset(
  companyId: Option[String], userName: String
) {
  lazy val compoundUserName: String = companyId.map(_ + "-").getOrElse("") + userName
}
