package models

sealed trait UserType {
    val typeCode: Int
}

case object Buyer extends UserType {
  val typeCode = 0
}

case object SuperUser extends UserType {
  val typeCode = 1
}

case class SiteOwner(siteUser: SiteUser) extends UserType with NotNull {
  val typeCode = 2
}
