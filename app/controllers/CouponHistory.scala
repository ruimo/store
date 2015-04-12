package controllers

import models.PagedRecords
import play.api._
import db.DB
import play.api.mvc._
import play.api.Play.current
import controllers.I18n.I18nAware
import models.{CouponDetail, TransactionLogCoupon, LocaleInfo, TransactionLogCouponId, SiteItemNumericMetadataType, CouponDetailWithMetadata, LoginSession, SiteItemNumericMetadata, ItemId}

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
      val c = TransactionLogCoupon.at(
        LocaleInfo.byLang(lang), login.userId, TransactionLogCouponId(tranCouponId)
      )
      showCoupon(
        c.siteItemNumericMetadata,
        c.couponDetail.itemId,
        c.couponDetail.time,
        Some(c.couponDetail.tranHeaderId)
      )
    }
  }

  def showInstantCoupon(siteId: Long, itemId: Long) = isAuthenticated { implicit login => implicit request =>
    val metaData: Map[SiteItemNumericMetadataType, SiteItemNumericMetadata] = 
      DB.withConnection { implicit conn => SiteItemNumericMetadata.all(siteId, ItemId(itemId)) }

    if (metaData.get(SiteItemNumericMetadataType.INSTANT_COUPON).getOrElse(0) != 0) {
      showCoupon(metaData, ItemId(itemId), System.currentTimeMillis, None)
    }
    else {
      Redirect(routes.Application.index)
    }
  }

  def showCoupon(
    siteItemNumericMetadata: Map[SiteItemNumericMetadataType, SiteItemNumericMetadata],
    itemId: ItemId,
    time: Long,
    tranId: Option[Long]
  )(
    implicit request: RequestHeader,
    login: LoginSession
  ): Result = {
    siteItemNumericMetadata.get(SiteItemNumericMetadataType.COUPON_TEMPLATE) match {
      case None => Ok(
        views.html.showCoupon(itemId, time, tranId)
      )
      case Some(metadata) =>
        if (metadata.metadata == 0) Ok(
          views.html.showCoupon(itemId, time, tranId)
        )
        else Ok(
          views.html.showCouponTemplate(metadata.metadata, itemId, time, tranId)
        )
    }
  }
}
