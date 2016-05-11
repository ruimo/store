package helpers

import org.openqa.selenium.WebDriver

class SimpleChromeWebDriverFactory extends SeleniumWebDriverFactory {
  def create(): WebDriver = new org.openqa.selenium.remote.RemoteWebDriver(
    new java.net.URL("http://127.0.0.1:4444/wd/hub"), SimpleChromeWebDriverFactory.caps
  )
}

object SimpleChromeWebDriverFactory {
  val caps = org.openqa.selenium.remote.DesiredCapabilities.chrome()
}


