package models

import java.sql.Connection

case class LoginSession(storeUser: StoreUser, siteUser: Option[SiteUser], expireTime: Long) extends NotNull {
  lazy val user = User(storeUser, siteUser)
  lazy val userId = storeUser.id.get
  def withExpireTime(newExpireTime: Long) = LoginSession(storeUser, siteUser, newExpireTime)
  def toSessionString = storeUser.id.get + ";" + expireTime
  lazy val role: UserType = user.userType
  lazy val isBuyer = role == Buyer
  lazy val isSuperUser = role == SuperUser
  lazy val isAdmin = role != Buyer
}

object LoginSession {
  def apply(sessionString: String)(implicit conn: Connection): LoginSession = {
    val args = sessionString.split(';').map(_.toLong)
    val storeSiteUser = StoreUser.withSite(args(0))
    LoginSession(storeSiteUser.user, storeSiteUser.siteUser, args(1))
  }

  def serialize(storeUserId: Long, expirationTime: Long): String = storeUserId + ";" + expirationTime
}
