package models

import anorm._
import java.sql.Connection
import scala.language.postfixOps
import scala.collection.immutable
import play.api.i18n.Lang

case class TransactionSummaryEntry(
  transactionId: Long,
  transactionSiteId: Long,
  transactionTime: Long,
  totalAmount: BigDecimal,
  totalTax: BigDecimal,
  address: Option[Address],
  siteId: Long,
  siteName: String,
  shippingFee: BigDecimal,
  status: TransactionStatus,
  statusLastUpdate: Long,
  shipStatus: Option[ShippingInfo],
  buyer: StoreUser,
  shippingDate: Option[Long],
  mailSent: Boolean
) {
  lazy val totalWithTax = totalAmount + totalTax
}

case class AccountingBillTable(
  userId: Option[Long],
  summariesForAllUser: Seq[TransactionSummaryEntry],
  summaries: Seq[TransactionSummaryEntry],
  siteTranByTranId: immutable.LongMap[PersistedTransaction]
)

object TransactionSummary {
  val ListDefaultOrderBy = OrderBy("base.transaction_time", Desc)
  val parser = {
    SqlParser.get[Long]("transaction_id") ~
    SqlParser.get[Long]("transaction_site_id") ~
    SqlParser.get[java.util.Date]("transaction_time") ~
    SqlParser.get[java.math.BigDecimal]("total_amount") ~
    SqlParser.get[java.math.BigDecimal]("tax_amount") ~
    (Address.simple ?) ~
    SqlParser.get[Long]("site_id") ~
    SqlParser.get[String]("site_name") ~
    SqlParser.get[java.math.BigDecimal]("shipping") ~
    SqlParser.get[Int]("status") ~
    StoreUser.simple ~
    SqlParser.get[Option[java.util.Date]]("shipping_date") ~
    SqlParser.get[java.util.Date]("transaction_status.last_update") ~ 
    SqlParser.get[Option[Long]]("transaction_status.transporter_id") ~
    SqlParser.get[Option[String]]("transaction_status.slip_code") ~
    SqlParser.get[Boolean]("transaction_status.mail_sent") map {
      case id~tranSiteId~time~amount~tax~address~siteId~siteName~shippingFee~status~user~shippingDate~lastUpdate~transporterId~slipCode~mailSent =>
        TransactionSummaryEntry(
          id, tranSiteId, time.getTime, amount, tax, address, siteId, siteName,
          shippingFee, TransactionStatus.byIndex(status), lastUpdate.getTime,
          if (transporterId.isDefined) Some(ShippingInfo(transporterId.get, slipCode.get)) else None,
          user, shippingDate.map(_.getTime), mailSent
        )
    }
  }

  val baseColumns = """
      transaction_id,
      base.transaction_site_id,
      transaction_time,
      total_amount,
      tax_amount,
      address.*,
      site.site_id,
      site.site_name,
      shipping,
      coalesce(transaction_status.status, 0) status,
      transaction_status.last_update,
      transaction_status.transporter_id,
      transaction_status.slip_code,
      transaction_status.mail_sent,
      store_user.*,
      base.shipping_date
  """

  def baseSql(
    siteId: Option[Long],
    withLimit: Boolean,
    storeUserId: Option[Long] = None,
    tranId: Option[Long] = None,
    additionalWhere: String = "",
    orderByOpt: Seq[OrderBy] = List(ListDefaultOrderBy),
    forCount: Boolean = false,
    columns: String = baseColumns
  ) =
    """
    select 
    """ +
    columns +
    """
    from (
      select            
        transaction_header.transaction_id,
        transaction_header.transaction_time,
        transaction_site.total_amount,
        transaction_site.site_id,
        transaction_site.transaction_site_id,
        transaction_header.store_user_id,
        max(transaction_shipping.shipping_date) shipping_date,
        min(coalesce(transaction_shipping.address_id, 0)) address_id,
        sum(coalesce(transaction_shipping.amount, 0)) shipping,
        sum(coalesce(transaction_tax.amount, 0)) tax_amount
      from transaction_header
      inner join transaction_site on
        transaction_header.transaction_id = transaction_site.transaction_id
      left join transaction_shipping on
        transaction_shipping.transaction_site_id = transaction_site.transaction_site_id
      left join transaction_tax on
        transaction_tax.transaction_site_id = transaction_site.transaction_site_id and
        transaction_tax.tax_type = """ + TaxType.OUTER_TAX.ordinal +
    " where 1 = 1" +
    siteId.map {id => " and transaction_site.site_id = " + id}.getOrElse("") +
    storeUserId.map {uid => " and transaction_header.store_user_id = " + uid}.getOrElse("") +
    tranId.map {id => " and transaction_header.transaction_id = " + id}.getOrElse("") +
    """
      group by
        transaction_header.transaction_id,
        transaction_header.transaction_time,
        transaction_site.total_amount,
        transaction_site.tax_amount,
        transaction_site.site_id,
        transaction_site.transaction_site_id,
        transaction_header.store_user_id
    ) base
    left join address on address.address_id = base.address_id
    inner join site on site.site_id = base.site_id
    inner join store_user on base.store_user_id = store_user.store_user_id
    left join transaction_status
      on transaction_status.transaction_site_id = base.transaction_site_id
    """ +
    additionalWhere +
    (if (forCount) ""
     else " order by " + orderByOpt.map {o => s"$o, "}.mkString("") + "base.site_id asc ") +
    (if (withLimit) "limit {limit} offset {offset}" else "")

  def list(
    siteId: Option[Long] = None, storeUserId: Option[Long] = None,
    tranId: Option[Long] = None,
    page: Int = 0, pageSize: Int = 25, orderBy: OrderBy = ListDefaultOrderBy
  )(implicit conn: Connection): PagedRecords[TransactionSummaryEntry] = {
    val count = SQL(
      baseSql(columns = "count(*)", siteId = siteId, storeUserId = storeUserId, tranId = tranId,
              additionalWhere = "", withLimit = false, orderByOpt = List(), forCount = true)
    ).as(
      SqlParser.scalar[Long].single
    )

    PagedRecords[TransactionSummaryEntry] (
      page,
      pageSize,
      (count + pageSize - 1) / pageSize,
      orderBy,
      SQL(
        baseSql(
          siteId = siteId, storeUserId = storeUserId, tranId = tranId,
          additionalWhere = "", withLimit = true, orderByOpt = List(orderBy)
        )
      ).on(
        'limit -> pageSize,
        'offset -> page * pageSize
      ).as(
        parser *
      )
    )
  }

  def listByPeriod(
    siteId: Option[Long] = None,
    storeUserId: Option[Long] = None,
    yearMonth: HasYearMonth,
    onlyShipped: Boolean = false,
    useShippedDate:Boolean = false
  )(implicit conn: Connection): Seq[TransactionSummaryEntry] = {
    val nextYearMonth = yearMonth.next
    val dateCol = if (useShippedDate) "transaction_status.last_update" else "transaction_time"

    SQL(
      baseSql(
        siteId = siteId,
        storeUserId = storeUserId,
        additionalWhere = (
          "where date '%d-%02d-01' <= " + dateCol + " and " + dateCol + " < date '%d-%02d-01'"
        ).format(
          yearMonth.year, yearMonth.month, nextYearMonth.year, nextYearMonth.month
        ) + (
          if (onlyShipped) " and transaction_status.status = " + TransactionStatus.SHIPPED.ordinal else ""
        ),
        orderByOpt = List(OrderBy("base.store_user_id", Asc), ListDefaultOrderBy),
        withLimit = false
      )
    ).as(
      parser *
    )
  }

  def get(siteId: Option[Long], tranSiteId: Long)(implicit conn: Connection): Option[TransactionSummaryEntry] =
    SQL(
      baseSql(siteId = siteId, additionalWhere = "where base.transaction_site_id = {tranSiteId}", withLimit = false)
    ).on(
      'tranSiteId -> tranSiteId
    ).as(
      parser.singleOpt
    )

  def accountingBillForUser(
    siteId: Option[Long], yearMonth: HasYearMonth, userId: Option[Long], lang: Lang, userShippedDate: Boolean
  )(
    implicit conn: Connection
  ): AccountingBillTable = {
    val summariesForAllUser: Seq[TransactionSummaryEntry] =
      TransactionSummary.summaryForAllUser(yearMonth, userId, siteId, userShippedDate)
    val summaries = TransactionSummary.summaryForUser(userId, summariesForAllUser)
    val siteTranByTranId = TransactionSummary.getSiteTranByTranId(summaries, lang)

    AccountingBillTable(
      userId,
      summariesForAllUser,
      summaries,
      siteTranByTranId
    )
  }

  def summaryForAllUser(
    yearMonth: HasYearMonth, userId: Option[Long], siteId: Option[Long], userShippedDate: Boolean
  )(
    implicit conn: Connection
  ): Seq[TransactionSummaryEntry] = {
    val summariesForAllUser: Seq[TransactionSummaryEntry] = TransactionSummary.listByPeriod(
      siteId = siteId,
      yearMonth = yearMonth,
      onlyShipped = true, useShippedDate = userShippedDate
    )
    summariesForAllUser
  }

  def summaryForUser(
    userId: Option[Long], summariesForAllUser: Seq[TransactionSummaryEntry]
  ): Seq[TransactionSummaryEntry] = {
    userId match {
      case Some(userId) => summariesForAllUser.filter(_.buyer.id.get == userId)
      case None => summariesForAllUser
    }
  }

  def getSiteTranByTranId(
    summaries: Seq[TransactionSummaryEntry], lang: Lang
  )(
    implicit conn: Connection
  ): immutable.LongMap[PersistedTransaction] = summaries.foldLeft(immutable.LongMap[PersistedTransaction]()) {
    (sum, e) =>
    val siteTran = (new TransactionPersister).load(e.transactionId, LocaleInfo.getDefault(lang))
    sum.updated(e.transactionId, siteTran)
  }
}
