package models

import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

case class CreateCategory(localeId: Long, categoryName: String, parent: Option[Long]) {
  def save()(implicit conn: Connection) {
    parent match {
      case Some(p) => Category.createNew(Category.get(p), Map(LocaleInfo(localeId) -> categoryName)) 
      case _       => Category.createNew(Map(LocaleInfo(localeId) -> categoryName)) 
    }
  }
}

