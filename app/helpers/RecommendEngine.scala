package helpers

import controllers.HasLogger
import models._
import models.PersistedTransaction
import com.ruimo.recoeng.json.{TransactionSalesMode, SalesItem, OnSalesJsonResponse, ScoredItem}
import com.ruimo.recoeng.json.RecommendByItemJsonResponse
import com.ruimo.recoeng.json.JsonRequestPaging
import com.ruimo.recoeng.{RecoEngApi, RecoEngPlugin}
import play.api.Play.current
import play.api.libs.json.JsResult
import play.api.libs.concurrent.Akka
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsResult, JsError, JsSuccess}

object RecommendEngine extends HasLogger {
  def onSales(login: LoginSession, tran: PersistedTransaction, addr: Option[Address]) {
    Akka.system.scheduler.scheduleOnce(0.microsecond) {
      try {
        sendOnSales(login, tran, addr) match {
          case JsSuccess(resp, _) =>
            resp.header.statusCode match {
              case "OK" => logger.info("Receive response of recommend info onSales: " + resp)
              case _ => logger.error("Receive response of recommend info onSales: " + resp)
            }
          case JsError(errors) =>
            logger.error("Cannot invoke recommend engine's onSales: " + errors)
        }
      }
      catch {
        case t: Throwable =>
          logger.error("Failed with exception at recommend enging's onSales.", t)
      }
    }
  }

  def recommendByItem(salesItems: Seq[SalesItem]): Seq[ScoredItem] = {
    try {
      sendRecommendByItem(salesItems) match {
        case JsSuccess(resp, _) =>
          resp.header.statusCode match {
            case "OK" => {
              logger.info("Receive response of recommend info recommendByItem: " + resp)
              resp.salesItems
            }
            case _ => {
              logger.error("Receive response of recommend info recommendByItem: " + resp)
              Seq()
            }
          }
        case JsError(errors) => {
          logger.error("Cannot invoke recommend engine's recommendByItem: " + errors)
          Seq()
        }
      }
    }
    catch {
      case t: Throwable => {
        logger.error("Failed with exception at recommend enging's recommendByItem.", t)
        Seq()
      }
    }
  }

  def sendOnSales(
    login: LoginSession, tran: PersistedTransaction, addr: Option[Address], api: RecoEngApi = RecoEngPlugin.api
  ): JsResult[OnSalesJsonResponse] =
    api.onSales(
      transactionMode = TransactionSalesMode,
      transactionTime = tran.header.transactionTime,
      userCode = tran.header.userId.toString,
      itemTable = tran.itemTable.map { e =>
        val siteId: String = e._1.toString
        val items: Seq[(ItemName, TransactionLogItem, Option[TransactionLogCoupon])] = e._2
        
        items.map { t =>
          val item = t._2
          SalesItem(
            storeCode = siteId,
            itemCode = item.itemId.toString,
            quantity = item.quantity.toInt
          )
        }
      }.fold(List()) {
        (s1, s2) => s1 ++ s2
      }
    )

  def sendRecommendByItem(
    salesItems: Seq[SalesItem], api: RecoEngApi = RecoEngPlugin.api
  ): JsResult[RecommendByItemJsonResponse] =
    api.recommendByItem(
      salesItems = salesItems,
      paging = JsonRequestPaging(
        offset = 0,
        limit = 5
      )
    )
}
