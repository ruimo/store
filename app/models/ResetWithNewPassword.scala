package models

case class ResetWithNewPassword(
  userId: Long,
  token: Long,
  passwords: (String, String)
)
