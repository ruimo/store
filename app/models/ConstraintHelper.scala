package models

import play.api.db._
import java.sql.Connection
import play.api.Play.current
import scala.collection.parallel.mutable.ParHashSet

object ConstraintHelper {
  case class ColumnSize(schema: String, table: String, column: String) {
    lazy val columnSize = DB.withConnection { implicit conn =>
      val md = conn.getMetaData()
      val cols = md.getColumns(null, schema, table, column)
      if(cols.next())
        cols.getInt("COLUMN_SIZE")
      else 
        -1
    }
  }
  
  val columnSizes = ParHashSet[ColumnSize]()

  def getColumnSize(schema: String, table: String, column: String) : Int = {
    val newone = ColumnSize(schema,table,column)

    columnSizes.find { x => x.hashCode == newone.hashCode } match {
      case Some(cached) => cached.columnSize
      case _            => { columnSizes += newone; newone.columnSize }
    }
  }







}