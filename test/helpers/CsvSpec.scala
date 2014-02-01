package helpers

import org.specs2.mutable._
import java.io.StringWriter

class CsvSpec extends Specification {
  "Csv" should {
    "Has header" in {
      val csv = new Csv("A", "B,C")
      csv.header === "A,\"B,C\""
    }

    "Can write csv." in {
      val writer = new StringWriter
      val csv = new Csv("A", "B,C")
      val csvWriter = csv.createWriter(writer)
    }
  }
}

class CsvFieldSpec extends Specification {
  "CsvField" should {
    "Empty field should empty." in {
      CsvField.toField("") === ""
    }

    "ABC should remain unchanged." in {
      CsvField.toField("ABC") === "ABC"
    }

    "Escape string starts with space." in {
      CsvField.toField(" ABC") === "\" ABC\""
      CsvField.toField("ABC ") === "\"ABC \""
    }

    "Escape double quote." in {
      CsvField.toField("A\"B") === "\"A\"\"B\""
      CsvField.toField("A\"B\"C") === "\"A\"\"B\"\"C\""
    }

    "Escape CRLF." in {
      CsvField.toField("A\r\nB") === "\"A\r\nB\""
    }

    "Escape comma." in {
      CsvField.toField("A,B,C") === "\"A,B,C\""
    }
  }
}
