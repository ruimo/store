package helpers

import models.{OrderNotification, UserRegistration}
import controllers.HasLogger
import play.api.Play
import java.sql.Connection
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import com.typesafe.plugin._
import play.api.i18n.Messages

object UserEntryMail extends HasLogger {
  val disableMailer = Play.current.configuration.getBoolean("disable.mailer").getOrElse(false)
  val from = Play.current.configuration.getString("user.registration.email.from").get

  def sendUserRegistration(ur: UserRegistration)(implicit conn: Connection) {
    if (! disableMailer) {
      OrderNotification.listAdmin.foreach { admin =>
        logger.info("Sending user registration mail to " + admin.email)
        val body = views.html.mail.userRegistration(admin, ur).toString
        Akka.system.scheduler.scheduleOnce(0.microsecond) {
          val mail = use[MailerPlugin].email
          mail.setSubject(Messages("mail.user.registration.subject"))
          mail.addRecipient(admin.email)
          mail.addFrom(from)
          mail.send(body)
          logger.info("User registration mail sent to " + admin.email)
        }
      }
    }
    else {
      logger.info("User registration mail is not sent since mailer is disabled.")
    }
  }
}
