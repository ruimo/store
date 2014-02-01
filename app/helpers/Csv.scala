package helpers

import java.io.Writer

class Csv(fieldNames: List[String]) {
  def this(fieldNames: String*) = this(fieldNames.toList)
  val header = fieldNames.map(CsvField.toField).mkString(",")

  def createWriter(writer: Writer): CsvWriter = {
    writer.write(header)
    createWriterWithoutHeader(writer)
  }

  def createWriterWithoutHeader(writer: Writer): CsvWriter = new CsvWriter(writer)
}

class CsvWriter(writer: Writer) {
  def print(record: String*): CsvWriter = printSeq(record)

  def printSeq(record: Seq[String]): CsvWriter = {
    writer.write(record.map(CsvField.toField).mkString(","))
    this
  }
}

object CsvField {
  def toField(fld: String): String = escapeIfNeeded(fld).toString

  private[helpers] abstract class State(initialString: String) {
    protected val buf: StringBuilder = new StringBuilder(initialString)
    def next(c: Char): State
  }

  private[helpers] case class NormalState(initialString: String) extends State(initialString) {
    def next(c: Char): State =
      if (c == '"') {
        buf.append("\"\"")
        NeedEscapeState(buf.toString)
      }
      else if (c == ',') {
        buf.append(c)
        NeedEscapeState(buf.toString)
      }
      else if (c < ' ') {
        buf.append(c)
        NeedEscapeState(buf.toString)
      }
      else {
        buf.append(c)
        this
      }

    override def toString = buf.toString
  }

  private[helpers] case class NeedEscapeState(initialString: String) extends State(initialString) {
    def next(c: Char): State = {
      buf.append(
        if (c == '"') "\"\"" else c
      )
      this
    }

    override def toString = "\"" + buf.toString() + "\""
  }

  def escapeIfNeeded(fld: String): State = {
    val state: State = if (fld.startsWith(" ") || fld.endsWith(" ")) NeedEscapeState("") else NormalState("")

    fld.foldLeft(state) { (sum, c) => sum.next(c) }
  }
}
