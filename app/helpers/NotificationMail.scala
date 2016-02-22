package helpers

import models._
import play.api.i18n.{Lang, Messages}
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._
import play.api.Play.current
import play.api.libs.mailer._
import play.api.i18n.Messages
import models.PersistedTransaction
import java.sql.Connection
import play.api.Play
import controllers.HasLogger
import collection.immutable
import play.api.templates.Html

object NotificationMail extends HasLogger {
  val disableMailer = Play.current.configuration.getBoolean("disable.mailer").getOrElse(false)
  val from = Play.current.configuration.getString("order.email.from").get

  def orderCompleted(
    login: LoginSession, tran: PersistedTransaction, addr: Option[Address]
  )(implicit conn: Connection) {
    val metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]] = retrieveMetadata(tran)

    val supplementalEmails = SupplementalUserEmail.load(login.storeUser.id.get).map(_.email)
    sendToBuyer(login, tran, addr, metadata, supplementalEmails)
    sendToStoreOwner(login, tran, addr, metadata)
    sendToAdmin(login, tran, addr, metadata)
  }

  def shipCompleted(
    login: LoginSession, siteId: Long, tran: PersistedTransaction, addr: Address,
    status: TransactionShipStatus, transporters: immutable.LongMap[String]
  )(implicit conn: Connection) {
    val metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]] = retrieveMetadata(tran)

    sendShippingNotificationToBuyer(
      login, siteId, tran, addr, metadata, status, transporters
    )
    sendShippingNotificationToStoreOwner(login, siteId, tran, addr, metadata, status, transporters)
    sendShippingNotificationToAdmin(login, siteId, tran, addr, metadata, status, transporters)
  }

  def shipCanceled(
    login: LoginSession, siteId: Long, tran: PersistedTransaction, addr: Address,
    status: TransactionShipStatus, transporters: immutable.LongMap[String]
  )(implicit conn: Connection) {
    val metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]] = retrieveMetadata(tran)

    sendCancelNotificationToBuyer(
      login, siteId, tran, addr, metadata, status, transporters
    )
    sendCancelNotificationToStoreOwner(login, siteId, tran, addr, metadata, status, transporters)
    sendCancelNotificationToAdmin(login, siteId, tran, addr, metadata, status, transporters)
  }

  def retrieveMetadata(
    tran: PersistedTransaction
  )(
    implicit conn: Connection
  ): Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]] = {
    val buf = scala.collection.mutable.HashMap[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]]()
    tran.itemTable.foreach {
      e =>
        val siteId = e._1
        val items = e._2
        items.foreach {
          it =>
            val tranItem = it._2
            val itemId = tranItem.itemId
            buf.update(siteId -> itemId, SiteItemNumericMetadata.all(siteId, ItemId(tranItem.itemId)))
        }
    }
    val metadata = buf.toMap
    metadata
  }

  def sendToBuyer(
    login: LoginSession, tran: PersistedTransaction, addr: Option[Address],
    metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]],
    supplementalEmails: immutable.Set[String]
  ) {
    val primaryEmail = addr.map(_.email).filter(!_.isEmpty).getOrElse(login.storeUser.email)

    (supplementalEmails + primaryEmail).foreach { email =>
      logger.info("Sending confirmation for buyer sent to " + email)
      val body = views.html.mail.forBuyer(login, tran, addr, metadata).toString
      if (! disableMailer) {
        Akka.system.scheduler.scheduleOnce(0.microsecond) {
          val mail = Email(
            subject = Messages("mail.buyer.subject").format(tran.header.id.get),
            to = Seq(email),
            from = from,
            bodyText = Some(body)
          )
          MailerPlugin.send(mail)
          logger.info("Ordering confirmation for buyer sent to " + email)
        }
      }
    }
  }

  def sendShippingNotificationToBuyer(
    login: LoginSession, siteId: Long, tran: PersistedTransaction, addr: Address,
    metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]],
    status: TransactionShipStatus, transporters: immutable.LongMap[String]
  )(implicit conn: Connection) {
    val buyer = StoreUser(tran.header.userId)
    val primaryEmail = if (addr.email.isEmpty) buyer.email else addr.email
    val supplementalEmails = SupplementalUserEmail.load(tran.header.userId).map(_.email)

    (supplementalEmails + primaryEmail).foreach { email =>
      logger.info("Sending shipping notification for buyer sent to " + email)
      val body = views.html.mail.shippingNotificationForBuyer(
        login, siteId, tran, addr, metadata, buyer, status, transporters
      ).toString

      if (! disableMailer) {
        Akka.system.scheduler.scheduleOnce(0.microsecond) {
          val mail = Email(
            subject = Messages("mail.shipping.buyer.subject").format(tran.header.id.get),
            to = Seq(email),
            from = from,
            bodyText = Some(body)
          )
          MailerPlugin.send(mail)
          logger.info("Shipping notification for buyer sent to " + email)
        }
      }
    }
  }

  def sendCancelNotificationToBuyer(
    login: LoginSession, siteId: Long, tran: PersistedTransaction, addr: Address,
    metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]],
    status: TransactionShipStatus, transporters: immutable.LongMap[String]
  )(implicit conn: Connection) {
    val buyer = StoreUser(tran.header.userId)
    val primaryEmail = if (addr.email.isEmpty) buyer.email else addr.email
    val supplementalEmails = SupplementalUserEmail.load(tran.header.userId).map(_.email)

    (supplementalEmails + primaryEmail).foreach { email =>
      logger.info("Sending cancel notification for buyer sent to " + email)
      val body = views.html.mail.cancelNotificationForBuyer(
        login, siteId, tran, addr, metadata, buyer, status, transporters
      ).toString

      if (! disableMailer) {
        Akka.system.scheduler.scheduleOnce(0.microsecond) {
          val mail = Email(
            subject = Messages("mail.cancel.buyer.subject").format(tran.header.id.get),
            to = Seq(email),
            from = from,
            bodyText = Some(body)
          )
          MailerPlugin.send(mail)
          logger.info("Shipping cancel notification for buyer sent to " + email)
        }
      }
    }
  }

  def sendToStoreOwner(
    login: LoginSession, tran: PersistedTransaction, addr: Option[Address],
    metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]]
  )(implicit conn: Connection) {
    tran.siteTable.foreach { site =>
      OrderNotification.listBySite(site.id.get).foreach { owner =>
        logger.info("Sending ordering confirmation for site owner " + site + " sent to " + owner.email)
        val body = views.html.mail.forSiteOwner(login, site, owner, tran, addr, metadata).toString
        if (! disableMailer) {
          Akka.system.scheduler.scheduleOnce(0.microsecond) {
            val mail = Email(
              subject = Messages("mail.site.owner.subject").format(tran.header.id.get),
              to = Seq(owner.email),
              from = from,
              bodyText = Some(body)
            )
            MailerPlugin.send(mail)
            logger.info("Ordering confirmation for site owner " + site + " sent to " + owner.email)
          }
        }
      }
    }
  }

  def sendShippingNotificationToStoreOwner(
    login: LoginSession, siteId: Long, tran: PersistedTransaction, addr: Address,
    metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]],
    status: TransactionShipStatus, transporters: immutable.LongMap[String]
  )(implicit conn: Connection) {
    val buyer = StoreUser(tran.header.userId)
    tran.siteTable.foreach { site =>
      if (site.id.get == siteId) {
        OrderNotification.listBySite(site.id.get).foreach { owner =>
          logger.info("Sending shipping confirmation for site owner " + site + " sent to " + owner.email)
          val body = views.html.mail.shippingNotificationForSiteOwner(
            login, site, owner, tran, addr, metadata, buyer, status, transporters
          ).toString
          if (! disableMailer) {
            Akka.system.scheduler.scheduleOnce(0.microsecond) {
              val mail = Email(
                subject = Messages("mail.shipping.site.owner.subject").format(tran.header.id.get),
                to = Seq(owner.email),
                from = from,
                bodyText = Some(body)
              )
              MailerPlugin.send(mail)
              logger.info("Shipping confirmation for site owner " + site + " sent to " + owner.email)
            }
          }
        }
      }
    }
  }

  def sendCancelNotificationToStoreOwner(
    login: LoginSession, siteId: Long, tran: PersistedTransaction, addr: Address,
    metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]],
    status: TransactionShipStatus, transporters: immutable.LongMap[String]
  )(implicit conn: Connection) {
    val buyer = StoreUser(tran.header.userId)
    tran.siteTable.foreach { site =>
      if (site.id.get == siteId) {
        OrderNotification.listBySite(site.id.get).foreach { owner =>
          logger.info("Sending cancel confirmation for site owner " + site + " sent to " + owner.email)
          val body = views.html.mail.cancelNotificationForSiteOwner(
            login, site, owner, tran, addr, metadata, buyer, status, transporters
          ).toString
          if (! disableMailer) {
            Akka.system.scheduler.scheduleOnce(0.microsecond) {
              val mail = Email(
                subject = Messages("mail.cancel.site.owner.subject").format(tran.header.id.get),
                to = Seq(owner.email),
                from = from,
                bodyText = Some(body)
              )
              MailerPlugin.send(mail)
              logger.info("Shipping cancel confirmation for site owner " + site + " sent to " + owner.email)
            }
          }
        }
      }
    }
  }

  def sendToAdmin(
    login: LoginSession, tran: PersistedTransaction, addr: Option[Address],
    metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]]
  )(implicit conn: Connection) {
    OrderNotification.listAdmin.foreach { admin =>
      logger.info("Sending ordering confirmation for admin sent to " + admin.email)
      val body = views.html.mail.forAdmin(login, admin, tran, addr, metadata).toString
      if (! disableMailer) {
        Akka.system.scheduler.scheduleOnce(0.microsecond) {
          val mail = Email(
            subject = Messages("mail.admin.subject").format(tran.header.id.get),
            to = Seq(admin.email),
            from = from,
            bodyText = Some(body)
          )
          MailerPlugin.send(mail)
          logger.info("Ordering confirmation for admin sent to " + admin.email)
        }
      }
    }
  }

  def sendShippingNotificationToAdmin(
    login: LoginSession, siteId: Long, tran: PersistedTransaction, addr: Address,
    metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]],
    status: TransactionShipStatus, transporters: immutable.LongMap[String]
  )(implicit conn: Connection) {
    val buyer = StoreUser(tran.header.userId)
    tran.siteTable.foreach { site =>
      if (site.id.get == siteId) {
        OrderNotification.listAdmin.foreach { admin =>
          logger.info("Sending shipping notification for admin sent to " + admin.email)
          val body = views.html.mail.shippingNotificationForAdmin(
            login, site, admin, tran, addr, metadata, buyer, status, transporters
          ).toString
          if (! disableMailer) {
            Akka.system.scheduler.scheduleOnce(0.microsecond) {
              val mail = Email(
                subject = Messages("mail.shipping.admin.subject").format(tran.header.id.get),
                to = Seq(admin.email),
                from = from,
                bodyText = Some(body)
              )
              MailerPlugin.send(mail)
              logger.info("Shipping notification for admin sent to " + admin.email)
            }
          }
        }
      }
    }
  }

  def sendCancelNotificationToAdmin(
    login: LoginSession, siteId: Long, tran: PersistedTransaction, addr: Address,
    metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]],
    status: TransactionShipStatus, transporters: immutable.LongMap[String]
  )(implicit conn: Connection) {
    val buyer = StoreUser(tran.header.userId)
    tran.siteTable.foreach { site =>
      if (site.id.get == siteId) {
        OrderNotification.listAdmin.foreach { admin =>
          logger.info("Sending cancel notification for admin sent to " + admin.email)
          val body = views.html.mail.cancelNotificationForAdmin(
            login, site, admin, tran, addr, metadata, buyer, status, transporters
          ).toString
          if (! disableMailer) {
            Akka.system.scheduler.scheduleOnce(0.microsecond) {
              val mail = Email(
                subject = Messages("mail.cancel.admin.subject").format(tran.header.id.get),
                to = Seq(admin.email),
                from = from,
                bodyText = Some(body)
              )
              MailerPlugin.send(mail)
              logger.info("Shipping cancel notification for admin sent to " + admin.email)
            }
          }
        }
      }
    }
  }

  def sendResetPasswordConfirmation(
    user: StoreUser, rec: ResetPassword
  )(
    implicit lang: Lang
  ) {
    logger.info("Sending reset password confirmation to " + user.email)
    val body = views.html.mail.resetPassword(user, rec).toString
    if (! disableMailer) {
      Akka.system.scheduler.scheduleOnce(0.microsecond) {
        val mail = Email(
          subject = Messages("resetPassword.mail.subject"),
          to = Seq(user.email),
          from = from,
          bodyText = Some(body)
        )
        MailerPlugin.send(mail)
        logger.info("Reset password confirmation sent to " + user.email)
      }
    }
  }    
}
