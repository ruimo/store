package controllers

import play.api.i18n.{Lang, Messages}
import play.api.Play.current
import play.api.data.Forms._
import controllers.I18n.I18nAware
import models._
import play.api.data.Form
import play.api.db.DB
import collection.immutable.LongMap
import play.api.mvc.{Controller, RequestHeader}
import java.sql.Connection

object AccountingBill extends Controller with NeedLogin with HasLogger with I18nAware {
  val accountingBillForm = Form(
    mapping(
      "year" -> number(min = YearMonth.MinYear, max = YearMonth.MaxYear),
      "month" -> number(min = 1, max = 12),
      "user" -> longNumber
    )(YearMonthUser.apply)(YearMonthUser.unapply)
  )

  val accountingBillForStoreForm = Form(
    mapping(
      "year" -> number(min = YearMonth.MinYear, max = YearMonth.MaxYear),
      "month" -> number(min = 1, max = 12),
      "site" -> longNumber
    )(YearMonthSite.apply)(YearMonthSite.unapply)
  )

  def AllUser(implicit lang: Lang) = ("0", Messages("all"))

  def index() = NeedAuthenticated { implicit request =>
    implicit val login = request.user

    DB.withConnection { implicit conn =>
      Ok(
        views.html.accountingBill(
          accountingBillForm, accountingBillForStoreForm, List(), LongMap(), LongMap(), LongMap(),
          false, Site.tableForDropDown, Map(), List(AllUser)
        )
      )
    }
  }

  def show() = NeedAuthenticated { implicit request =>
    implicit val login = request.user

    accountingBillForm.bindFromRequest.fold(
      formWithErrors => {
        DB.withConnection { implicit conn =>
          BadRequest(
            views.html.accountingBill(
              formWithErrors, accountingBillForStoreForm, List(), LongMap(), LongMap(), LongMap(),
              false, Site.tableForDropDown, Map(), List(AllUser)
            )
          )
        }
      },
      yearMonth => {
        DB.withConnection { implicit conn =>
          val summariesForAllUser: Seq[TransactionSummaryEntry] = TransactionSummary.listByPeriod(
            siteId = login.siteUser.map(_.siteId), 
            yearMonth = yearMonth,
            onlyShipped = true
          )
          val userDropDown = getUserDropDown(summariesForAllUser)
          val summaries = yearMonth.userIdOpt match {
            case Some(userId) => summariesForAllUser.filter(_.buyer.id.get == userId)
            case None => summariesForAllUser
          }
          val siteTranByTranId = getSiteTranByTranId(summaries, request2lang)

println("*** summaries = " + summaries)
          Ok(views.html.accountingBill(
            accountingBillForm.fill(yearMonth),
            accountingBillForStoreForm,
            summaries, getDetailByTranSiteId(summaries),
            getBoxBySiteAndItemSize(summaries),
            siteTranByTranId,
            false,
            Site.tableForDropDown,
            getAddressTable(siteTranByTranId),
            userDropDown
          ))
        }
      }
    )
  }
  def showForStore() = NeedAuthenticated { implicit request =>
    implicit val login = request.user

    accountingBillForStoreForm.bindFromRequest.fold(
      formWithErrors => {
        DB.withConnection { implicit conn =>
          BadRequest(
            views.html.accountingBill(
              accountingBillForm, formWithErrors, List(), LongMap(), LongMap(), LongMap(), 
              true, Site.tableForDropDown, Map(), List()
            )
          )
        }
      },
      yearMonthSite => {
        DB.withConnection { implicit conn =>
          val summaries = TransactionSummary.listByPeriod(
            siteId = Some(yearMonthSite.siteId), yearMonth = yearMonthSite,
            onlyShipped = true
          )
          val siteTranByTranId = getSiteTranByTranId(summaries, request2lang)

          Ok(views.html.accountingBill(
            accountingBillForm,
            accountingBillForStoreForm.fill(yearMonthSite),
            summaries, getDetailByTranSiteId(summaries),
            getBoxBySiteAndItemSize(summaries),
            siteTranByTranId,
            true, Site.tableForDropDown,
            getAddressTable(siteTranByTranId),
            getUserDropDown(summaries)
          ))
        }
      }
    )
  }

  def getAddressTable(
    tran: LongMap[PersistedTransaction]
  )(
    implicit conn: Connection
  ): Map[Long, Address] = {
    val addresses = scala.collection.mutable.Map[Long, Address]()
    tran.values.foreach {
      pt => pt.shippingTable.values.foreach {
        seq => seq.foreach {
          tranShipping =>
          val id = tranShipping.addressId
          if (! addresses.isDefinedAt(id)) {
            addresses.put(id, Address.byId(id))
          }
        }
      }
    }
    addresses.toMap
  }

  def getBoxBySiteAndItemSize(
    summaries: Seq[TransactionSummaryEntry]
  )(
    implicit conn: Connection
  ): LongMap[LongMap[TransactionLogShipping]] = {
    summaries.foldLeft(
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
  }

  def getDetailByTranSiteId(
    summaries: Seq[TransactionSummaryEntry]
  )(
    implicit conn: Connection, lang: Lang
  ): LongMap[Seq[TransactionDetail]] = summaries.foldLeft(LongMap[Seq[TransactionDetail]]()) {
    (sum, e) =>
    val details = TransactionDetail.show(e.transactionSiteId, LocaleInfo.getDefault)
    sum.updated(e.transactionSiteId, details)
  }

  def getSiteTranByTranId(
    summaries: Seq[TransactionSummaryEntry], lang: Lang
  )(
    implicit conn: Connection
  ): LongMap[PersistedTransaction] = summaries.foldLeft(LongMap[PersistedTransaction]()) {
    (sum, e) =>
    val siteTran = (new TransactionPersister).load(e.transactionId, LocaleInfo.getDefault(lang))
    sum.updated(e.transactionId, siteTran)
  }

  def getUserDropDown(
    summaries: Seq[TransactionSummaryEntry]
  )(
    implicit lang: Lang
  ): Seq[(String, String)] = AllUser +: summaries.foldLeft(Set[StoreUser]()) {
    (set, e) => set + e.buyer
  }.map {
    u => (u.id.get.toString, u.userName)
  }.toSeq
}
