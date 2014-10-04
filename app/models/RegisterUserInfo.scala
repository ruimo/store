package models

import java.sql.Connection

case class RegisterUserInfo(
  firstName: String, middleName: Option[String], lastName: String,
  firstNameKana: String, lastNameKana: String,
  email: String, 
  currentPassword: String,
  passwords: (String, String),
  countryCode: CountryCode,
  zip1: String, zip2: String,
  prefecture: Prefecture,
  address1: String,
  address2: String,
  address3: String,
  tel1: String
) {
  def currentPasswordNotMatch(storeUser: StoreUser)(implicit conn: Connection): Boolean =
    storeUser.passwordMatch(currentPassword)

  def isNaivePassword(implicit conn: Connection): Boolean =
    PasswordDictionary.isNaivePassword(passwords._1)
}

object RegisterUserInfo {
  def apply4Japan(
    firstName: String, middleName: Option[String], lastName: String,
    firstNameKana: String, lastNameKana: String,
    email: String,
    currentPassword: String,
    passwords: (String, String),
    countryCode: Int,
    zip1: String, zip2: String,
    prefecture: Int,
    address1: String,
    address2: String,
    address3: String,
    tel1: String
  ) = RegisterUserInfo(
    firstName, None, lastName,
    firstNameKana, lastNameKana,
    email, currentPassword, passwords, CountryCode.byIndex(countryCode), zip1, zip2,
    JapanPrefecture.byIndex(prefecture),
    address1, address2, address3,
    tel1
  )

  def unapply4Japan(info: RegisterUserInfo): Option[(
    String, // firstName
    Option[String], // middleName
    String, // lastName
    String, // firstNameKana
    String, // lastNameKana
    String, // email
    String, // currentPassword
    (String, String), // passwords
    Int, // country code
    String, // zip1
    String, // zip2
    Int, // prefecture code
    String, // address1
    String, // address2
    String, // address3
    String  // tel1
  )] = Some((
    info.firstName, None, info.lastName,
    info.firstNameKana, info.lastNameKana,
    info.email, info.currentPassword, info.passwords,
    info.countryCode.ordinal,
    info.zip1, info.zip2,
    info.prefecture.code, 
    info.address1, info.address2, info.address3,
    info.tel1
  ))
}

