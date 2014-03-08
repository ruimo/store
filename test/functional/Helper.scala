package functional

import play.api.db.DB
import models.{UserRole, StoreUser}
import play.api.Play.current
import play.api.test.TestBrowser
import collection.mutable.ListBuffer
import annotation.tailrec
import java.io.BufferedReader

object Helper {
  val disableMailer = Map("disable.mailer" -> true)

  // password == password
  def createTestUser(): StoreUser = DB.withConnection { implicit conn =>
    StoreUser.create(
      "administrator", "Admin", None, "Manager", "admin@abc.com",
      4151208325021896473L, -1106301469931443100L, UserRole.ADMIN, Some("Company1")
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

  def takeScreenShot(browser: TestBrowser) {
    val stack = (new Throwable()).getStackTrace()(1)
    val fname = "screenShots/" + stack.getFileName() + "_" + stack.getLineNumber() + ".png"
    browser.takeScreenShot(fname)
  }

  def doWith[T](arg: T)(func: T => Unit) {
    func(arg)
  }

  def readFully(br: BufferedReader): Seq[String] = {
    @tailrec def readFully(buf: ListBuffer[String]): List[String] = {
      val line = br.readLine
      if (line == null) buf.toList
      else {
        buf += line
        readFully(buf)
      }
    }

    readFully(new ListBuffer[String])
  }
}
