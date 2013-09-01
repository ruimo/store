package models

case class CreateItem(itemName: String, price: BigDecimal, description: String) {
  def save() {
  }
}
