package helpers

import org.specs2.mutable._
import com.ruimo.recoeng.RecoEngApi
import com.ruimo.recoeng.json.{JsonResponseHeader, OnSalesJsonResponse, SalesItem, TransactionMode, TransactionSalesMode, SortOrder, JsonRequestPaging, Desc, Asc, ScoredItem}
import com.ruimo.recoeng.json.RecommendByItemJsonResponse
import play.api.libs.json.{JsSuccess, JsResult}
import models.LoginSession
import org.mockito.Mockito.mock
import models.PersistedTransaction
import models.TransactionLogItem
import models.TransactionLogCoupon
import models.TransactionLogHeader
import models.TransactionType
import models.Address
import models.ItemName
import helpers.Helper._
import com.ruimo.scoins.Scoping._

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
              JsonResponseHeader(sequenceNumber = "1234", statusCode = "OK", message = "msg")
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
            ), mock(classOf[Option[TransactionLogCoupon]])),
            (mock(classOf[ItemName]), TransactionLogItem(
              id = None,
              transactionSiteId = 889L,
              itemId = 8193L,
              itemPriceHistoryId = 445L,
              quantity = 5,
              amount = BigDecimal(124),
              costPrice = BigDecimal(555556),
              taxId = 1234L
            ), mock(classOf[Option[TransactionLogCoupon]]))
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
            ), mock(classOf[Option[TransactionLogCoupon]]))
          )
        )
      )
      val addr = mock(classOf[Address])
      val resp: JsResult[OnSalesJsonResponse] = RecommendEngine.sendOnSales(login, tran, Some(addr), api)
      doWith(resp.get.header) { header =>
        header.sequenceNumber === "1234"
        header.statusCode === "OK"
        header.message === "msg"
      }
    }

    "Can get recommendByItem" in {
      val api: RecoEngApi = new RecoEngApi {
        def onSales(
          requestTime: Long,
          sequenceNumber: Long,
          transactionMode: TransactionMode,
          transactionTime: Long,
          userCode: String,
          itemTable: Seq[SalesItem]
        ): JsResult[OnSalesJsonResponse] = null

        def recommendByItem(
          requestTime: Long,
          sequenceNumber: Long,
          salesItems: Seq[SalesItem],
          sort: SortOrder,
          paging: JsonRequestPaging
        ): JsResult[RecommendByItemJsonResponse] = {
          salesItems.size === 1
          doWith(salesItems(0)) { item =>
            item.storeCode === "11111"
            item.itemCode === "22222"
          }
          sort === Desc("score")
          paging.offset === 0
          paging.limit === 5

          JsSuccess(
            RecommendByItemJsonResponse(
              JsonResponseHeader(sequenceNumber = "1234", statusCode = "OK", message = "msg"),
              salesItems = Seq(
                ScoredItem(
                  storeCode = "1212",
                  itemCode = "2323",
                  score = 12
                ),
                ScoredItem(
                  storeCode = "3434",
                  itemCode = "4545",
                  score = 11
                )
              ),
              "desc(\"col\")",
              JsonRequestPaging(
                offset = 2,
                limit = 20
              )
            )
          )
        }
      }

      val result: JsResult[RecommendByItemJsonResponse] =
        RecommendEngine.sendRecommendByItem(
          Seq(SalesItem(storeCode = "11111", itemCode = "22222", quantity = 1)),
          api
        )
      doWith(result.get) { resp =>
        doWith(resp.header) { header =>
          header.sequenceNumber === "1234"
          header.statusCode === "OK"
          header.message === "msg"
        }
        doWith(resp.salesItems) { salesItems =>
          salesItems.size === 2
          doWith(salesItems(0)) { item =>
            item.storeCode === "1212"
            item.itemCode === "2323"
            item.score === 12f
          }
          doWith(salesItems(1)) { item =>
            item.storeCode === "3434"
            item.itemCode === "4545"
            item.score === 11f
          }
        }
      }
    }
  }
}
