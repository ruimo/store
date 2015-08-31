package controllers

import scala.collection.immutable
import helpers.Cache
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
  val UseShippingDateForAccountingBill: () => Boolean = Cache.cacheOnProd(
    Cache.Conf.getBoolean("useShippingDateForAccountingBill").getOrElse(false)
  )

  val accountingBillForm = Form(
    mapping(
      "year" -> number(min = YearMonth.MinYear, max = YearMonth.MaxYear),
      "month" -> number(min = 1, max = 12),
      "user" -> longNumber,
      "command" -> text
    )(YearMonthUser.apply)(YearMonthUser.unapply)
  )

  val accountingBillForStoreForm = Form(
    mapping(
      "year" -> number(min = YearMonth.MinYear, max = YearMonth.MaxYear),
      "month" -> number(min = 1, max = 12),
      "site" -> longNumber,
      "command" -> text
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
        logger.error("Validation error in AccountingBill.showForStore() " + formWithErrors)
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
          val table: AccountingBillTable = TransactionSummary.accountingBillForUser(
            login.siteUser.map(_.siteId),
            yearMonth,
            yearMonth.userIdOpt,
            request2lang,
            UseShippingDateForAccountingBill()
          )

          if (yearMonth.command == "csv") {
            implicit val cs = play.api.mvc.Codec.javaSupported("Windows-31j")
            val fileName = "fileName.csv"

            Ok(
              createCsv(table.summaries, table.siteTranByTranId, false)
            ).as(
              "text/csv charset=Shift_JIS"
            ).withHeaders(
              CONTENT_DISPOSITION -> ("""attachment; filename="%s"""".format(fileName))
            )
          }
          else {
            Ok(views.html.accountingBill(
              accountingBillForm.fill(yearMonth),
              accountingBillForStoreForm,
              table.summaries,
              TransactionSummary.getDetailByTranSiteId(table.summaries),
              getBoxBySiteAndItemSize(table.summaries),
              table.siteTranByTranId,
              false,
              Site.tableForDropDown,
              getAddressTable(table.siteTranByTranId),
              getUserDropDown(table.summariesForAllUser)
            ))
          }
        }
      }
    )
  }

  def showForStore() = NeedAuthenticated { implicit request =>
    implicit val login = request.user

    accountingBillForStoreForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in AccountingBill.showForStore() " + formWithErrors)
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
            onlyShipped = true, useShippedDate = UseShippingDateForAccountingBill()
          )
          val siteTranByTranId = TransactionSummary.getSiteTranByTranId(summaries, request2lang)
          val useCostPrice = true

          if (yearMonthSite.command == "csv") {
            implicit val cs = play.api.mvc.Codec.javaSupported("Windows-31j")
            val fileName = "fileName.csv"

            Ok(
              createCsv(summaries, siteTranByTranId, useCostPrice)
            ).as(
              "text/csv charset=Shift_JIS"
            ).withHeaders(
              CONTENT_DISPOSITION -> ("""attachment; filename="%s"""".format(fileName))
            )
          }
          else {
            Ok(views.html.accountingBill(
              accountingBillForm,
              accountingBillForStoreForm.fill(yearMonthSite),
              summaries,
              TransactionSummary.getDetailByTranSiteId(summaries),
              getBoxBySiteAndItemSize(summaries),
              siteTranByTranId,
              useCostPrice,
              Site.tableForDropDown,
              getAddressTable(siteTranByTranId),
              getUserDropDown(summaries)
            ))
          }
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

  def getUserDropDown(
    summaries: Seq[TransactionSummaryEntry]
  )(
    implicit lang: Lang
  ): Seq[(String, String)] = AllUser +: summaries.foldLeft(Set[StoreUser]()) {
    (set, e) => set + e.buyer
  }.map {
    u => (u.id.get.toString, u.userName)
  }.toSeq

  def createCsv(
    summaries: Seq[TransactionSummaryEntry],
    siteTranByTranId: immutable.LongMap[PersistedTransaction],
    useCostPrice: Boolean
  ): String = {
    class Rec {
      var userName: String = _
      var itemSubtotal: BigDecimal = BigDecimal(0)
      var tax: BigDecimal = BigDecimal(0)
      var fee: BigDecimal = BigDecimal(0)
      def total = itemSubtotal + tax + fee
    }

    val rows = summaries.foldLeft(immutable.LongMap[Rec]().withDefault(idx => new Rec)) { (sum, e) =>
      val rec = sum(e.buyer.id.get)

      rec.userName = e.buyer.fullName
      rec.itemSubtotal += (
        if (useCostPrice) siteTranByTranId(e.transactionId).costPriceTotal
        else siteTranByTranId(e.transactionId).itemTotal
      )(e.siteId)
      rec.tax += (
        if (useCostPrice) siteTranByTranId(e.transactionId).outerTaxWhenCostPrice
        else siteTranByTranId(e.transactionId).outerTaxTotal
      )(e.siteId)
      rec.fee += siteTranByTranId(e.transactionId).boxTotal(e.siteId)

      sum.updated(e.buyer.id.get, rec)
    }.foldLeft(new StringBuilder) { (buf, e) =>
      buf.append(e._1)
        .append(',').append('"').append(e._2.userName).append('"')
        .append(',').append(e._2.itemSubtotal)
        .append(',').append(e._2.tax)
        .append(',').append(e._2.fee)
        .append(',').append(e._2.total)
        .append("\r\n")
    }.toString

    "userId,userName,itemTotal,outerTax,fee,grandTotal\r\n" + rows
  }
}
