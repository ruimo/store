package models

case class CreateEmployee(
  userName: String,
  passwords: (String, String)
) {
  def password: String = passwords._1
}


