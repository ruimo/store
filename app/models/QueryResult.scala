package models

import scala.collection.immutable

case class QueryResult(
  columnNames: immutable.Seq[String],
  rows: immutable.Seq[immutable.Seq[Any]]
)
