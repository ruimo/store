package models;

import scala.collection.immutable

object PaypalCountry {
  val map = immutable.Map[CountryCode, String](
    CountryCode.JPN -> "JP"
  )

  def apply(cc: CountryCode): Option[String] = map.get(cc)
}
