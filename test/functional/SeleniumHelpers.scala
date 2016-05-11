package functional

import play.api.test.WebDriverFactory
import java.net.URI
import java.util.{ArrayList, Arrays}
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import play.api.test.{TestBrowser, TestServer}
import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxProfile}
import helpers.Cache
import helpers.SeleniumWebDriverFactory

object SeleniumHelpers {
  def FirefoxJa = {
    val profile = new FirefoxProfile
    profile.setPreference("general.useragent.locale", "ja")
    profile.setPreference("intl.accept_languages", "ja, en")
    new FirefoxDriver(profile)
  }

  val DefaultWebDriverFactory: Option[String] = Option(
    System.getProperty("defaultWebDriverFactory")
  )

  def webDriver[WD <: WebDriver](clazz: Class[WD]): WebDriver = DefaultWebDriverFactory.map { className =>
    Class.forName(className).newInstance().asInstanceOf[SeleniumWebDriverFactory].create()
  }.getOrElse(WebDriverFactory(clazz))

  def running[T](testServer: TestServer, webDriver: WebDriver)(block: TestBrowser => T): T = {
    var browser: TestBrowser = null
    synchronized {
      try {
        testServer.start()
        browser = TestBrowser(webDriver, None)
        block(browser)
      } finally {
        if (browser != null) {
          browser.quit()
        }
        testServer.stop()
      }
    }
  }

  def htmlUnit(): HtmlUnitDriver = {
    val htmlUnit = new HtmlUnitDriver()
    val proxy: String = System.getenv("http_proxy")
    if (proxy != null) {
      val url: URI = new URI(proxy)
      htmlUnit.setHTTPProxy(url.getHost(), url.getPort(), new ArrayList[String](Arrays.asList("localhost")))
    }

    htmlUnit.setJavascriptEnabled(true)
    htmlUnit
  }
}
