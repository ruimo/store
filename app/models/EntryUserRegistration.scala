package models

case class EntryUserRegistration(
  companyName: Option[String],
  zip1: String,
  zip2: String,
  zip3: String,
  prefecture: Prefecture,
  address1: String,
  address2: String,
  address3: String,
  tel: String,
  fax: String,
  title: String,
  firstName: String,
  middleName: String,
  lastName: String,
  email: String
)

object EntryUserRegistration {
  def apply4Japan(
    companyName: Option[String],
    zip1: String,
    zip2: String,
    prefecture: Int,
    address1: String,
    address2: String,
    address3: String,
    tel: String,
    fax: String,
    title: String,
    firstName: String,
    lastName: String,
    email: String
  ) = EntryUserRegistration(
    companyName,
    zip1,
    zip2,
    "",
    JapanPrefecture.byIndex(prefecture),
    address1,
    address2,
    address3,
    tel,
    fax,
    title,
    firstName,
    "",
    lastName,
    email
  )

  def unapply4Japan(ue: EntryUserRegistration): Option[(
    Option[String], // companyName
    String, // zip1
    String, // zip2
    Int, // prefecture
    String, // address1
    String, // address2
    String, // address3
    String, // tel
    String, // fax
    String, // title
    String, // firstName
    String, // lastName
    String  // email
  )] = Some((ue.companyName,
             ue.zip1, ue.zip2,
             ue.prefecture.code,
             ue.address1, ue.address2, ue.address3,
             ue.tel, ue.fax,
             ue.title,
             ue.firstName, ue.lastName,
             ue.email
           ))
}
