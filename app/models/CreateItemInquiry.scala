package models

trait CreateItemInquiryReservation {
  def siteId: Long
  def itemId: Long
  def name: String
  def email: String
  def inquiryBody: String
}

case class CreateItemInquiry(
  siteId: Long,
  itemId: Long,
  name: String,
  email: String,
  inquiryBody: String
) extends CreateItemInquiryReservation
