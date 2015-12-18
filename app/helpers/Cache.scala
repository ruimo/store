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

    class CacheEntryHolder {
      private var entry: CacheEntry = InitCacheEntry

      def cacheEntry(e: CacheEntry): Unit = this.synchronized {
        this.entry = e
      }

      def cacheEntry: CacheEntry = this.synchronized {
        entry
      }
    }

    var current: CacheEntryHolder = new CacheEntryHolder

    () => current.cacheEntry match {
      case InitCacheEntry => {
        val newVal = gen()
        current.cacheEntry(ExpiringCacheEntry(newVal, genTime()))
        newVal
      }

      case ExpiringCacheEntry(currentValue, lastUpdateInMillis) => {
        val now = genTime()
        if (now - lastUpdateInMillis > expirationInMillis) {
          val newValue = gen()
          current.cacheEntry(ExpiringCacheEntry(newValue, now))
          newValue
        }
        else {
          currentValue
        }
      }
    }
  }

  def mayBeCached[T](
    cacheOn: Boolean = true,
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


