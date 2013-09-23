package models

trait Prefecture {
  def code: Int
  def isUnknown = code == UnknownPrefecture.UNKNOWN.code
}
