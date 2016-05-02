package helpers

object Sanitize {
  def forUrl(url: String): String = 
    if (url.trim.startsWith("//")) "/"
    else if (url.indexOf("://") != -1) "/"
    else if (url.indexOf("csrfToken=") != -1) "/"
    else url
}
