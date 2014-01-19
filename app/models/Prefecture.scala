package models

trait Prefecture {
  def code: Int
  def isUnknown = code == UnknownPrefecture.UNKNOWN.code
}

object Prefecture {
  val table = Map(
    CountryCode.JPN -> JapanPrefecture.all().toList
  )
}
