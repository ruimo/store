package models

trait CreateUserBase {
  val userName: String
  val firstName: String
  val middleName: Option[String]
  val lastName: String
  val email: String
  val supplementalEmails: Seq[String]
  val password: String
  val companyName: String
}
