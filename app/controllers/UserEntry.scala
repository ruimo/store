package controllers

import java.util.Locale
import play.api.i18n.{Lang, Messages}
import play.api.Play.current
import controllers.I18n.I18nAware
import play.api.mvc.{Action, Controller, RequestHeader}
import play.api.db.DB
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models.UserRegistration
import models.Address
import helpers.UserEntryMail

object UserEntry extends Controller with HasLogger with I18nAware with NeedLogin {
  def jaForm(implicit lang: Lang) = Form(
    mapping(
      "companyName" -> text.verifying(nonEmpty, maxLength(64)),
      "zip1" -> text.verifying(z => Shipping.Zip1Pattern.matcher(z).matches),
      "zip2" -> text.verifying(z => Shipping.Zip2Pattern.matcher(z).matches),
      "prefecture" -> number,
      "address1" -> text.verifying(nonEmpty, maxLength(256)),
      "address2" -> text.verifying(nonEmpty, maxLength(256)),
      "address3" -> text.verifying(maxLength(256)),
      "tel" -> text.verifying(Messages("error.number"), z => Shipping.TelPattern.matcher(z).matches),
      "fax" -> text.verifying(Messages("error.number"), z => Shipping.TelOptionPattern.matcher(z).matches),
      "title" -> text.verifying(maxLength(256)),
      "firstName" -> text.verifying(nonEmpty, maxLength(64)),
      "lastName" -> text.verifying(nonEmpty, maxLength(64)),
      "email" -> text.verifying(nonEmpty, maxLength(128))
    )(UserRegistration.apply4Japan)(UserRegistration.unapply4Japan)
  )

  def index() = Action { implicit request => DB.withConnection { implicit conn => {
    implicit val login = loginSession(request, conn)
    lang.toLocale match {
      case Locale.JAPANESE =>
        Ok(views.html.userEntryJa(jaForm, Address.JapanPrefectures))
      case Locale.JAPAN =>
        Ok(views.html.userEntryJa(jaForm, Address.JapanPrefectures))
        
      case _ =>
        Ok(views.html.userEntryJa(jaForm, Address.JapanPrefectures))
    }
  }}}

  def submitUserJa() = Action { implicit request => DB.withConnection { implicit conn => {
    implicit val login = loginSession(request, conn)
    jaForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.userEntryJa(formWithErrors, Address.JapanPrefectures))
      },
      newUser => {
        UserEntryMail.sendUserRegistration(newUser)
        Ok(views.html.userEntryCompleted())
      }
    )
  }}}
}
