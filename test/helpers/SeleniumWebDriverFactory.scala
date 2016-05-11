package helpers

import org.openqa.selenium.WebDriver

trait SeleniumWebDriverFactory {
  def create(): WebDriver
}
