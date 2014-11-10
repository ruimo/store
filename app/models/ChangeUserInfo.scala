package models

import java.sql.Connection

case class ChangeUserInfo(
  firstName: String, middleName: Option[String], lastName: String,
  firstNameKana: String, lastNameKana: String,
  email: String, 
  currentPassword: String,
  countryIndex: Int,
  zip1: String, zip2: String,
  prefectureIndex: Int,
  address1: String,
  address2: String,
  address3: String,
  tel1: String
) {
  lazy val countryCode = CountryCode.byIndex(countryIndex)
}
