package controllers

import java.util.Locale
import play.api.i18n.{Lang, Messages}
import play.api.db.DB
import play.api.Play.current
import controllers.I18n.I18nAware
import play.api.mvc.{Action, Controller, RequestHeader}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models.{QaEntry, UserAddress, Address}
import helpers.QaMail
import constraints.FormConstraints._

object Qa extends Controller with HasLogger with I18nAware with NeedLogin {
  def jaForm(implicit lang: Lang) = Form(
    mapping(
      "qaType" -> text,
      "comment" -> text.verifying(nonEmpty, maxLength(8192)),
      "companyName" -> text.verifying(nonEmpty, maxLength(64)),
      "firstName" -> text.verifying(nonEmpty, maxLength(64)),
      "lastName" -> text.verifying(nonEmpty, maxLength(64)),
      "tel" -> text.verifying(nonEmpty).verifying(Messages("error.number"), z => Shipping.TelPattern.matcher(z).matches),
      "email" -> text.verifying(emailConstraint: _*)
    )(QaEntry.apply4Japan)(QaEntry.unapply4Japan)
  )

  def index() = optIsAuthenticated { implicit optLogin => implicit request => DB.withConnection { implicit conn => {
    val form = optLogin.map { login =>
      val optAddr: Option[Address] = UserAddress.getByUserId(login.storeUser.id.get).map { ua =>
        Address.byId(ua.addressId)
      }

      jaForm.fill(
        QaEntry(
          qaType = "",
          comment = "",
          companyName = login.storeUser.companyName.getOrElse(""),
          firstName = login.storeUser.firstName,
          middleName = login.storeUser.middleName.getOrElse(""),
          lastName = login.storeUser.lastName,
          tel = optAddr.map(_.tel1).getOrElse(""),
          email = login.storeUser.email
        )
      ).discardingErrors
    }.getOrElse(jaForm)

    lang.toLocale match {
      case Locale.JAPANESE =>
        Ok(views.html.qaJa(form))
      case Locale.JAPAN =>
        Ok(views.html.qaJa(form))
        
      case _ =>
        Ok(views.html.qaJa(form))
    }
  }}}

  def submitQaJa() = optIsAuthenticated { implicit optLogin => implicit request => DB.withConnection { implicit conn => {
    jaForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.qaJa(formWithErrors))
      },
      qa => {
        QaMail.send(qa)
        Ok(views.html.qaCompleted())
      }
    )
  }}}
}
