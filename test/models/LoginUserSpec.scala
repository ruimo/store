package models

import org.specs2.mutable._

class LoginUserSpec extends Specification {
  "LoginUser" should {
    "Compound user name is set." in {
      LoginUser(None, "userName", "password", "uri").compoundUserName === "userName"
      LoginUser(Some("company"), "userName", "password", "uri").compoundUserName === "company-userName"
    }
  }
}
