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
import collection.immutable

object ShoppingCart extends Controller with I18nAware with NeedLogin with HasLogger {
  def addToCartJson = isAuthenticatedJson { implicit login => implicit request =>
    request.body.asJson.map { json =>
      val siteId = (json \ "siteId").as[Long]
      val itemId = (json \ "itemId").as[Long]

      DB.withConnection { implicit conn => {
        ShoppingCartItem.addItem(login.userId, siteId, itemId, 1)
        val cart: ShoppingCartTotal = ShoppingCartItem.listItemsForUser(
          LocaleInfo.getDefault, 
          login.userId
        )

        Ok(Json.toJson(toJson(
          getItemInfo(Map((siteId, itemId) -> 1), cart.table),
          cart
        )))
      }}
    }.get
  }

  def addOrderHistoryJson = isAuthenticated { implicit login => implicit request =>
    request.body.asJson.map { json =>
      val siteId = (json \ "siteId").as[Long]
      val tranSiteId = (json \ "tranSiteId").as[Long]

      DB.withConnection { implicit conn => {
        val itemsInTran: Seq[TransactionLogItem] = TransactionLogItem.listBySite(tranSiteId)
        itemsInTran.foreach { e =>
          ShoppingCartItem.addItem(login.userId, siteId, e.itemId, e.quantity.toInt)
        }

        val cart = ShoppingCartItem.listItemsForUser(
          LocaleInfo.getDefault, 
          login.userId
        )

        Ok(Json.toJson(toJson(
          getItemInfo(
            itemsInTran.map {
              e => (siteId, e.itemId) -> e.quantity.toInt
            }.toMap,
            cart.table
          ),
          cart
        )))
      }}
    }.get
  }

  def toJson(
    addedItems: immutable.Seq[ShoppingCartTotalEntry],
    cart: ShoppingCartTotal
  )(implicit lang: Lang): immutable.Map[String, immutable.Seq[immutable.Map[String, String]]] = {
    Map(
      "added" -> toJson(addedItems),
      "current" -> toJson(cart.table)
    )
  }

  def toJson(table: immutable.Seq[ShoppingCartTotalEntry]): immutable.Seq[immutable.Map[String, String]] = table.map { e =>
      Map(
        "itemName" -> e.itemName.name,
        "siteName" -> e.site.name,
        "unitPrice" -> ViewHelpers.toAmount(e.itemPriceHistory.unitPrice),
        "quantity" -> e.shoppingCartItem.quantity.toString,
        "price" -> ViewHelpers.toAmount(e.itemPrice)
      )
    }

  /**
   * @param keys Tupple of siteId, itemId, quantity.
   * @param cart Shopping cart information.
   * @return Shopping cart information.
   * */
  def getItemInfo(
    keys: immutable.Map[(Long, Long), Int], 
    cart: immutable.Seq[ShoppingCartTotalEntry]
  ): immutable.Seq[ShoppingCartTotalEntry] = {
    def getItemInfo(
      keys: immutable.Map[(Long, Long), Int], 
      cart: immutable.Seq[ShoppingCartTotalEntry],
      result: immutable.Vector[ShoppingCartTotalEntry]
    ): immutable.Seq[ShoppingCartTotalEntry] = 
      if (cart.isEmpty) {
        if (keys.isEmpty) {
          result
        }
        else {
          throw new Error("Logic error. key(" + keys + ") was not found in shopping cart.")
        }
      }
      else {
        val cartHead = cart.head
        val keyToDrop = (cartHead.site.id.get, cartHead.itemId)

        keys.get(keyToDrop) match {
          case None => getItemInfo(keys, cart.tail, result)
          case Some(quantity) => getItemInfo(
            keys - keyToDrop,
            cart.tail,
            result :+ cartHead.withNewQuantity(quantity)
          )
        }
      }

    getItemInfo(keys, cart, Vector[ShoppingCartTotalEntry]())
  }
}
