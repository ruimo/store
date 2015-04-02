package controllers

import java.util.Locale
import controllers.I18n.I18nAware
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.db.DB
import play.api.mvc._
import play.api.i18n.{Lang, Messages}
import models.{StoreUser, CreatePrize, Address, UserAddress, CountryCode, JapanPrefecture}
import constraints.FormConstraints._
import models.Sex
import helpers.Enums

object Prize extends Controller with NeedLogin with HasLogger with I18nAware {
  lazy val SexForDropdown: Seq[(String, String)] = Seq(
    (Sex.MALE.ordinal.toString, Messages("sex." + Sex.MALE)),
    (Sex.FEMALE.ordinal.toString, Messages("sex." + Sex.FEMALE))
  )

  def prizeFormJa(implicit lang: Lang) = Form(
    mapping(
      "firstName" -> text.verifying(firstNameConstraint: _*),
      "lastName" -> text.verifying(lastNameConstraint: _*),
      "firstNameKana" -> text.verifying(firstNameConstraint: _*),
      "lastNameKana" -> text.verifying(lastNameConstraint: _*),
      "zip" -> tuple(
        "zip1" -> text.verifying(z => Shipping.Zip1Pattern.matcher(z).matches),
        "zip2" -> text.verifying(z => Shipping.Zip2Pattern.matcher(z).matches)
      ),
      "prefecture" -> number,
      "address1" -> text.verifying(nonEmpty, maxLength(256)),
      "address2" -> text.verifying(nonEmpty, maxLength(256)),
      "address3" -> text.verifying(maxLength(256)),
      "address4" -> text.verifying(maxLength(256)),
      "address5" -> text.verifying(maxLength(256)),
      "tel" -> text.verifying(Messages("error.number"), z => Shipping.TelPattern.matcher(z).matches),
      "comment" -> text.verifying(maxLength(2048)),
      "command" -> text,
      "age" -> text,
      "sex" -> number
    )(CreatePrize.apply4Japan)(CreatePrize.unapply4Japan)
  )

  def entry(itemName: String) = isAuthenticated { implicit login => implicit request =>
    val user: StoreUser = login.storeUser
    val addr: Option[Address] = DB.withConnection{ implicit conn =>
      UserAddress.getByUserId(user.id.get).map {
        ua => Address.byId(ua.addressId)
      }
    }

    val (countryCode, prefectures, lookupPref) = lang.toLocale match {
      case Locale.JAPANESE =>
        (CountryCode.JPN, Address.JapanPrefectures, JapanPrefecture.byIndex _)
      case Locale.JAPAN =>
        (CountryCode.JPN, Address.JapanPrefectures, JapanPrefecture.byIndex _)
      case _ =>
        (CountryCode.JPN, Address.JapanPrefectures, JapanPrefecture.byIndex _)
    }

    val model = CreatePrize(
      countryCode,
      user.firstName, user.middleName.getOrElse(""), user.lastName,
      addr.map(_.firstNameKana).getOrElse(""), addr.map(_.lastNameKana).getOrElse(""),
      (addr.map(_.zip1).getOrElse(""), addr.map(_.zip2).getOrElse(""), addr.map(_.zip3).getOrElse("")),
      lookupPref(addr.map(_.prefecture.code).getOrElse(1)),
      addr.map(_.address1).getOrElse(""),
      addr.map(_.address2).getOrElse(""),
      addr.map(_.address3).getOrElse(""),
      addr.map(_.address4).getOrElse(""),
      addr.map(_.address5).getOrElse(""),
      addr.map(_.tel1).getOrElse(""),
      "", "", "", Sex.MALE
    )

    Ok(
      lang.toLocale match {
        case Locale.JAPANESE =>
          views.html.prizeJa(itemName, user, prefectures, prizeFormJa.fill(model), SexForDropdown)
        case Locale.JAPAN =>
          views.html.prizeJa(itemName, user, prefectures, prizeFormJa.fill(model), SexForDropdown)
        case _ =>
          views.html.prizeJa(itemName, user, prefectures, prizeFormJa.fill(model), SexForDropdown)
      }
    )
  }

  def confirm(itemName: String) = isAuthenticated { implicit login => implicit request =>
    Ok("") // TODOkT.B.D.
  }

  def confirmJa(itemName: String) = isAuthenticated { implicit login => implicit request =>
    val user: StoreUser = login.storeUser

    prizeFormJa.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in Prize.confirmJa. " + formWithErrors)
        BadRequest(
          views.html.prizeJa(
            itemName,
            user,
            Address.JapanPrefectures,
            formWithErrors,
            SexForDropdown
          )
        )
      },
      info => {
        Ok(
          views.html.prizeConfirmJa(
            itemName,
            user,
            info
          )
        )
      }
    )
  }

  def submit(itemName: String) = isAuthenticated { implicit login => implicit request =>
    Ok("") // T.B.D.
  }

  def submitJa(itemName: String) = isAuthenticated { implicit login => implicit request =>
    val user: StoreUser = login.storeUser

    prizeFormJa.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in Prize.submitJa. " + formWithErrors)
        BadRequest(
          views.html.prizeJa(
            itemName,
            user,
            Address.JapanPrefectures,
            formWithErrors,
            SexForDropdown
          )
        )
      },
      info => {
        if (info.command == "amend") {
          Ok(
            views.html.prizeJa(
              itemName,
              user,
              Address.JapanPrefectures,
              prizeFormJa.fill(info),
              SexForDropdown
            )
          )
        }
        else {
          Ok(
            views.html.prizeCompleted(
              itemName,
              user,
              info
            )
          )
        }
      }
    )
  }
}
