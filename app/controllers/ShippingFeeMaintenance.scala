package controllers

import scala.collection.JavaConversions._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models._
import play.api.data.Form
import controllers.I18n.I18nAware
import play.api.mvc.Controller
import play.api.db.DB
import play.api.i18n.Messages
import play.api.Play.current
import models.CreateShippingBox

object ShippingFeeMaintenance extends Controller with I18nAware with NeedLogin with HasLogger {
  val createShippingBoxForm = Form(
    mapping(
      "siteId" -> longNumber,
      "itemClass" -> longNumber,
      "boxSize" -> number,
      "boxName" -> text.verifying(nonEmpty, maxLength(32))
    ) (CreateShippingBox.apply)(CreateShippingBox.unapply)
  )

  def startFeeMaintenance(boxId: Long) = isAuthenticated { implicit login =>
    forSuperUser { implicit request =>
      DB.withConnection { implicit conn =>
        ShippingBox.getWithSite(boxId) match {
          case Some(rec) => {
            val list = ShippingFee.listWithHistory(boxId, System.currentTimeMillis)
            Ok(views.html.admin.shippingFeeMaintenance(rec, list))
          }
          case None =>
            Redirect(
              routes.ShippingFeeMaintenance.startFeeMaintenance(boxId)
            ).flashing("message" -> Messages("record.already.deleted"))
        }
      }
    }
  }

  def withCountry(boxId: Long, country: String) = isAuthenticated { implicit login =>
    forSuperUser { implicit request =>
      val cc = CountryCode.byName(country);
      val prefectures = Prefecture.table(cc)

      Ok("")
    }
  }
}
