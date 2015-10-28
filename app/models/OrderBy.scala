package models

import java.util.regex.Pattern
import java.util.Locale

case class OrderBy(
  columnName: String,
  order: Order
) {
  require(OrderBy.OrderByPattern.matcher(columnName).matches)
  require(order != null)

  def invert = OrderBy(
    columnName,
    order.invert
  )

  override def toString = columnName + " " + order
}

object OrderBy {
  val OrderByPattern = Pattern.compile("[a-zA-Z0-9._ ]+");
  def apply(columnNameSpec: String): OrderBy = {
    val s = columnNameSpec.split("[ ]+")
    if (s.length == 2)
      OrderBy(s(0).toLowerCase(Locale.ROOT), Order(s(1)))
    else
      OrderBy(s(0), Asc)
  }
}

sealed abstract class Order {
  def invert: Order
}

object Order {
  def apply(s: String): Order =
    if (s.trim.toUpperCase == Asc.toString) Asc else Desc
}

case object Asc extends Order {
  override def invert = Desc
  override def toString = "ASC"
}

case object Desc extends Order {
  override def invert = Asc
  override def toString = "DESC"
}
