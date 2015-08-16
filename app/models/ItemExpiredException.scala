package models

class ItemExpiredException(
  val cartItem: ShoppingCartItem,
  val itemName: ItemName, 
  val site: Site
) extends Exception(
  "Item expired cartItem = " + cartItem + ", itemName = " + itemName + ", site = " + site
)

