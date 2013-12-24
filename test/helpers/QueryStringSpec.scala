package helpers

import org.specs2.mutable._

class QueryStringSpec extends Specification {
  "QueryStringSpec" should {
    "toString create string representation." in {
      QueryString(List()).toString === ""
      QueryString(List("Hello")).toString === "Hello"
      QueryString(List("Hello", "World")).toString === "\"Hello\" \"World\""
    }

    "Create by empty string." in {
      QueryString("").toList === List()
    }

    "Create by single string." in {
      QueryString("Hello").toList === List("Hello")
    }

    "Create by two words." in {
      QueryString("Hello World").toList === List("Hello", "World")
    }

    "Create by quoted single word." in {
      QueryString("\"Hello World\"").toList === List("Hello World")
    }

    "Create by quoted two words." in {
      QueryString("\"Hello\" \"World\"").toList === List("Hello", "World")
    }

    "Create by non-terminated word." in {
      QueryString("\"Hello\" \"World").toList === List("Hello", "World")
    }

    "Create by mixed words." in {
      QueryString("\"Hello\" World").toList === List("Hello", "World")
      QueryString("Hello \"World\"").toList === List("Hello", "World")
    }
  }
}
