package controllers

import play.api.Play.current
import play.api.data.Forms._
import play.api.mvc.Controller
import controllers.I18n.I18nAware
import models._
import play.api.data.Form
import play.api.db.DB
import collection.immutable.LongMap

object AccountingBill extends Controller with NeedLogin with HasLogger with I18nAware {
  val accountingBillForm = Form(
    mapping(
      "year" -> number(min = YearMonth.MinYear, max = YearMonth.MaxYear),
      "month" -> number(min = 1, max = 12)
    )(YearMonth.apply)(YearMonth.unapply)
  )

  def index() = isAuthenticated { implicit login => implicit request =>
    Ok(
      views.html.accountingBill(accountingBillForm, List(), LongMap(), LongMap(), LongMap(), false)
    )
  }

  def show() = isAuthenticated { implicit login => implicit request =>
    accountingBillForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(
          views.html.accountingBill(formWithErrors, List(), LongMap(), LongMap(), LongMap(), false)
        )
      },
      yearMonth => {
        DB.withConnection { implicit conn =>
          val summaries = TransactionSummary.listByPeriod(
            siteUser = login.siteUser, yearMonth = yearMonth
          )
          val siteTranByTranId = summaries.foldLeft(LongMap[PersistedTransaction]()) {
            (sum, e) =>
            val siteTran = (new TransactionPersister).load(e.transactionId, LocaleInfo.getDefault)
            sum.updated(e.transactionId, siteTran)
          }
          val detailByTranSiteId = summaries.foldLeft(LongMap[Seq[TransactionDetail]]()) {
            (sum, e) =>
            val details = TransactionDetail.show(e.transactionSiteId, LocaleInfo.byLang(lang))
            sum.updated(e.transactionSiteId, details)
          }
          val boxBySiteAndItemSize = summaries.foldLeft(
            LongMap[LongMap[TransactionLogShipping]]()
          ) {
            (sum, e) =>
              sum ++ TransactionLogShipping.listBySite(e.transactionSiteId).foldLeft(
                LongMap[LongMap[TransactionLogShipping]]().withDefaultValue(LongMap[TransactionLogShipping]())
              ) {
                (names, e2) =>
                  names.updated(
                    e.transactionSiteId,
                    names(e.transactionSiteId).updated(e2.itemClass, e2)
                  )
              }
          }

          Ok(views.html.accountingBill(
            accountingBillForm.fill(yearMonth),
            summaries, detailByTranSiteId, boxBySiteAndItemSize, siteTranByTranId,
            login.siteUser.isDefined
          ))
        }
      }
    )
  }
}
