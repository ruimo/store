package models

import anorm._
import anorm.{NotAssigned, Pk}
import anorm.SqlParser
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps

case class Item(id: Pk[Long] = NotAssigned, categoryId: Long) extends NotNull

case class ItemName(localeId: Long, itemId: Long, name: String) extends NotNull

case class ItemDescription(localeId: Long, itemId: Long, description: String) extends NotNull

case class ItemPrice(id: Pk[Long] = NotAssigned, siteId: Long, itemId: Long) extends NotNull
