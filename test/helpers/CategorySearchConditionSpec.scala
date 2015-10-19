package helpers

import org.specs2.mutable._

class CategoryIdSearchConditionSpec extends Specification {
  "CategoryIdSearchCondition" should {
    "Can parse empty string" in {
      val c = CategoryIdSearchCondition("")
      c.condition.length === 0
    }

    "Can parse singe character" in {
      val c = CategoryIdSearchCondition("1")
      c.condition.length === 1
      c.condition(0).length === 1
      c.condition(0)(0) === 1
    }

    "Can parse a few digits" in {
      val c = CategoryIdSearchCondition("123")
      c.condition.length === 1
      c.condition(0).length === 1
      c.condition(0)(0) === 123
    }

    "Can parse a few categories" in {
      val c = CategoryIdSearchCondition("123,234")
      c.condition.length === 1
      c.condition(0).length === 2
      c.condition(0)(0) === 123
      c.condition(0)(1) === 234
    }

    "Can parse a few categories with and" in {
      val c = CategoryIdSearchCondition("123,234&9,8,7")
      c.condition.length === 2
      c.condition(0).length === 2
      c.condition(0)(0) === 123
      c.condition(0)(1) === 234
      c.condition(1).length === 3
      c.condition(1)(0) === 9
      c.condition(1)(1) === 8
      c.condition(1)(2) === 7
    }

    "Can parse a few categories with empty at end" in {
      val c = CategoryIdSearchCondition("123,234&")
      c.condition.length === 1
      c.condition(0).length === 2
      c.condition(0)(0) === 123
      c.condition(0)(1) === 234
    }

    "Can parse a few categories with empty at beginning" in {
      val c = CategoryIdSearchCondition("&123,234")
      c.condition.length === 1
      c.condition(0).length === 2
      c.condition(0)(0) === 123
      c.condition(0)(1) === 234
    }

    "Can parse a few categories with empty in the middle" in {
      val c = CategoryIdSearchCondition("123,234&&9")
      c.condition.length === 2
      c.condition(0).length === 2
      c.condition(0)(0) === 123
      c.condition(0)(1) === 234
      c.condition(1).length === 1
      c.condition(1)(0) === 9
    }

    "Can parse a empty condition with &" in {
      CategoryIdSearchCondition("&").condition.length === 0
      CategoryIdSearchCondition("&&").condition.length === 0
    }

    "Can detect error" in {
      CategoryIdSearchCondition("&,&") must throwA[IllegalArgumentException]
      CategoryIdSearchCondition(",") must throwA[IllegalArgumentException]
      CategoryIdSearchCondition(",1") must throwA[IllegalArgumentException]
      CategoryIdSearchCondition("1,") must throwA[IllegalArgumentException]
      CategoryIdSearchCondition("1&,") must throwA[IllegalArgumentException]
    }
  }
}
