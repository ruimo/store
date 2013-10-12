package models

class CannotShippingException(
  val site: Site, val locationCode: Int, val itemClass: Option[Long]
) extends Exception {
  def this (site: Site, locationCode: Int) = this (site, locationCode, None)
  def this (site: Site, locationCode: Int, itemClass: Long)
    = this (site, locationCode, Some(itemClass))

  def isCannotShip(site: Site, locationCode: Int, itemClass: Long): Boolean = 
    this.site == site && this.locationCode == locationCode && (this.itemClass match {
      case None => true
      case Some(c) => c == itemClass
    })
}

