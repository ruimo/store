package models

case class CreatePrize(
  countryCode: CountryCode,
  firstName: String,
  middleName: String,
  lastName: String,
  zip1: String,
  zip2: String,
  zip3: String,
  prefecture: Prefecture,
  address1: String,
  address2: String,
  address3: String,
  address4: String,
  address5: String,
  tel: String,
  comment: String,
  command: String
)

object CreatePrize {
  def apply4Japan(
    firstName: String,
    lastName: String,
    zip1: String,
    zip2: String,
    prefecture: Int,
    address1: String,
    address2: String,
    address3: String,
    address4: String,
    address5: String,
    tel: String,
    comment: String,
    command: String
  ) = CreatePrize(
    CountryCode.JPN,
    firstName,
    "",
    lastName,
    zip1,
    zip2,
    "",
    JapanPrefecture.byIndex(prefecture),
    address1,
    address2,
    address3,
    address4,
    address5,
    tel,
    comment,
    command
  )

  def unapply4Japan(prize: CreatePrize): Option[(
    String, String,
    String, String,
    Int,
    String, String, String, String, String,
    String, String, String
  )] = Some(
    (
      prize.firstName, prize.lastName,
      prize.zip1, prize.zip2,
      prize.prefecture.code,
      prize.address1, prize.address2, prize.address3, prize.address4, prize.address5,
      prize.tel, prize.comment, prize.command
    )
  )
}


