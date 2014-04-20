package models

import org.specs2.mutable._

class YearMonthSpec extends Specification {
  "YearMonth" should {
    "next returns next month." in {
      YearMonth(1999, 1).next === YearMonth(1999, 2)
      YearMonth(1999, 12).next === YearMonth(2000, 1)
    }
  }
}
