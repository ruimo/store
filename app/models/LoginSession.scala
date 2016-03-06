package models

import play.api.Play.current
import anorm._
import java.sql.Connection

import play.api.db.DB

case class LoginSession(storeUser: StoreUser, siteUser: Option[SiteUser], expireTime: Long) {
  lazy val user = User(storeUser, siteUser)
  lazy val userId = storeUser.id.get
  def withExpireTime(newExpireTime: Long) = LoginSession(storeUser, siteUser, newExpireTime)
  def toSessionString = storeUser.id.get + ";" + expireTime
  lazy val role: UserType = user.userType
  lazy val isBuyer = role == Buyer
  lazy val isSuperUser = role == SuperUser
  lazy val isAdmin = role != Buyer
  lazy val isAnonymousBuyer = role == AnonymousBuyer
  lazy val isSiteOwner = isAdmin && (! isSuperUser)
  def quantityInShoppingCart: Long = DB.withConnection { implicit conn =>
    ShoppingCartItem.quantityForUser(storeUser.id.get)
  }
  def update(addr: CreateAddress)(implicit conn: Connection) {
    StoreUser.update(
      storeUser.id.get,
      storeUser.userName, 
      addr.firstName,
      if (addr.middleName.isEmpty) None else Some(addr.middleName),
      addr.lastName,
      addr.email,
      storeUser.passwordHash,
      storeUser.salt,
      storeUser.companyName
    )
  }
}

object LoginSession {
  def apply(sessionString: String)(implicit conn: Connection): LoginSession = {
    val args = sessionString.split(';').map(_.toLong)
    val storeSiteUser = StoreUser.withSite(args(0))
    LoginSession(storeSiteUser.user, storeSiteUser.siteUser, args(1))
  }

  def serialize(storeUserId: Long, expirationTime: Long): String = storeUserId + ";" + expirationTime
}
