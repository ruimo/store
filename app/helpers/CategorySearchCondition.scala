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
case class CategoryIdSearchCondition(
  condition: immutable.Seq[immutable.Seq[Long]]
)

case class CategoryCodeSearchCondition(
  condition: immutable.Seq[immutable.Seq[String]]
)

object CategorySearchCondition {
  def parseInput[T](
    in: String,
    strToData: String => T,
    isDataChar: Char => Boolean
  ): immutable.Seq[immutable.Seq[T]] = {
    @tailrec def parse(
      idx: Int, charBuf: StringBuilder, orBuf: Vector[T], andBuf: Vector[immutable.Seq[T]]
    ): immutable.Seq[immutable.Seq[T]] = {
      if (in.endsWith(","))
        throw new IllegalArgumentException("Input string ends with comma '" + in + "'.")
      if (idx >= in.length)
        if (charBuf.length != 0)
          andBuf :+ (orBuf :+ strToData(charBuf.toString))
        else
          andBuf
      else {
        val c = in.charAt(idx)
        if (isDataChar(c))
          parse(idx + 1, charBuf.append(c), orBuf, andBuf)
        else if (c == ',') {
          val newOrBuf = orBuf :+ strToData(charBuf.toString)
          charBuf.setLength(0)
          parse(idx + 1, charBuf, newOrBuf, andBuf)
        }
        else if (c == '&') {
          if (charBuf.length != 0) {
            val newOrBuf = orBuf :+ strToData(charBuf.toString)
            charBuf.setLength(0)
            parse(idx + 1, charBuf, Vector[T](), andBuf :+ newOrBuf)
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

    parse(0, new StringBuilder, Vector[T](), Vector[immutable.Seq[T]]())
  }
}

object CategoryIdSearchCondition {
  val Null = CategoryIdSearchCondition(Vector[immutable.Seq[Long]]())

  def apply(categoryIds: Long*): CategoryIdSearchCondition =
    CategoryIdSearchCondition(Vector(categoryIds.toVector))

  def apply(in: String): CategoryIdSearchCondition = 
    CategoryIdSearchCondition(CategorySearchCondition.parseInput(in, _.toLong, isNum))

  def isNum(c: Char): Boolean = '0' <= c && c <= '9'
}

object CategoryCodeSearchCondition {
  val Null = CategoryCodeSearchCondition(Vector[immutable.Seq[String]]())

  def apply(categoryCodes: String*): CategoryCodeSearchCondition =
    CategoryCodeSearchCondition(Vector(categoryCodes.toVector))

  def apply(in: String): CategoryCodeSearchCondition = 
    CategoryCodeSearchCondition(CategorySearchCondition.parseInput(in, identity, isIdentifier))

  def isIdentifier(c: Char): Boolean = '0' <= c && c <= '9' ||
    'a' <= c && c <= 'z' || 'A' <= c && c <= 'Z' ||
    c == '_'
}
