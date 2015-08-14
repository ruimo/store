package models

class ItemExpiredException(cartItem: ShoppingCartItem, itemName: ItemName, site: Site) extends Exception(
  "Item expired cartItem = " + cartItem + ", itemName = " + itemName + ", site = " + site
)

