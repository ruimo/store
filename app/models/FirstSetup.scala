package models

case class FirstSetup(
  userName: String, firstName: String, lastName: String, email: String,
  password: String, passwordConfirm: String
) extends NotNull {
  def passwordMatch = (password == passwordConfirm)
}
