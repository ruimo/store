package helpers

import play.api.db.DB
import models.{UserRole, StoreUser}
import play.api.Play.current
import play.api.test.TestBrowser
import java.io.{ByteArrayOutputStream, InputStreamReader, BufferedReader, InputStream}
import java.net.{HttpURLConnection, URL}
import collection.mutable.ListBuffer
import annotation.tailrec

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

  def login(browser: TestBrowser, userName: String, password: String) {
    browser.goTo("http://localhost:3333" + controllers.routes.Admin.index.url)
    browser.fill("#userName").`with`(userName)
    browser.fill("#password").`with`(password)
    browser.click("#doLoginButton")
  }

  def logoff(browser: TestBrowser) {
    browser.goTo("http://localhost:3333" + controllers.routes.Admin.logoff("/").url)
  }

  def createNormalUser(
    browser: TestBrowser,
    userName: String,
    password: String,
    email: String,
    firstName: String,
    lastName: String,
    companyName: String
  ) {
    browser.goTo("http://localhost:3333" + controllers.routes.UserMaintenance.startCreateNewNormalUser.url)
    browser.fill("#userName").`with`(userName)
    browser.fill("#firstName").`with`(firstName)
    browser.fill("#lastName").`with`(lastName)
    browser.fill("#companyName").`with`(companyName)
    browser.fill("#email").`with`(email)
    browser.fill("#password_main").`with`(password)
    browser.fill("#password_confirm").`with`(password)
    browser.click("#registerNormalUser")
  }

  def takeScreenShot(browser: TestBrowser) {
    val stack = (new Throwable()).getStackTrace()(1)
    val fname = "screenShots/" + stack.getFileName() + "_" + stack.getLineNumber() + ".png"
    browser.takeScreenShot(fname)
  }

  def downloadString(urlString: String): (Int, String) = downloadString(None, urlString)
  def downloadBytes(urlString: String): (Int, Array[Byte]) = downloadBytes(None, urlString)

  def download[T](ifModifiedSince: Option[Long], urlString: String)(f: InputStream => T): (Int, T) = {
    val url = new URL(urlString)
    val conn = url.openConnection().asInstanceOf[HttpURLConnection]
    try {
      if (ifModifiedSince.isDefined) conn.setIfModifiedSince(ifModifiedSince.get)
      (conn.getResponseCode, f(conn.getInputStream))
    }
    finally {
      conn.disconnect()
    }
  }

  def downloadString(ifModifiedSince: Option[Long], urlString: String): (Int, String) =
    download(ifModifiedSince, urlString) { is =>
      val br = new BufferedReader(new InputStreamReader(is, "UTF-8"))
      val buf = new StringBuilder()
      readFully(buf, br)
      buf.toString
    }

  def downloadBytes(ifModifiedSince: Option[Long], urlString: String): (Int, Array[Byte]) =
    download(ifModifiedSince, urlString) { is =>
      def reader(buf: ByteArrayOutputStream): ByteArrayOutputStream = {
        val c = is.read
        if (c == -1) buf
        else {
          buf.write(c)
          reader(buf)
        }
      }

      reader(new ByteArrayOutputStream).toByteArray
    }

  def readFully(buf: StringBuilder, br: BufferedReader) {
    val s = br.readLine()
    if (s == null) return
    buf.append(s)
    readFully(buf, br)
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
