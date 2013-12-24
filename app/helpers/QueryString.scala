package helpers

import collection.mutable.ArrayBuffer

case class QueryString(table: Seq[String]) {
  require(table != null)

  lazy val size = table.size
  lazy val toList = table.toList
  override def toString =
    if (table.isEmpty) ""
    else if (table.size == 1) table.head
    else table.map {e => "\"" + e + "\""}.mkString(" ")
}

object QueryString {
  def apply(qs: String): QueryString = QueryString(Parser.parse(qs))
  def apply(): QueryString = QueryString(List())

  private object Parser {
    trait State
    case object Init extends State
    case object Text extends State
    case object Quote extends State

    def parse(s: String): Seq[String] = {
      var state: State = Init
      val buf = new StringBuilder
      val tokenBuf = new ArrayBuffer[String]

      s.foreach { c =>
        state match {
          case Init =>
            if (c == '"') {
              if (! buf.isEmpty) {
                tokenBuf += buf.toString
                buf.clear()
              }
              state = Quote
            }
            else if (c == ' ') {
              // Ignore
            }
            else {
              state = Text
              buf.append(c)
            }

          case Text =>
            if (c == ' ') {
              if (! buf.isEmpty) {
                tokenBuf += buf.toString
                buf.clear()
              }

              state = Init
            }
            else if (c == '"') {
              if (! buf.isEmpty) {
                tokenBuf += buf.toString
                buf.clear()
              }

              state = Quote
            }
            else {
              buf.append(c)
            }

          case Quote =>
            if (c == '"') {
              tokenBuf += buf.toString
              buf.clear()
              state = Init
            }
            else {
              buf.append(c)
            }
        }
      }

      if (! buf.isEmpty)
        tokenBuf += buf.toString

      tokenBuf.toSeq
    }
  }
}
