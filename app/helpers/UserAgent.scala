package helpers

import play.api.mvc._
import scala.collection.immutable

sealed trait UserAgent

object UserAgent {
  case object IPhone extends UserAgent
  case object IPad extends UserAgent
  case object Android extends UserAgent

  def apply(uaStr: String): immutable.Set[UserAgent] = {
    var ret = Set[UserAgent]()
    if (uaStr.indexOf("iPhone") != -1) {
      ret = ret + UserAgent.IPhone
    }
    else if (uaStr.indexOf("iPad") != -1) {
      ret = ret + UserAgent.IPad
    }
    else if (uaStr.indexOf("Android") != -1) {
      ret = ret + UserAgent.Android
    }
    ret
  }

  def userAgent(implicit request: RequestHeader): immutable.Set[UserAgent] =
    apply(request.headers.get("User-Agent").getOrElse(""))
}
