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

object NotificationMail extends HasLogger {
  val from = Play.current.configuration.getString("order.email.from").get

  def orderCompleted(
    login: LoginSession, tran: PersistedTransaction, addr: Address
  )(implicit conn: Connection) {
    val buf = scala.collection.mutable.HashMap[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]]()
    tran.itemTable.foreach { e =>
      val siteId = e._1
      val items = e._2
      items.foreach { it =>
        val tranItem = it._2
        val itemId = tranItem.itemId
        buf.update(siteId -> itemId, SiteItemNumericMetadata.all(siteId, tranItem.itemId))
      }
    }
    val metadata = buf.toMap

    sendToBuyer(login, tran, addr, metadata)
    sendToStoreOwner(login, tran, addr, metadata)
    sendToAdmin(login, tran, addr, metadata)
  }

  def sendToBuyer(
    login: LoginSession, tran: PersistedTransaction, addr: Address,
    metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]]
  ) {
    Akka.system.scheduler.scheduleOnce(0.microsecond) {
      val mail = use[MailerPlugin].email
      mail.setSubject(Messages("mail.buyer.subject").format(tran.header.id.get))
      mail.addRecipient(login.storeUser.email)
      mail.addFrom(from)
      mail.send(views.html.mail.forBuyer(login, tran, addr, metadata).toString)
      logger.info("Ordering confirmation sent to " + login.storeUser.email)
    }
  }

  def sendToStoreOwner(
    login: LoginSession, tran: PersistedTransaction, addr: Address,
    metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]]
  )(implicit conn: Connection) {
    tran.siteTable.foreach { site =>
      OrderNotification.listBySite(site.id.get).foreach { owner =>
        Akka.system.scheduler.scheduleOnce(0.microsecond) {
          val mail = use[MailerPlugin].email
          mail.setSubject(Messages("mail.site.owner.subject").format(tran.header.id.get))
          mail.addRecipient(owner.email)
          mail.addFrom(from)
          mail.send(views.html.mail.forSiteOwner(login, site, owner, tran, addr, metadata).toString)
          logger.info("Ordering confirmation for site owner sent to " + owner.email)
        }
      }
    }
  }

  def sendToAdmin(
    login: LoginSession, tran: PersistedTransaction, addr: Address,
    metadata: Map[(Long, Long), Map[SiteItemNumericMetadataType, SiteItemNumericMetadata]]
  )(implicit conn: Connection) {
    OrderNotification.listAdmin.foreach { admin =>
      Akka.system.scheduler.scheduleOnce(0.microsecond) {
        val mail = use[MailerPlugin].email
        mail.setSubject(Messages("mail.admin.subject").format(tran.header.id.get))
        mail.addRecipient(admin.email)
        mail.addFrom(from)
        mail.send(views.html.mail.forAdmin(login, admin, tran, addr, metadata).toString)
        logger.info("Ordering confirmation for admin sent to " + admin.email)
      }
    }
  }
}
