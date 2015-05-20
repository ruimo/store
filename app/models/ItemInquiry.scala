package models

import scala.collection.immutable
import play.api.Logger
import anorm._
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import helpers.PasswordHash
import java.sql.Connection

case class ItemInquiryId(id: Long) extends AnyVal

case class ItemInquiryFieldId(id: Long) extends AnyVal

case class ItemInquiry(
  id: Option[ItemInquiryId] = None,
  siteId: Long,
  itemId: ItemId,
  storeUserId: Long,
  inquiryType: ItemInquiryType,
  submitUserName: String,
  email: String,
  status: ItemInquiryStatusType,
  created: Long
)

case class ItemInquiryField(
  id: Option[ItemInquiryFieldId] = None,
  itemInquiryId: Long,
  fieldName: String,
  fieldValue: String
)

object ItemInquiry {
  val simple = {
    SqlParser.get[Option[Long]]("item_inquiry.item_inquiry_id") ~
    SqlParser.get[Long]("item_inquiry.site_id") ~
    SqlParser.get[Long]("item_inquiry.item_id") ~
    SqlParser.get[Long]("item_inquiry.store_user_id") ~
    SqlParser.get[Int]("item_inquiry.inquiry_type") ~
    SqlParser.get[String]("item_inquiry.submit_user_name") ~
    SqlParser.get[String]("item_inquiry.email") ~
    SqlParser.get[Int]("item_inquiry.status") ~
    SqlParser.get[java.util.Date]("item_inquiry.created") map {
      case id~siteId~itemId~userId~inquiryType~submitUserName~email~status~created =>
        ItemInquiry(
          id.map(ItemInquiryId.apply), siteId, ItemId(itemId), userId,
          ItemInquiryType.byIndex(inquiryType), 
          submitUserName, email, ItemInquiryStatus.byIndex(status), created.getTime
        )
    }
  }

  def apply(id: ItemInquiryId)(implicit conn: Connection): ItemInquiry =
    SQL(
      "select * from item_inquiry where item_inquiry_id = {id}"
    ).on(
      'id -> id.id
    ).as(ItemInquiry.simple.single)

  def createNew(
    siteId: Long, itemId: ItemId, userId: Long, inquiryType: ItemInquiryType, 
    submitUserName: String, email: String, created: Long
  )(implicit conn: Connection): ItemInquiry = {
    SQL(
      """
      insert into item_inquiry (
        item_inquiry_id, site_id, item_id, store_user_id, inquiry_type, submit_user_name, email, status, created
      ) values (
        (select nextval('item_inquiry_seq')),
        {siteId}, {itemId}, {userId}, {inquiryType}, {submitUserName}, {email}, """ + 
      ItemInquiryStatus.DRAFT.ordinal +
      """, {created}
      )
      """
    ).on(
      'siteId -> siteId,
      'itemId -> itemId.id,
      'userId -> userId,
      'inquiryType -> inquiryType.ordinal,
      'submitUserName -> submitUserName,
      'email -> email,
      'created -> new java.util.Date(created)
    ).executeUpdate()

    val id = SQL("select currval('item_inquiry_seq')").as(SqlParser.scalar[Long].single)
    ItemInquiry(
      Some(ItemInquiryId(id)), siteId, itemId, userId, inquiryType, submitUserName, email, ItemInquiryStatus.DRAFT, created
    )
  }

  def changeStatus(
    id: ItemInquiryId, status: ItemInquiryStatusType
  )(
    implicit conn: Connection
  ): Int = SQL(
    """
    update item_inquiry set status = {status} where item_inquiry_id = {id}
    """
  ).on(
    'status -> status.code,
    'id -> id.id
  ).executeUpdate()

  def update(
    id: ItemInquiryId, name: String, email: String
  )(
    implicit conn: Connection
  ): Int = SQL(
    """
    update item_inquiry set submit_user_name = {name}, email = {email} where item_inquiry_id = {id}
    """
  ).on(
    'name -> name,
    'email -> email,
    'id -> id.id
  ).executeUpdate()
}

object ItemInquiryField {
  val simple = {
    SqlParser.get[Option[Long]]("item_inquiry_field.item_inquiry_field_id") ~
    SqlParser.get[Long]("item_inquiry_field.item_inquiry_id") ~
    SqlParser.get[String]("item_inquiry_field.field_name") ~
    SqlParser.get[String]("item_inquiry_field.field_value") map {
      case id~itemInquiryId~fieldName~fieldValue =>
        ItemInquiryField(
          id.map(ItemInquiryFieldId.apply), itemInquiryId, fieldName, fieldValue
        )
    }
  }

  def apply(id: ItemInquiryId)(implicit conn: Connection): immutable.Map[Symbol, String] =
    SQL(
      "select * from item_inquiry_field where item_inquiry_id = {id}"
    ).on(
      'id -> id.id
    ).as(
      ItemInquiryField.simple *
    ).map { e =>
      (Symbol(e.fieldName), e.fieldValue)
    }.toMap

  def createNew(
    id: ItemInquiryId, fields: immutable.Map[Symbol, String]
  )(
    implicit conn: Connection
  ) {
    if (! fields.isEmpty) {
      BatchSql(
        """
        insert into item_inquiry_field (
          item_inquiry_field_id, item_inquiry_id, field_name, field_value
        ) values (
          (select nextval('item_inquiry_field_seq')),
          {itemInquiryId}, {fieldName}, {fieldValue}
        )
        """,
        Seq[NamedParameter](
          'itemInquiryId -> id.id, 'fieldName -> fields.head._1.name, 'fieldValue -> fields.head._2
        ),
        fields.tail.map { e =>
          Seq[NamedParameter](
            'itemInquiryId -> id.id, 'fieldName -> e._1.name, 'fieldValue -> e._2
          )
        }.toSeq: _*
      ).execute()
    }
  }

  def update(
    id: ItemInquiryId, fields: immutable.Map[Symbol, String]
  )(
    implicit conn: Connection
  ) {
    SQL(
      """
      delete from item_inquiry_field where item_inquiry_id = {id}
      """
    ).on(
      'id -> id.id
    ).executeUpdate()

    createNew(id, fields)
  }
}
