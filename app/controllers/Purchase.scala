package controllers

import play.api._
import db.DB
import play.api.mvc._
import models._
import play.api.Play.current
import controllers.I18n.I18nAware

object Purchase extends Controller with NeedLogin with HasLogger with I18nAware {
  def addToCart(siteId: Long, itemId: Long, quantity: Int) = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    DB.withConnection { implicit conn => {
      val cartItem = ShoppingCartItem.addItem(login.userId, siteId, itemId, quantity)
      Redirect(routes.Purchase.showShoppingCart())
    }}
  }

  def showShoppingCart = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    val (total: ShoppingCartTotal, errors: Seq[ItemExpiredException]) = DB.withConnection { implicit conn =>
      ShoppingCartItem.listItemsForUser(
        LocaleInfo.getDefault, login
      )
    }

    if (errors.isEmpty) Ok(views.html.shoppingCart(total)) else Ok(views.html.itemExpired(errors))
  }

  def changeItemQuantity(cartId: Long, quantity: Int) = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    DB.withConnection { implicit conn => {
      val updateCount = ShoppingCartItem.changeQuantity(cartId, login.userId, quantity)
      logger.info("Purchase.changeItemQuantity() updateCount = " + updateCount)

      Results.Redirect(routes.Purchase.showShoppingCart())
    }}
  }

  def deleteItemFromCart(cartId: Long) = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    DB.withConnection { implicit conn => {
      val updateCount = ShoppingCartItem.remove(cartId, login.userId)
      logger.info("Purchase.deleteItemFromCart() updateCount = " + updateCount)

      Results.Redirect(routes.Purchase.showShoppingCart())
    }}
  }

  def clear = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    DB.withConnection { implicit conn => {
      val updateCount = ShoppingCartItem.removeForUser(login.userId)
      logger.info("Purchase.clear() updateCount = " + updateCount)

      Results.Redirect(routes.Purchase.showShoppingCart())
    }}
  }
}
