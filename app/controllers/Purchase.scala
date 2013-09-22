package controllers

import play.api._
import db.DB
import play.api.mvc._
import models.{LocaleInfo, ShoppingCart}
import play.api.Play.current

object Purchase extends Controller with NeedLogin with HasLogger {
  def addToCart(siteId: Long, itemId: Long) = isAuthenticated { loginSession => implicit request =>
    DB.withConnection { implicit conn => {
      val cartItem = ShoppingCart.addItem(loginSession.userId, siteId, itemId, 1)
      Results.Redirect(routes.Purchase.showShoppingCart())
    }}
  }

  def showShoppingCart = isAuthenticated { loginSession => implicit request =>
    Ok(
      views.html.shoppingCart(
        DB.withConnection { implicit conn => {
          ShoppingCart.listItemsForUser(
            LocaleInfo.getDefault, 
            loginSession.userId
          )
        }}
      )
    )
  }

  def changeItemQuantity(cartId: Long, quantity: Int) = isAuthenticated { loginSession => implicit request =>
    DB.withConnection { implicit conn => {
      val updateCount = ShoppingCart.changeQuantity(cartId, loginSession.userId, quantity)
      logger.info("Purchase.changeItemQuantity() updateCount = " + updateCount)

      Results.Redirect(routes.Purchase.showShoppingCart())
    }}
  }

  def deleteItemFromCart(cartId: Long) = isAuthenticated { loginSession => implicit request =>
    DB.withConnection { implicit conn => {
      val updateCount = ShoppingCart.remove(cartId, loginSession.userId)
      logger.info("Purchase.deleteItemFromCart() updateCount = " + updateCount)

      Results.Redirect(routes.Purchase.showShoppingCart())
    }}
  }

  def clear = isAuthenticated { loginSession => implicit request =>
    DB.withConnection { implicit conn => {
      val updateCount = ShoppingCart.removeForUser(loginSession.userId)
      logger.info("Purchase.clear() updateCount = " + updateCount)

      Results.Redirect(routes.Purchase.showShoppingCart())
    }}
  }
}
