package helpers

import models.{QaEntry, OrderNotification}
import controllers.HasLogger
import play.api.Play
import java.sql.Connection
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.mailer._
import play.api.i18n.Messages

object QaMail extends HasLogger {
  val disableMailer = Play.current.configuration.getBoolean("disable.mailer").getOrElse(false)
  val from = Play.current.configuration.getString("user.registration.email.from").get

  def send(qa: QaEntry)(implicit conn: Connection) {
    if (! disableMailer) {
      OrderNotification.listAdmin.foreach { admin =>
        logger.info("Sending QA mail to " + admin.email)
        val body = views.html.mail.qa(admin, qa).toString
        Akka.system.scheduler.scheduleOnce(0.microsecond) {
          val mail = Email(
            subject = Messages("mail.qa.subject"),
            to = Seq(admin.email),
            from = from,
            bodyText = Some(body)
          )
          MailerPlugin.send(mail)
          logger.info("QA mail sent to " + admin.email)
        }
      }
    }
    else {
      logger.info("QA mail is not sent since mailer is disabled.")
    }
  }
}
