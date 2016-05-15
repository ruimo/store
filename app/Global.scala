import controllers.I18n.I18nFilter
import play.api.mvc._
import play.api._
import play.filters.csrf._
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import helpers.Cache
import scala.concurrent.duration._
import play.api.db.DB
import play.api.Logger
import java.sql.Connection
import models.StoreUser

object Global extends WithFilters(CSRFFilter(), I18nFilter) with GlobalSettings {
  val logger = Logger(getClass)
  
  override def onStart(app: Application) {
    val durationToPreserveAnonymousUser: FiniteDuration = app.configuration.getMilliseconds(
      "durationToPreserveAnonymousUser"
    ).map(l => FiniteDuration.apply(l, MILLISECONDS)).getOrElse(
      throw new IllegalStateException("durationToPreserveAnonymousUser should be defined in application.conf.")
    )

    val intervalToCheckAnonymousUserRemoval: FiniteDuration = app.configuration.getMilliseconds(
      "intervalToCheckAnonymousUserRemoval"
    ).map(l => FiniteDuration.apply(l, MILLISECONDS)).getOrElse(
      throw new IllegalStateException("intervalToCheckAnonymousUserRemoval should be defined in application.conf.")
    )

    Akka.system.scheduler.schedule(0.second, intervalToCheckAnonymousUserRemoval) {
      try {
        DB.withConnection { implicit conn =>
          StoreUser.removeObsoleteAnonymousUser(
            durationToPreserveAnonymousUser
          )
        }
      }
      catch {
        case t: Throwable =>
          logger.error("Cannot remove obsolete anonymous user.", t)
      }
    }
  }
}


