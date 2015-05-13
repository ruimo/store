package helpers

import scala.language.implicitConversions

class UrlHelper(url: String) {
  def addParm(key: String, value: String): UrlHelper = new UrlHelper(
    url + (if (url.indexOf('?') != -1) '&' else '?') + key + '=' + value
  )

  override def toString = url
}

object UrlHelper {
  implicit def fromString(from: String) = new UrlHelper(from)
}

