package helpers

import controllers.HasLogger
import models._
import models.PersistedTransaction
import com.ruimo.recoeng.json.{TransactionSalesMode, SalesItem, OnSalesJsonResponse}
import com.ruimo.recoeng.{RecoEngApi, RecoEngPlugin}
import play.api.Play.current
import play.api.libs.json.JsResult
import play.api.libs.concurrent.Akka
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsResult, JsError, JsSuccess}

object RecommendEngine extends HasLogger {
  def onSales(
    login: LoginSession, tran: PersistedTransaction, addr: Address
  ) {
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
          logger.error("Cannot invoke recommend enging's onSales.", t)
      }
    }
  }

  def sendOnSales(
    login: LoginSession, tran: PersistedTransaction, addr: Address, api: RecoEngApi = RecoEngPlugin.api
  ): JsResult[OnSalesJsonResponse] =
    api.onSales(
      transactionMode = TransactionSalesMode,
      transactionTime = tran.header.transactionTime,
      userCode = tran.header.userId.toString,
      itemTable = tran.itemTable.map { e =>
        val siteId: String = e._1.toString
        val items: Seq[(ItemName, TransactionLogItem)] = e._2
        
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
}
