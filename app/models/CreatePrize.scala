package models

case class CreatePrize(
  firstName: String,
  lastName: String,
  zip1: String,
  zip2: String,
  prefectureCode: Int,
  address1: String,
  address2: String,
  address3: String,
  address4: String,
  address5: String,
  tel: String,
  comment: String
)
