package controllers

import controllers.I18n.I18nAware
import helpers.ViewHelpers
import models._
import play.api.Play.current
import play.api.db.DB
import play.api.i18n.Lang
import play.api.libs.json._
import play.api.mvc.Controller

import scala.collection.immutable

object ShoppingCart extends Controller with I18nAware with NeedLogin with HasLogger {
  def addToCartJson = NeedAuthenticatedJson { implicit request =>
    implicit val login = request.user

    request.body.asJson.map { json =>
      val siteId = (json \ "siteId").as[Long]
      val itemId = (json \ "itemId").as[Long]
      val quantity = (json \ "quantity").as[Int]

      DB.withConnection { implicit conn => {
        ShoppingCartItem.addItem(login.userId, siteId, itemId, quantity)
        val (cart: ShoppingCartTotal, errors: Seq[ItemExpiredException]) = ShoppingCartItem.listItemsForUser(
          LocaleInfo.getDefault, 
          login.userId
        )

        Ok(
          Json.toJson(
            toJson(errors) ++
            toJson(
              getItemInfo(Map((siteId, itemId) -> quantity), cart.table),
              cart
            )
          )
        )
      }}
    }.get
  }

  def addOrderHistoryJson = NeedAuthenticatedJson { implicit request =>
    implicit val login = request.user

    request.body.asJson.map { json =>
      val siteId = (json \ "siteId").as[Long]
      val tranSiteId = (json \ "tranSiteId").as[Long]

      DB.withConnection { implicit conn => {
        val itemsInTran: Seq[TransactionLogItem] = TransactionLogItem.listBySite(tranSiteId)
        itemsInTran.foreach { e =>
          ShoppingCartItem.addItem(login.userId, siteId, e.itemId, e.quantity.toInt)
        }

        val (cart: ShoppingCartTotal, errors: Seq[ItemExpiredException]) = ShoppingCartItem.listItemsForUser(
          LocaleInfo.getDefault, 
          login.userId
        )

        Ok(
          Json.toJson(
            toJson(errors) ++
            toJson(
              getItemInfo(
                itemsInTran.map {
                  e => (siteId, e.itemId) -> e.quantity.toInt
                }.toMap,
                cart.table
              ),
              cart
            )
          )
        )
      }}
    }.get
  }

  def toJson(errors: Seq[ItemExpiredException]): JsObject = JsObject(
    Seq(
      "expiredItemExists" -> JsBoolean(!errors.isEmpty)
    )
  )

  def toJson(
    addedItems: immutable.Seq[ShoppingCartTotalEntry],
    cart: ShoppingCartTotal
  )(implicit lang: Lang): JsObject = JsObject(
    Seq(
      "added" -> toJson(addedItems),
      "current" -> toJson(cart.table)
    )
  )

  def toJson(
    table: immutable.Seq[ShoppingCartTotalEntry]
  )(implicit lang: Lang): JsArray = JsArray(
    table.map { e =>
      JsObject(
        Seq(
          "itemName" -> JsString(e.itemName.name),
          "siteName" -> JsString(e.site.name),
          "unitPrice" -> JsString(ViewHelpers.toAmount(e.itemPriceHistory.unitPrice)),
          "quantity" -> JsString(e.shoppingCartItem.quantity.toString),
          "price" -> JsString(ViewHelpers.toAmount(e.itemPrice))
        )
      )
    }
  )

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

  def removeExpiredItems = NeedAuthenticated { implicit request =>
    DB.withConnection { implicit conn =>
      models.ShoppingCartItem.removeExpiredItems(request.user.storeUser.id.get)
    }
    Redirect(routes.Purchase.showShoppingCart)
  }
}
