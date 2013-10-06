package models

import java.sql.Connection

case class LoginSession(storeUser: StoreUser, siteUser: Option[SiteUser], expireTime: Long) extends NotNull {
  lazy val userId = storeUser.id.get
  def withExpireTime(newExpireTime: Long) = LoginSession(storeUser, siteUser, newExpireTime)
  def toSessionString = storeUser.id.get + ";" + expireTime
  lazy val role: Role = storeUser.userRole match {
    case UserRole.ADMIN => SuperUser
    case UserRole.NORMAL => siteUser match {
      case None => Buyer
      case Some(u) => SiteOwner(u)
    }
  }
  lazy val isSuperUser = role == SuperUser
  lazy val isAdmin = role != Buyer
}

object LoginSession {
  def apply(sessionString: String)(implicit conn: Connection): LoginSession = {
    val args = sessionString.split(';').map(_.toLong)
    val storeSiteUser = StoreUser.withSite(args(0))
    LoginSession(storeSiteUser._1, storeSiteUser._2, args(1))
  }

  def serialize(storeUserId: Long, expirationTime: Long): String = storeUserId + ";" + expirationTime
}
