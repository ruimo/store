package helpers

import play.api.Configuration
import play.api.Application
import play.api.Play
import play.api.Mode

object Cache {
  def mayBeCached[T](cacheOn: Boolean, gen: => T): () => T = {
    if (cacheOn) {
      lazy val value = gen
      () => value
    }
    else () => gen
  }

  def cacheOnProd[T](gen: => T): () => T = mayBeCached(Play.maybeApplication.get.mode == Mode.Test, gen)

  def App: Application = cacheOnProd(Play.maybeApplication.get)()
  def Conf: Configuration = cacheOnProd(App.configuration)()
}


