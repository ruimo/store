package models

case class CreateCategory(localeId: Long, categoryName: String) {
  def save() {
    Category.createNew(Map(LocaleInfo(localeId) -> categoryName))
  }
}

