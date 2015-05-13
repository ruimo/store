package models

import java.sql.Connection

trait CreateItemInquiryReservation {
  def siteId: Long
  def itemId: Long
  def name: String
  def email: String
  def message: String
  def save(user: StoreUser)(implicit conn: Connection): ItemInquiry
  def update(id: ItemInquiryId)(implicit conn: Connection): Int
}

case class CreateItemInquiry(
  siteId: Long,
  itemId: Long,
  name: String,
  email: String,
  message: String
) extends CreateItemInquiryReservation {
  def save(user: StoreUser)(implicit conn: Connection): ItemInquiry = {
    val inq = ItemInquiry.createNew(
      siteId, ItemId(itemId), user.id.get, ItemInquiryType.QUERY, name, email, System.currentTimeMillis
    )

    ItemInquiryField.createNew(inq.id.get, Map('Message -> message))
    inq
  }

  def update(id: ItemInquiryId)(implicit conn: Connection): Int = {
    val updateCount = ItemInquiry.update(id, name, email)
    ItemInquiryField.update(id, Map('Message -> message))
    updateCount
  }
}

case class CreateItemReservation(
  siteId: Long,
  itemId: Long,
  name: String,
  email: String,
  message: String
) extends CreateItemInquiryReservation {
  def save(user: StoreUser)(implicit conn: Connection): ItemInquiry = {
    val inq = ItemInquiry.createNew(
      siteId, ItemId(itemId), user.id.get, ItemInquiryType.RESERVATION, name, email, System.currentTimeMillis
    )

    ItemInquiryField.createNew(inq.id.get, Map('Message -> message))
    inq
  }

  def update(id: ItemInquiryId)(implicit conn: Connection): Int = {
    val updateCount = ItemInquiry.update(id, name, email)
    ItemInquiryField.update(id, Map('Message -> message))
    updateCount
  }
}

