package models

sealed abstract class UserType(val typeCode: UserTypeCode)

case object Buyer extends UserType(UserTypeCode.BUYER)

case object SuperUser extends UserType(UserTypeCode.SUPER_USER)

case class SiteOwner(siteUser: SiteUser) extends UserType(UserTypeCode.SITE_OWNER)

case object AnonymousBuyer extends UserType(UserTypeCode.ANONYMOUS_BUYER)
