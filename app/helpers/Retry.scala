package helpers

import scala.annotation.tailrec

object Retry {
  @tailrec def doRetry[T](
    t: Class[_ <: Throwable],
    waitPeriodMillis: Long = 5,
    retryCount: Int = 3
  )(
    f: => T
  ): T = {
    try {
      f
    }
    catch {
      case error: Throwable =>
        if (t.isAssignableFrom(error.getClass)) {
          if (retryCount == 0) throw error
          else {
            Thread.sleep(waitPeriodMillis)
            doRetry(t, waitPeriodMillis, retryCount - 1)(f)
          }
        }
        else throw error
    }
  }
}
