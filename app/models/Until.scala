package models

object Until {
  val EverStr: String = "9999-12-31 23:59:59"
  val Ever: Long = java.sql.Timestamp.valueOf(EverStr).getTime
}
