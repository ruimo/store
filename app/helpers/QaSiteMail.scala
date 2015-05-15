package helpers

import models.{OrderNotification, CreateQaSite, StoreUser, Site}
import controllers.HasLogger
import play.api.Play
import java.sql.Connection
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.mailer._
import play.api.i18n.Messages

object QaSiteMail extends HasLogger {
  val disableMailer = Play.current.configuration.getBoolean("disable.mailer").getOrElse(false)
  val from = Play.current.configuration.getString("user.registration.email.from").get

  def send(qa: CreateQaSite, user: StoreUser, site: Site)(implicit conn: Connection) {
    if (! disableMailer) {
      OrderNotification.listAdmin.foreach { admin =>
        sendTo(qa, site, admin.email, views.html.mail.qaSiteForAdmin(qa, site).toString)
      }
      OrderNotification.listBySite(site.id.get).foreach { owner =>
        sendTo(qa, site, owner.email, views.html.mail.qaSiteForStoreOwner(qa, site).toString)
      }
      sendTo(qa, site, qa.email, views.html.mail.qaSiteForUser(qa, site).toString)
    }
    else {
      logger.info("QA site mail is not sent since mailer is disabled.")
    }
  }

  def sendTo(qa: CreateQaSite, site: Site, email: String, body: String)(implicit conn: Connection) {
    logger.info("Sending QA site mail to " + email)
    Akka.system.scheduler.scheduleOnce(0.microsecond) {
      val mail = Email(
        subject = Messages("mail.qa.site.subject"),
        to = Seq(email),
        from = from,
        bodyText = Some(body)
      )
      MailerPlugin.send(mail)
      logger.info("QA site mail sent to " + email)
    }
  }
}
