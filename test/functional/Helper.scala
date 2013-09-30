package functional

import play.api.db.DB
import models.{UserRole, StoreUser}
import play.api.Play.current
import play.api.test.TestBrowser

object Helper {
  // password == password
  def createTestUser(): StoreUser = DB.withConnection { implicit conn =>
    StoreUser.create(
      "administrator", "Admin", None, "Manager", "admin@abc.com",
      4151208325021896473L, -1106301469931443100L, UserRole.ADMIN
    )
  }

  def loginWithTestUser(browser: TestBrowser): StoreUser = {
    val user = createTestUser()
    browser.goTo("http://localhost:3333" + controllers.routes.Admin.index.url)
    browser.fill("#userName").`with`("administrator")
    browser.fill("#password").`with`("password")
    browser.click("#doLoginButton")

    user
  }
}
