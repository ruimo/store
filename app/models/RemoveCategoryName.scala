package models

import java.sql.Connection

case class RemoveCategoryName(
  categoryId: Long, localeId: Long
) {
  def remove()(implicit conn: Connection) {
    CategoryName.remove(categoryId, localeId)
  }
}
