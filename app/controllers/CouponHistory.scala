package controllers

import models.PagedRecords
import play.api._
import db.DB
import play.api.mvc._
import play.api.Play.current
import controllers.I18n.I18nAware
import models.{CouponDetail, TransactionLogCoupon, LocaleInfo, TransactionLogCouponId, SiteItemNumericMetadataType}

object CouponHistory extends Controller with I18nAware with NeedLogin {
  def showPurchasedCouponList(
    page: Int, pageSize: Int, orderBySpec: String
  ) = isAuthenticated { implicit login => implicit request =>
    DB.withConnection { implicit conn =>
      val list: PagedRecords[CouponDetail] = TransactionLogCoupon.list(
        locale = LocaleInfo.byLang(lang),
        userId = login.userId, 
        page = page,
        pageSize = pageSize
      )

      Ok(views.html.showCouponHistory(list))
    }
  }

  def showPurchasedCoupon(
    tranCouponId: Long
  ) = isAuthenticated { implicit login => implicit request =>
    DB.withConnection { implicit conn =>
      val couponDetail = TransactionLogCoupon.at(
        LocaleInfo.byLang(lang), login.userId, TransactionLogCouponId(tranCouponId)
      )
      couponDetail.siteItemNumericMetadata.get(SiteItemNumericMetadataType.COUPON_TEMPLATE) match {
        case None => Ok(views.html.showCoupon(couponDetail))
          case Some(metadata) =>
          if (metadata.metadata == 0) Ok(views.html.showCoupon(couponDetail))
          else Ok(views.html.showCouponTemplate(metadata.metadata, couponDetail))
      }
    }
  }
}
