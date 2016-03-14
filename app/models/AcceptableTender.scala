package models

import helpers.Cache
import scala.collection.immutable
import scala.collection.JavaConversions._
import com.typesafe.config.ConfigList
import com.typesafe.config.ConfigValue
import play.api.Logger

object AcceptableTender {
  val logger = Logger(getClass)

  val AcceptableTender: () => immutable.Map[UserTypeCode, immutable.Set[TenderType]] = Cache.config { cfg => 
    val tendersByUserTypeCode = cfg.getConfig("acceptableTenders").getOrElse(
      throw new IllegalStateException("Cannot find configuration 'acceptableTenders'")
    )

    immutable.HashMap[UserTypeCode, immutable.Set[TenderType]]() ++
    classOf[UserTypeCode].getEnumConstants().map { utc =>
      val tendersList: java.util.List[String] = tendersByUserTypeCode.getStringList(utc.toString).getOrElse {
        logger.error("Cannot find configuration 'acceptableTenders." + utc + "'")
        java.util.Collections.emptyList[String]()
      }

      utc -> (
        immutable.HashSet[TenderType]() ++ tendersList.map(
          t => TenderType.fromString(t)
        )
      )
    }
  }
}
