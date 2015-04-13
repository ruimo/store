package models

case class CreateQaSite(
  command: String,
  companyName: String,
  name: String,
  tel: String,
  email: String,
  inquiryBody: String
)
