package models

trait CreateItemInquiryReservation {
  def siteId: Long
  def itemId: Long
  def name: String
  def email: String
}

case class CreateItemInquiry(
  siteId: Long,
  itemId: Long,
  name: String,
  email: String
) extends CreateItemInquiryReservation


