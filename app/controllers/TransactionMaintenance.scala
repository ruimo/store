package controllers

import play.api.i18n.{Lang, Messages}
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models._
import play.api.data.Form
import controllers.I18n.I18nAware
import play.api.mvc.Controller
import play.api.db.DB
import play.api.i18n.Messages
import play.api.Play.current

object TransactionMaintenance extends Controller with I18nAware with NeedLogin with HasLogger {
  val changeStatusForm = Form(
    mapping(
      "transactionSiteId" -> longNumber,
      "status" -> number
    )(ChangeTransactionStatus.apply)(ChangeTransactionStatus.unapply)
  )

  def statusDropDown(implicit lang: Lang): Seq[(String, String)] =
    classOf[TransactionStatus].getEnumConstants.foldLeft(List[(String, String)]()) {
      (list, e) => (e.ordinal.toString, Messages("transaction.status." + e.toString)) :: list
    }.reverse

  def index = isAuthenticated { implicit login => forAdmin { implicit request =>
    DB.withConnection { implicit conn =>
      Ok(
        views.html.admin.transactionMaintenance(
          TransactionSummary.list(login.siteUser),
          changeStatusForm, statusDropDown
        )
      )
    }
  }}

  def setStatus = isAuthenticated { implicit login => forAdmin { implicit request =>
    changeStatusForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in TransactionMaintenance.setStatus.")
        DB.withConnection { implicit conn =>
          BadRequest(
            views.html.admin.transactionMaintenance(
              TransactionSummary.list(
                if (login.isAdmin) None else login.siteUser
              ),
              changeStatusForm, statusDropDown
            )
          )
        }
      },
      newStatus => {
        DB.withConnection { implicit conn =>
          newStatus.save()
          Redirect(routes.TransactionMaintenance.index)
        }
      }
    )
  }}
}
