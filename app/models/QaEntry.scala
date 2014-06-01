package models

case class QaEntry(
  qaType: String,
  comment: String,
  companyName: String,
  firstName: String,
  middleName: String,
  lastName: String,
  tel: String,
  email: String
)

object QaEntry {
  def apply4Japan(
    qaType: String,
    comment: String,
    companyName: String,
    firstName: String,
    lastName: String,
    tel: String,
    email: String
  ) = QaEntry(
    qaType,
    comment,
    companyName,
    firstName,
    "",
    lastName,
    tel,
    email
  )

  def unapply4Japan(qa: QaEntry): Option[(
    String, // qaType
    String, // comment
    String, // companyName
    String, // firstName
    String, // lastName
    String, // tel
    String // email
  )] = Some((qa.qaType,
             qa.comment,
             qa.companyName,
             qa.firstName,
             qa.lastName,
             qa.tel,
             qa.email
           ))

}
