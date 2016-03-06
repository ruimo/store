package controllers

import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable._
import play.api.Play.current
import org.specs2.mock._
import play.api.mvc.Session
import models.{UserRole, StoreUser, TestHelper, LoginSession}
import play.api.db.DB
import org.mockito.Mockito.mock
import models.PagedRecords
import models.OrderBy
import models.Desc
import models.RecommendByAdmin
import com.ruimo.recoeng.json.SalesItem

class RecommendationSpec extends Specification {
  "Recommendation controller" should {
    "Can calculate recommendation by admin when item recommendation is empty." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase())
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser => DB.withConnection { implicit conn =>
        val expectedDetails = Map(
          (111L, 222L) -> mock(classOf[models.ItemDetail]),
          (112L, 223L) -> mock(classOf[models.ItemDetail]),
          (113L, 224L) -> mock(classOf[models.ItemDetail])
        )
        val details = Recommendation.calcByAdmin(
          shoppingCartItems = List(),
          maxCount = 5,
          queryRecommendByAdmin = (recordCountToRetrieve: Int) => {
            recordCountToRetrieve === 5
            PagedRecords(
              currentPage = 0,
              pageSize = 10,
              pageCount = 1,
              orderBy = OrderBy("score", Desc),
              records = Seq(
                (RecommendByAdmin(Some(1L), 111L, 222L, 10, true), None, None),
                (RecommendByAdmin(Some(2L), 112L, 223L, 20, true), None, None),
                (RecommendByAdmin(Some(3L), 113L, 224L, 30, true), None, None)
              )
            )
          },
          (siteId: Long, itemId: Long) => expectedDetails(siteId -> itemId)
        )
        details.size === 3
        details(0) === expectedDetails(111L -> 222L)
        details(1) === expectedDetails(112L -> 223L)
        details(2) === expectedDetails(113L -> 224L)
      }}
    }

    "Can calculate recommendation by admin when there are some item recommendations." in {
      val expectedDetails = Map(
        (111L, 222L) -> mock(classOf[models.ItemDetail]),
        (112L, 223L) -> mock(classOf[models.ItemDetail]),
        (113L, 224L) -> mock(classOf[models.ItemDetail]),
        (111L, 777L) -> mock(classOf[models.ItemDetail]),
        (111L, 778L) -> mock(classOf[models.ItemDetail]),
        (111L, 779L) -> mock(classOf[models.ItemDetail])
      )
      val details = Recommendation.calcByAdmin(
        shoppingCartItems = List(
          SalesItem("111", "777", 1),
          SalesItem("111", "778", 1),
          SalesItem("111", "779", 1)
        ),
        maxCount = 2,
        queryRecommendByAdmin = (recordCountToRetrieve: Int) => {
          recordCountToRetrieve === 5
          PagedRecords(
            currentPage = 0,
            pageSize = 10,
            pageCount = 1,
            orderBy = OrderBy("score", Desc),
            records = Seq(
              (RecommendByAdmin(Some(1L), 111L, 222L, 10, true), None, None),
              (RecommendByAdmin(Some(2L), 112L, 223L, 20, true), None, None),
              (RecommendByAdmin(Some(3L), 113L, 224L, 30, true), None, None)
            )
          )
        },
        (siteId: Long, itemId: Long) => expectedDetails(siteId -> itemId)
      )
      details.size === 2
      details(0) === expectedDetails(111L -> 222L)
      details(1) === expectedDetails(112L -> 223L)
    }
  }
}
