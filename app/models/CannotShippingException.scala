package models

class CannotShippingException(
  val siteId: Long, val locationCode: Int, val itemClass: Option[Long]
) extends Exception {
  def this (siteId: Long, locationCode: Int) = this (siteId, locationCode, None)
  def this (siteId: Long, locationCode: Int, itemClass: Long)
    = this (siteId, locationCode, Some(itemClass))

  def isCannotShip(siteId: Long, locationCode: Int, itemClass: Long): Boolean = 
    this.siteId == siteId && this.locationCode == locationCode && (this.itemClass match {
      case None => true
      case Some(c) => c == itemClass
    })
}

