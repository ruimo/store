package helpers

import org.specs2.mutable._

class CategorySearchConditionSpec extends Specification {
  "CategorySearchCondition" should {
    "Can parse empty string" in {
      val c = CategorySearchCondition("")
      c.condition.length === 0
    }

    "Can parse singe character" in {
      val c = CategorySearchCondition("1")
      c.condition.length === 1
      c.condition(0).length === 1
      c.condition(0)(0) === 1
    }

    "Can parse a few digits" in {
      val c = CategorySearchCondition("123")
      c.condition.length === 1
      c.condition(0).length === 1
      c.condition(0)(0) === 123
    }

    "Can parse a few categories" in {
      val c = CategorySearchCondition("123,234")
      c.condition.length === 1
      c.condition(0).length === 2
      c.condition(0)(0) === 123
      c.condition(0)(1) === 234
    }

    "Can parse a few categories with and" in {
      val c = CategorySearchCondition("123,234&9,8,7")
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
      val c = CategorySearchCondition("123,234&")
      c.condition.length === 1
      c.condition(0).length === 2
      c.condition(0)(0) === 123
      c.condition(0)(1) === 234
    }

    "Can parse a few categories with empty at beginning" in {
      val c = CategorySearchCondition("&123,234")
      c.condition.length === 1
      c.condition(0).length === 2
      c.condition(0)(0) === 123
      c.condition(0)(1) === 234
    }

    "Can parse a few categories with empty in the middle" in {
      val c = CategorySearchCondition("123,234&&9")
      c.condition.length === 2
      c.condition(0).length === 2
      c.condition(0)(0) === 123
      c.condition(0)(1) === 234
      c.condition(1).length === 1
      c.condition(1)(0) === 9
    }

    "Can parse a empty condition with &" in {
      CategorySearchCondition("&").condition.length === 0
      CategorySearchCondition("&&").condition.length === 0
    }

    "Can detect error" in {
      CategorySearchCondition("&,&") must throwA[IllegalArgumentException]
      CategorySearchCondition(",") must throwA[IllegalArgumentException]
      CategorySearchCondition(",1") must throwA[IllegalArgumentException]
      CategorySearchCondition("1,") must throwA[IllegalArgumentException]
      CategorySearchCondition("1&,") must throwA[IllegalArgumentException]
    }
  }
}
