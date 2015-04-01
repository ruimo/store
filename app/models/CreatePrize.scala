package models

case class CreatePrize(
  countryCode: CountryCode,
  firstName: String,
  middleName: String,
  lastName: String,
  firstNameKana: String,
  lastNameKana: String,
  zip: (String, String, String),
  prefecture: Prefecture,
  address1: String,
  address2: String,
  address3: String,
  address4: String,
  address5: String,
  tel: String,
  comment: String,
  command: String,
  age: String,
  sex: Sex
)

object CreatePrize {
  def apply4Japan(
    firstName: String,
    lastName: String,
    firstNameKana: String,
    lastNameKana: String,
    zip: (String, String),
    prefecture: Int,
    address1: String,
    address2: String,
    address3: String,
    address4: String,
    address5: String,
    tel: String,
    comment: String,
    command: String,
    age: String,
    sex: Int
  ) = CreatePrize(
    CountryCode.JPN,
    firstName,
    "",
    lastName,
    firstNameKana,
    lastNameKana,
    (zip._1, zip._2, ""),
    JapanPrefecture.byIndex(prefecture),
    address1,
    address2,
    address3,
    address4,
    address5,
    tel,
    comment,
    command,
    age,
    Sex.byIndex(sex)
  )

  def unapply4Japan(prize: CreatePrize): Option[(
    String, String,
    String, String,
    (String, String),
    Int,
    String, String, String, String, String,
    String, String, String,
    String, Int
  )] = Some(
    (
      prize.firstName, prize.lastName,
      prize.firstNameKana, prize.lastNameKana,
      (prize.zip._1, prize.zip._2),
      prize.prefecture.code,
      prize.address1, prize.address2, prize.address3, prize.address4, prize.address5,
      prize.tel, prize.comment, prize.command,
      prize.age, prize.sex.ordinal
    )
  )
}


