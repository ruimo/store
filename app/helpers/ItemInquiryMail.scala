package helpers

import controllers.HasLogger
import play.api.Play
import scala.collection.immutable
import models.{ItemInquiry, ItemInquiryField, Site, OrderNotification, LocaleInfo, ItemName, SiteItem, StoreUser}
import java.sql.Connection
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.concurrent.duration._
import com.typesafe.plugin._
import play.api.i18n.Messages
import play.api.libs.concurrent.Execution.Implicits._

object ItemInquiryMail extends HasLogger {
  val disableMailer = Play.current.configuration.getBoolean("disable.mailer").getOrElse(false)
  val from = Play.current.configuration.getString("user.registration.email.from").get

  def send(
    user: StoreUser, inq: ItemInquiry, fields: immutable.Map[Symbol, String], locale: LocaleInfo
  )(
    implicit conn: Connection
  ) {
    val itemInfo: (Site, ItemName) = SiteItem.getWithSiteAndItem(inq.siteId, inq.itemId, locale).get

    sendToStoreOwner(user, locale, itemInfo, inq, fields)
    sendToAdmin(user, locale, itemInfo, inq, fields)
  }

  def sendToStoreOwner(
    user: StoreUser, locale: LocaleInfo, itemInfo: (Site, ItemName), inq: ItemInquiry, fields: immutable.Map[Symbol, String]
  )(
    implicit conn: Connection
  ) {
    OrderNotification.listBySite(inq.siteId).foreach { owner =>
      logger.info("Sending item inquiry to site owner " + itemInfo._1 + " sent to " + inq.email)
      val body = views.html.mail.itemInquiryForSiteOwner(user, itemInfo, inq, fields).toString
      if (! disableMailer) {
        Akka.system.scheduler.scheduleOnce(0.microsecond) {
          val mail = use[MailerPlugin].email
          mail.setSubject(Messages("mail.item.inquiry.site.owner.subject").format(inq.id.get))
          mail.addRecipient(owner.email)
          mail.addFrom(from)
          mail.send(body)
          logger.info("Item inquiry notification for site owner " + itemInfo._1 + " sent to " + inq.email)
        }
      }
    }
  }

  def sendToAdmin(
    user: StoreUser, locale: LocaleInfo, itemInfo: (Site, ItemName), inq: ItemInquiry, fields: immutable.Map[Symbol, String]
  )(
    implicit conn: Connection
  ) {
    if (! disableMailer) {
      OrderNotification.listAdmin.foreach { admin =>
        logger.info("Sending item inquiry for admin to " + admin.email)
        val body = views.html.mail.itemInquiryForAdmin(user, itemInfo, inq, fields).toString
        Akka.system.scheduler.scheduleOnce(0.microsecond) {
          val mail = use[MailerPlugin].email
          mail.setSubject(Messages("mail.item.inquiry.site.owner.subject").format(inq.id.get))
          mail.addRecipient(admin.email)
          mail.addFrom(from)
          mail.send(body)
          logger.info("Item inquiry notification for admin to " + admin.email)
        }
      }
    }
    else {
      logger.info("Item inquiry notification mail is not sent since mailer is disabled.")
    }
  }
}
