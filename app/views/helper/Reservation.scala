package views.helper

import models._

object Reservation {
  def isReservationItem(metadata: Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]): Boolean =
    metadata.get(SiteItemNumericMetadataType.RESERVATION_ITEM).map(_.metadata != 0).getOrElse(false)
}
