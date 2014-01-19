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
  lazy val config = play.api.Play.maybeApplication.map(_.configuration).get
  lazy val shippingCountries: List[String] = config.getStringList("shipping.countries").get.toList

  val createShippingBoxForm = Form(
    mapping(
      "siteId" -> longNumber,
      "itemClass" -> longNumber,
      "boxSize" -> number,
      "boxName" -> text.verifying(nonEmpty, maxLength(32))
    ) (CreateShippingBox.apply)(CreateShippingBox.unapply)
  )

  def index = isAuthenticated { implicit login => forSuperUser { implicit request =>
    Ok(views.html.admin.shippingFeeMaintenance(shippingCountries))
  }}

  def withCountry(country: String) = isAuthenticated { implicit login => forSuperUser { implicit request =>
    val cc = CountryCode.byName(country);
    val prefectures = Prefecture.table(cc)

    Ok("")
  }}
}
