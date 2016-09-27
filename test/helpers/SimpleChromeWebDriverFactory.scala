package helpers

import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.remote.LocalFileDetector
import org.openqa.selenium.WebDriver

class SimpleChromeWebDriverFactory extends SeleniumWebDriverFactory {
  def create(): WebDriver = {
    val wd = new org.openqa.selenium.remote.RemoteWebDriver(
      new java.net.URL("http://127.0.0.1:4444/wd/hub"), SimpleChromeWebDriverFactory.caps
    )
    wd.setFileDetector(new LocalFileDetector)
    wd
  }
}

object SimpleChromeWebDriverFactory {
  val caps = org.openqa.selenium.remote.DesiredCapabilities.chrome()
}


