package controllers

import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models._
import play.api.data.Form
import controllers.I18n.I18nAware
import play.api.mvc.Controller
import play.api.db.DB
import play.api.i18n.{Lang, Messages}
import play.api.Play.current
import helpers.{TokenGenerator, RandomTokenGenerator}
import play.api.libs.json.Json

import helpers.ViewHelpers

object ShoppingCart extends Controller with I18nAware with NeedLogin with HasLogger {
  def addToCartJson = isAuthenticatedJson { implicit login => implicit request =>
    request.body.asJson.map { json =>
      val siteId = (json \ "siteId").as[Long]
      val itemId = (json \ "itemId").as[Long]

      DB.withConnection { implicit conn => {
        val cartItem = ShoppingCartItem.addItem(login.userId, siteId, itemId, 1)
        val cart = ShoppingCartItem.listItemsForUser(
          LocaleInfo.getDefault, 
          login.userId
        )

        Ok(Json.toJson(toJson(cart)))
      }}
    }.get
  }

  def toJson(cart: ShoppingCartTotal): Seq[Map[String, String]] = cart.table.map { e =>
    Map(
      "itemName" -> e.itemName.name,
      "siteName" -> e.site.name,
      "unitPrice" -> ViewHelpers.toAmount(e.itemPriceHistory.unitPrice),
      "quantity" -> e.shoppingCartItem.quantity.toString,
      "price" -> ViewHelpers.toAmount(e.itemPrice)
    )
  }
}
