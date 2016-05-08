package models

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
  email: String
) {
  import EntryUserRegistration.tokenGenerator

  def save(stretchCount: Int): Unit = DB.withConnection { implicit conn =>
    val salt = tokenGenerator.next
    val passwordHash = PasswordHash.generate(passwords._1, salt)

    val user = StoreUser.create(
      userName, firstName, None, lastName,
      email, passwordHash, salt, UserRole.ENTRY_USER, None, stretchCount
    )
    // TODO address
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
    String  // email
  )] = Some((ue.userName, ue.passwords,
             ue.zip1, ue.zip2,
             ue.prefecture.code,
             ue.address1, ue.address2, ue.address3,
             ue.tel, ue.fax,
             ue.firstName, ue.lastName,
             ue.email
           ))
}
