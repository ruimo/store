package models

import java.sql.Connection

case class UpdateCategoryNameTable(
  categoryNames: Seq[UpdateCategoryName]
) {
  def save()(implicit conn: Connection) {
    categoryNames.foreach {
      _.save()
    }
  }
}

case class UpdateCategoryName(
  categoryId: Long, localeId: Long, name: String
) {
  def save()(implicit conn: Connection) {
    CategoryName.update(categoryId, localeId, name)
  }
  def create()(implicit conn: Connection) {
    ExceptionMapper.mapException {
      CategoryName.createNew(categoryId, localeId, name)
    }
  }
}
