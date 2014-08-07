package helpers

import org.specs2.mutable._
import com.ruimo.recoeng.RecoEngApi
import com.ruimo.recoeng.json.{JsonResponseHeader, OnSalesJsonResponse, SalesItem, TransactionMode, TransactionSalesMode, SortOrder, JsonRequestPaging, Desc, Asc}
import com.ruimo.recoeng.json.RecommendByItemJsonResponse
import play.api.libs.json.{JsSuccess, JsResult}
import models.LoginSession
import org.mockito.Mockito.mock
import models.PersistedTransaction
import models.TransactionLogItem
import models.TransactionLogHeader
import models.TransactionType
import models.Address
import models.ItemName
import helpers.Helper._

class RecommendEngineSpec extends Specification {
  "Recommend engine" should {
    "Can send transaction" in {
      val api: RecoEngApi = new RecoEngApi {
        def onSales(
          requestTime: Long,
          sequenceNumber: Long,
          transactionMode: TransactionMode,
          transactionTime: Long,
          userCode: String,
          salesItems: Seq[SalesItem]
        ): JsResult[OnSalesJsonResponse] = {
          transactionMode === TransactionSalesMode
          transactionTime === 23456L
          userCode === "12345"
          salesItems.size === 3
          val set = salesItems.toSet
          set.contains(SalesItem("555", "8192", 3)) must beTrue
          set.contains(SalesItem("555", "8193", 5)) must beTrue
          set.contains(SalesItem("666", "8194", 1)) must beTrue

          JsSuccess(
            OnSalesJsonResponse(
              JsonResponseHeader(
                sequenceNumber = "1234",
                statusCode = "OK",
                message = "msg"
              )
            )
          )
        }

        def recommendByItem(
          requestTime: Long = System.currentTimeMillis,
          sequenceNumber: Long,
          salesItems: Seq[SalesItem],
          sort: SortOrder = Desc("score"),
          paging: JsonRequestPaging
        ): JsResult[RecommendByItemJsonResponse] = null
      }

      val login = mock(classOf[LoginSession])
      val tran: PersistedTransaction = PersistedTransaction(
        header = TransactionLogHeader(
          id = None,
          userId = 12345L,
          transactionTime = 23456L,
          currencyId = 111L,
          totalAmount = BigDecimal(1234),
          taxAmount = BigDecimal(20),
          transactionType = TransactionType.NORMAL
        ),
        tranSiteLog = Map(),
        siteTable = Seq(),
        shippingTable = Map(),
        taxTable = Map(),
        itemTable = Map(
          555L -> Seq(
            (mock(classOf[ItemName]), TransactionLogItem(
              id = None,
              transactionSiteId = 888L,
              itemId = 8192L,
              itemPriceHistoryId = 444L,
              quantity = 3,
              amount = BigDecimal(123),
              costPrice = BigDecimal(555555),
              taxId = 1232L
            )),
            (mock(classOf[ItemName]), TransactionLogItem(
              id = None,
              transactionSiteId = 889L,
              itemId = 8193L,
              itemPriceHistoryId = 445L,
              quantity = 5,
              amount = BigDecimal(124),
              costPrice = BigDecimal(555556),
              taxId = 1234L
            ))
          ),
          666L -> Seq(
            (mock(classOf[ItemName]), TransactionLogItem(
              id = None,
              transactionSiteId = 890L,
              itemId = 8194L,
              itemPriceHistoryId = 446L,
              quantity = 1,
              amount = BigDecimal(125),
              costPrice = BigDecimal(555557),
              taxId = 1235L
            ))
          )
        )
      )
      val addr = mock(classOf[Address])
      val resp: JsResult[OnSalesJsonResponse] = RecommendEngine.sendOnSales(login, tran, addr, api)
      doWith(resp.get.header) { header =>
        header.sequenceNumber === "1234"
        header.statusCode === "OK"
        header.message === "msg"
      }
    }
  }
}
