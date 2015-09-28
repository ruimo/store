package helpers

import scala.collection.immutable.Vector
import scala.collection.immutable
import scala.annotation.tailrec

// Parse the syntax:
//    +-----------------------------+
//    |                             V
// -->+-->+--> or expression -->+-->+-->
//        A                     |
//        +<-- & <--------------+
//
// or expression:
//    +--------------------------+
//    |                          V
// -->-->+--> category id -->+-->+-->
//       A                   |
//       +<-- , <------------+
//
// Example:
// 1,2
// id = 1 or id = 2
//
// 1,2&3,4,5
// (id = 1 or id = 2) and (id = 3 or id = 4 or id = 5)
// For example, items having categories both id = 1 and id = 3 will be hit.
// Items having categories both id = 1 and id = 2 will not be hit.
case class CategorySearchCondition(
  condition: immutable.Seq[immutable.Seq[Long]]
)

object CategorySearchCondition {
  val Null = CategorySearchCondition(Vector[immutable.Seq[Long]]())

  def apply(categoryIds: Long*): CategorySearchCondition =
    CategorySearchCondition(Vector(categoryIds.toVector))

  def apply(in: String): CategorySearchCondition = {
    @tailrec def parse(
      idx: Int, charBuf: StringBuilder, orBuf: Vector[Long], andBuf: Vector[immutable.Seq[Long]]
    ): CategorySearchCondition = {
      if (in.endsWith(","))
        throw new IllegalArgumentException("Input string ends with comma '" + in + "'.")
      if (idx >= in.length)
        if (charBuf.length != 0)
          CategorySearchCondition(andBuf :+ (orBuf :+ charBuf.toString.toLong))
        else
          CategorySearchCondition(andBuf)
      else {
        val c = in.charAt(idx)
        if (isNum(c))
          parse(idx + 1, charBuf.append(c), orBuf, andBuf)
        else if (c == ',') {
          val newOrBuf = orBuf :+ charBuf.toString.toLong
          charBuf.setLength(0)
          parse(idx + 1, charBuf, newOrBuf, andBuf)
        }
        else if (c == '&') {
          if (charBuf.length != 0) {
            val newOrBuf = orBuf :+ charBuf.toString.toLong
            charBuf.setLength(0)
            parse(idx + 1, charBuf, Vector[Long](), andBuf :+ newOrBuf)
          }
          else
            parse(idx + 1, charBuf, orBuf, andBuf)
        }
        else
          throw new IllegalArgumentException(
            "Invalid character '" + c + "' around index = " + idx + " in '" + in + "'."
          )
      }
    }

    parse(0, new StringBuilder, Vector[Long](), Vector[immutable.Seq[Long]]())
  }

  def isNum(c: Char): Boolean = '0' <= c && c <= '9'
}
