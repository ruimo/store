package models

import play.api.db._
import java.sql.Connection
import play.api.Play.current
import scala.collection.parallel.mutable.ParHashSet

object ConstraintHelper {
  case class ColumnSize(schema: Option[String], table: String, column: String) {
    require(schema != null, "schema should not be null.")
    require(table != null, "table should not be null.")
    require(column != null, "column should not be null.")

    lazy val columnSize = DB.withConnection { implicit conn =>
      val md = conn.getMetaData()
      val cols = md.getColumns(null, schema.orNull, table, column)
      if(cols.next())
        cols.getInt("COLUMN_SIZE")
      else 
        throw new Error("Database metadata does not have 'COLUMN_SIZE'!")
    }
  }
  
  val columnSizes = ParHashSet[ColumnSize]()

  def getColumnSize(schema: Option[String], table: String, column: String) : Int = {
    val newone = ColumnSize(schema,table,column)

    columnSizes.find { x => x.hashCode == newone.hashCode } match {
      case Some(cached) => cached.columnSize
      case _            => { columnSizes += newone; newone.columnSize }
    }
  }

  def refreshColumnSizes() = {
    columnSizes.clear()
  }
}
