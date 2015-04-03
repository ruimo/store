package helpers

import controllers.HasLogger
import play.api.Play
import java.sql.Connection
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import com.typesafe.plugin._
import play.api.i18n.Messages
import models.{StoreUser, CreatePrize}

object PrizeMail extends HasLogger {
  val disableMailer = Play.current.configuration.getBoolean("disable.mailer").getOrElse(false)
  val from = Play.current.configuration.getString("prize.email.from").get
  val to = Play.current.configuration.getString("prize.email.to").get

  def send(itemName: String, user: StoreUser, prize: CreatePrize) {
    if (! disableMailer) {
      sendTo(itemName, user, prize, to, views.html.mail.prizeForAdmin(itemName, user, prize).toString)
      sendTo(itemName, user, prize, user.email, views.html.mail.prize(itemName, user, prize).toString)
    }
    else {
      logger.info("Prize mail is not sent since mailer is disabled.")
    }
  }

  def sendTo(itemName: String, user: StoreUser, prize: CreatePrize, sendTo: String, body: String) {
    logger.info("Sending Prize mail to " + sendTo)
    Akka.system.scheduler.scheduleOnce(0.microsecond) {
      val mail = use[MailerPlugin].email
      mail.setSubject(Messages("mail.prize.subject"))
      mail.addRecipient(sendTo)
      mail.addFrom(from)
      mail.send(body)
      logger.info("Prize mail sent to " + sendTo)
    }
  }
}
