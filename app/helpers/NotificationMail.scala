package helpers

import models._
import play.api.libs.concurrent.Akka
import com.typesafe.plugin.MailerPlugin
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._
import play.api.Play.current
import com.typesafe.plugin._
import play.api.i18n.Messages
import models.PersistedTransaction
import java.sql.Connection
import play.api.Play
import controllers.HasLogger
import collection.immutable.LongMap

object NotificationMail extends HasLogger {
  val disableMailer = Play.current.configuration.getBoolean("disable.mailer").getOrElse(false)
  val from = Play.current.configuration.getString("order.email.from").get

  def orderCompleted(
    login: LoginSession, tran: PersistedTransaction, addr: Address
  )(implicit conn: Connection) {
    val metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]] = retrieveMetadata(tran)

    sendToBuyer(login, tran, addr, metadata)
    sendToStoreOwner(login, tran, addr, metadata)
    sendToAdmin(login, tran, addr, metadata)
  }

  def shipCompleted(
    login: LoginSession, siteId: Long, tran: PersistedTransaction, addr: Address,
    status: TransactionShipStatus, transporters: LongMap[String]
  )(implicit conn: Connection) {
    val metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]] = retrieveMetadata(tran)
    sendShippingNotificationToBuyer(login, tran, addr, metadata, status, transporters)
    sendShippingNotificationToStoreOwner(login, siteId, tran, addr, metadata, status, transporters)
    sendShippingNotificationToAdmin(login, siteId, tran, addr, metadata, status, transporters)
  }

  def shipCanceled(
    login: LoginSession, siteId: Long, tran: PersistedTransaction, addr: Address,
    status: TransactionShipStatus, transporters: LongMap[String]
  )(implicit conn: Connection) {
    val metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]] = retrieveMetadata(tran)
    sendCancelNotificationToBuyer(login, tran, addr, metadata, status, transporters)
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
            buf.update(siteId -> itemId, SiteItemNumericMetadata.all(siteId, tranItem.itemId))
        }
    }
    val metadata = buf.toMap
    metadata
  }

  def sendToBuyer(
    login: LoginSession, tran: PersistedTransaction, addr: Address,
    metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]]
  ) {
    logger.info("Sending confirmation for buyer sent to " + login.storeUser.email)
    val body = views.html.mail.forBuyer(login, tran, addr, metadata).toString
    if (! disableMailer) {
      Akka.system.scheduler.scheduleOnce(0.microsecond) {
        val mail = use[MailerPlugin].email
        mail.setSubject(Messages("mail.buyer.subject").format(tran.header.id.get))
        mail.addRecipient(login.storeUser.email)
        mail.addFrom(from)
        mail.send(body)
        logger.info("Ordering confirmation for buyer sent to " + login.storeUser.email)
      }
    }
  }

  def sendShippingNotificationToBuyer(
    login: LoginSession, tran: PersistedTransaction, addr: Address,
    metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]],
    status: TransactionShipStatus, transporters: LongMap[String]
  )(implicit conn: Connection) {
    val buyer = StoreUser(tran.header.userId)

    logger.info("Sending shipping notification for buyer sent to " + buyer.email)
    val body = views.html.mail.shippingNotificationForBuyer(
      login, tran, addr, metadata, buyer, status, transporters
    ).toString

    if (! disableMailer) {
      Akka.system.scheduler.scheduleOnce(0.microsecond) {
        val mail = use[MailerPlugin].email
        mail.setSubject(Messages("mail.shipping.buyer.subject").format(tran.header.id.get))
        mail.addRecipient(buyer.email)
        mail.addFrom(from)
        mail.send(body)
        logger.info("Shipping notification for buyer sent to " + buyer.email)
      }
    }
  }

  def sendCancelNotificationToBuyer(
    login: LoginSession, tran: PersistedTransaction, addr: Address,
    metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]],
    status: TransactionShipStatus, transporters: LongMap[String]
  )(implicit conn: Connection) {
    val buyer = StoreUser(tran.header.userId)
    logger.info("Sending cancel notification for buyer sent to " + buyer.email)
    val body = views.html.mail.cancelNotificationForBuyer(
      login, tran, addr, metadata, buyer, status, transporters
    ).toString

    if (! disableMailer) {
      Akka.system.scheduler.scheduleOnce(0.microsecond) {
        val mail = use[MailerPlugin].email
        mail.setSubject(Messages("mail.cancel.buyer.subject").format(tran.header.id.get))
        mail.addRecipient(buyer.email)
        mail.addFrom(from)
        mail.send(body)
        logger.info("Shipping cancel notification for buyer sent to " + buyer.email)
      }
    }
  }

  def sendToStoreOwner(
    login: LoginSession, tran: PersistedTransaction, addr: Address,
    metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]]
  )(implicit conn: Connection) {
    tran.siteTable.foreach { site =>
      OrderNotification.listBySite(site.id.get).foreach { owner =>
        logger.info("Sending ordering confirmation for site owner " + site + " sent to " + owner.email)
        val body = views.html.mail.forSiteOwner(login, site, owner, tran, addr, metadata).toString
        if (! disableMailer) {
          Akka.system.scheduler.scheduleOnce(0.microsecond) {
            val mail = use[MailerPlugin].email
            mail.setSubject(Messages("mail.site.owner.subject").format(tran.header.id.get))
            mail.addRecipient(owner.email)
            mail.addFrom(from)
            mail.send(body)
            logger.info("Ordering confirmation for site owner " + site + " sent to " + owner.email)
          }
        }
      }
    }
  }

  def sendShippingNotificationToStoreOwner(
    login: LoginSession, siteId: Long, tran: PersistedTransaction, addr: Address,
    metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]],
    status: TransactionShipStatus, transporters: LongMap[String]
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
              val mail = use[MailerPlugin].email
              mail.setSubject(Messages("mail.shipping.site.owner.subject").format(tran.header.id.get))
              mail.addRecipient(owner.email)
              mail.addFrom(from)
              mail.send(body)
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
    status: TransactionShipStatus, transporters: LongMap[String]
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
              val mail = use[MailerPlugin].email
              mail.setSubject(Messages("mail.cancel.site.owner.subject").format(tran.header.id.get))
              mail.addRecipient(owner.email)
              mail.addFrom(from)
              mail.send(body)
              logger.info("Shipping cancel confirmation for site owner " + site + " sent to " + owner.email)
            }
          }
        }
      }
    }
  }

  def sendToAdmin(
    login: LoginSession, tran: PersistedTransaction, addr: Address,
    metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]]
  )(implicit conn: Connection) {
    OrderNotification.listAdmin.foreach { admin =>
      logger.info("Sending ordering confirmation for admin sent to " + admin.email)
      val body = views.html.mail.forAdmin(login, admin, tran, addr, metadata).toString
      if (! disableMailer) {
        Akka.system.scheduler.scheduleOnce(0.microsecond) {
          val mail = use[MailerPlugin].email
          mail.setSubject(Messages("mail.admin.subject").format(tran.header.id.get))
          mail.addRecipient(admin.email)
          mail.addFrom(from)
          mail.send(body)
          logger.info("Ordering confirmation for admin sent to " + admin.email)
        }
      }
    }
  }

  def sendShippingNotificationToAdmin(
    login: LoginSession, siteId: Long, tran: PersistedTransaction, addr: Address,
    metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]],
    status: TransactionShipStatus, transporters: LongMap[String]
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
              val mail = use[MailerPlugin].email
              mail.setSubject(Messages("mail.shipping.admin.subject").format(tran.header.id.get))
              mail.addRecipient(admin.email)
              mail.addFrom(from)
              mail.send(body)
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
    status: TransactionShipStatus, transporters: LongMap[String]
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
              val mail = use[MailerPlugin].email
              mail.setSubject(Messages("mail.cancel.admin.subject").format(tran.header.id.get))
              mail.addRecipient(admin.email)
              mail.addFrom(from)
              mail.send(body)
              logger.info("Shipping cancel notification for admin sent to " + admin.email)
            }
          }
        }
      }
    }
  }
}
