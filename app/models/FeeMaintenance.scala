package models

import org.joda.time.DateTime

case class FeeMaintenance(
  boxId: Long,
  now: DateTime
)
