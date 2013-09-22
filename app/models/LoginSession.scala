package models;

case class LoginSession(userId: Long, expireTime: Long) {
  def withExpireTime(newExpireTime: Long) = LoginSession(userId, newExpireTime)
  def toSessionString = userId + ";" + expireTime
}

object LoginSession {
  def apply(sessionString: String): LoginSession = {
    val args = sessionString.split(';').map(_.toLong)
    LoginSession(args(0), args(1))
  }
}
