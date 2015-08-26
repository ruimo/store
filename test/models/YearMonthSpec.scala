package models

import org.specs2.mutable._

class YearMonthSpec extends Specification {
  "YearMonth" should {
    "next returns next month." in {
      YearMonth(1999, 1, "show").next === YearMonth(1999, 2, "show")
      YearMonth(1999, 12, "csv").next === YearMonth(2000, 1, "csv")
    }
  }
}
