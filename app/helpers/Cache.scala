package helpers

import play.api.Configuration
import play.api.Application
import play.api.Play
import play.api.Mode
import java.time.Duration

object Cache {
  def expiringCache[T](expirationInMillis: Long, gen: () => T, genTime: () => Long): () => T = {
    trait CacheEntry

    case object InitCacheEntry extends CacheEntry

    case class ExpiringCacheEntry(
      currentValue: T,
      lastUpdateInMillis: Long
    ) extends CacheEntry

    var current: CacheEntry = InitCacheEntry

    () => current match {
      case InitCacheEntry => {
        val newVal = gen()
        current = ExpiringCacheEntry(newVal, genTime())
        newVal
      }

      case ExpiringCacheEntry(currentValue, lastUpdateInMillis) => {
        val now = genTime()
        if (now - lastUpdateInMillis > expirationInMillis) {
          val newValue = gen()
          current = ExpiringCacheEntry(newValue, now)
          newValue
        }
        else {
          currentValue
        }
      }
    }
  }

  def mayBeCached[T](
    cacheOn: Boolean,
    gen: () => T,
    expirationInMillis: Option[Long] = None,
    currentTimeInMillis: () => Long = System.currentTimeMillis
  ): () => T = {
    if (cacheOn) {
      expirationInMillis match {
        case None => {
          lazy val value = gen()
          () => value
        }
        case Some(dur) =>
          expiringCache(dur, gen, currentTimeInMillis)
      }
    }
    else () => gen()
  }

  def cacheOnProd[T](gen: () => T): () => T = mayBeCached(Play.maybeApplication.get.mode == Mode.Prod, gen)

  def App: Application = cacheOnProd(() => Play.maybeApplication.get)()
  def Conf: Configuration = cacheOnProd(() => App.configuration)()
  def config[T](f: Configuration => T): () => T = cacheOnProd(() => f(Conf))
}


