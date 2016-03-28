package models

import scala.reflect.runtime.universe
import scala.collection.mutable
import scala.collection.concurrent.TrieMap
import helpers.Cache
import scala.collection.immutable
import scala.collection.JavaConversions._
import play.api.Configuration
import com.typesafe.config.ConfigList
import com.typesafe.config.ConfigValue
import play.api.Logger

case class ItemPriceStrategyContext(
  loginSession: LoginSession
)

case class ItemPriceStrategyInput(
  itemPriceHistory: ItemPriceHistory
)

object UnitPriceStrategy extends ItemPriceStrategy.ItemPriceStrategy {
  def apply(in: ItemPriceStrategyInput): BigDecimal = in.itemPriceHistory.unitPrice
}

object ListPriceStrategy extends ItemPriceStrategy.ItemPriceStrategy {
  def apply(in: ItemPriceStrategyInput): BigDecimal = in.itemPriceHistory.listPrice.getOrElse(
    in.itemPriceHistory.unitPrice
  )
}

object ItemPriceStrategy {
  type ItemPriceStrategy = ItemPriceStrategyInput => BigDecimal
  val logger = Logger(getClass)
  private var ObjectCache: mutable.Map[String, ItemPriceStrategy] = TrieMap()

  def reflect(clsName: String): ItemPriceStrategy = {
    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
    val module = runtimeMirror.staticModule(clsName)
    val obj = runtimeMirror.reflectModule(module)
    obj.instance.asInstanceOf[ItemPriceStrategy]
  }

  val ItemPriceStrategyConf: () => immutable.Map[UserTypeCode, ItemPriceStrategy] = 
    Cache.config { cfg =>
      val strategyCfg = cfg.getConfig("itemPriceStrategy").getOrElse(
        throw new IllegalStateException("Cannot find configuration 'itemPriceStrategy'")
      )

      immutable.HashMap[UserTypeCode, ItemPriceStrategy]() ++
      classOf[UserTypeCode].getEnumConstants().map { utc =>
        val clsName =
          strategyCfg.getString(utc.toString + ".type").getOrElse("models.UnitPriceStrategy")

        utc -> ObjectCache.getOrElseUpdate(clsName, reflect(clsName))
      }
    }

  def apply(ctx: ItemPriceStrategyContext): ItemPriceStrategy =
    ItemPriceStrategyConf()(ctx.loginSession.role.typeCode)
}
