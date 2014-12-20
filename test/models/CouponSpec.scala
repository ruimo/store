package models

import org.specs2.mutable._

import anorm._
import anorm.SqlParser
import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current

class CouponSpec extends Specification {
  "Coupon" should {
    "Can create new coupon." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => {
          val coupon1 = Coupon.createNew()
          val coupon2 = Coupon.createNew()

          Coupon(coupon1.id.get) === coupon1
          Coupon(coupon2.id.get) === coupon2
        }}
      }
    }

    "Can let item a coupon." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => {
          val cat1 = Category.createNew(
            Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
          )
          val item = Item.createNew(cat1)
          Coupon.isCoupon(item.id.get) === false

          Coupon.update(item.id.get, false)
          Coupon.isCoupon(item.id.get) === false

          Coupon.update(item.id.get, true)
          Coupon.isCoupon(item.id.get) === true

          Coupon.update(item.id.get, false)
          Coupon.isCoupon(item.id.get) === false

          Coupon.update(item.id.get, true)
          Coupon.isCoupon(item.id.get) === true
        }}
      }
    }
  }
}
