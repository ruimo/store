package controllers

import play.api._
import play.api.mvc._

object Purchase extends Controller with NeedLogin with HasLogger {
  def addToCart(siteId: Long, itemId: Long) = isAuthenticated { loginSession => implicit request =>
    Ok(views.html.shoppingCart())
  }
}
