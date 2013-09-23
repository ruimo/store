package controllers

import play.api._
import db.DB
import play.api.mvc._
import models.{LocaleInfo, ShoppingCart}
import play.api.Play.current

object Purchase extends Controller with NeedLogin with HasLogger {
  def addToCart(siteId: Long, itemId: Long) = isAuthenticated { login => implicit request =>
    DB.withConnection { implicit conn => {
      val cartItem = ShoppingCart.addItem(login.userId, siteId, itemId, 1)
      Redirect(routes.Purchase.showShoppingCart())
    }}
  }

  def showShoppingCart = isAuthenticated { login => implicit request =>
    Ok(
      views.html.shoppingCart(
        DB.withConnection { implicit conn => {
          ShoppingCart.listItemsForUser(
            LocaleInfo.getDefault, 
            login.userId
          )
        }}
      )
    )
  }

  def changeItemQuantity(cartId: Long, quantity: Int) = isAuthenticated { login => implicit request =>
    DB.withConnection { implicit conn => {
      val updateCount = ShoppingCart.changeQuantity(cartId, login.userId, quantity)
      logger.info("Purchase.changeItemQuantity() updateCount = " + updateCount)

      Results.Redirect(routes.Purchase.showShoppingCart())
    }}
  }

  def deleteItemFromCart(cartId: Long) = isAuthenticated { login => implicit request =>
    DB.withConnection { implicit conn => {
      val updateCount = ShoppingCart.remove(cartId, login.userId)
      logger.info("Purchase.deleteItemFromCart() updateCount = " + updateCount)

      Results.Redirect(routes.Purchase.showShoppingCart())
    }}
  }

  def clear = isAuthenticated { login => implicit request =>
    DB.withConnection { implicit conn => {
      val updateCount = ShoppingCart.removeForUser(login.userId)
      logger.info("Purchase.clear() updateCount = " + updateCount)

      Results.Redirect(routes.Purchase.showShoppingCart())
    }}
  }
}
