package models

import java.sql.Connection
import play.api.db.DB
import helpers.{PasswordHash, TokenGenerator, RandomTokenGenerator}
import play.api.Play.current

case class EntryUserRegistration(
  userName: String,
  passwords: (String, String),
  zip1: String,
  zip2: String,
  zip3: String,
  prefecture: Prefecture,
  address1: String,
  address2: String,
  address3: String,
  tel: String,
  fax: String,
  firstName: String,
  middleName: String,
  lastName: String,
  firstNameKana: String,
  lastNameKana: String,
  email: String
) {
  import EntryUserRegistration.tokenGenerator

  def isNaivePassword(implicit conn: Connection): Boolean =
    PasswordDictionary.isNaivePassword(passwords._1)

  def save(cc: CountryCode, stretchCount: Int)(implicit conn: Connection): StoreUser = {
    val salt = tokenGenerator.next
    val passwordHash = PasswordHash.generate(passwords._1, salt, stretchCount)

    val user = ExceptionMapper.mapException {
      StoreUser.create(
        userName, firstName, None, lastName,
        email, passwordHash, salt, UserRole.ENTRY_USER, None, stretchCount
      )
    }

    val addr = Address.createNew(
      countryCode = cc,
      firstName = firstName,
      middleName = middleName,
      lastName = lastName,
      firstNameKana = firstNameKana,
      lastNameKana = lastNameKana,
      zip1 = zip1,
      zip2 = zip2,
      zip3 = zip3,
      prefecture = prefecture,
      address1 = address1,
      address2 = address2,
      address3 = address3,
      tel1 = tel,
      tel2 = fax,
      email = email
    )

    UserAddress.createNew(user.id.get, addr.id.get)

    user
  }
}

object EntryUserRegistration {
  val tokenGenerator: TokenGenerator = RandomTokenGenerator()

  def apply4Japan(
    userName: String,
    passwords: (String, String),
    zip1: String,
    zip2: String,
    prefecture: Int,
    address1: String,
    address2: String,
    address3: String,
    tel: String,
    fax: String,
    firstName: String,
    lastName: String,
    firstNameKana: String,
    lastNameKana: String,
    email: String
  ) = EntryUserRegistration(
    userName,
    passwords,
    zip1,
    zip2,
    "",
    JapanPrefecture.byIndex(prefecture),
    address1,
    address2,
    address3,
    tel,
    fax,
    firstName,
    "",
    lastName,
    firstNameKana,
    lastNameKana,
    email
  )

  def unapply4Japan(ue: EntryUserRegistration): Option[(
    String, // userName
    (String, String), // passwords
    String, // zip1
    String, // zip2
    Int, // prefecture
    String, // address1
    String, // address2
    String, // address3
    String, // tel
    String, // fax
    String, // firstName
    String, // lastName
    String, // firstName
    String, // lastName
    String  // email
  )] = Some((ue.userName, ue.passwords,
             ue.zip1, ue.zip2,
             ue.prefecture.code,
             ue.address1, ue.address2, ue.address3,
             ue.tel, ue.fax,
             ue.firstName, ue.lastName,
             ue.firstNameKana, ue.lastNameKana,
             ue.email
           ))
}
