package models

case class CreateItem(
  localeId: Long, categoryId: Long, itemName: String, price: BigDecimal, description: String
) {
  def save() {
  }
}
