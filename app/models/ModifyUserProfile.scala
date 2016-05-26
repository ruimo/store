package models

case class ModifyUserProfile(
  firstName: String,
  middleName: Option[String],
  lastName: String,
  email: String,
  password: String
)
