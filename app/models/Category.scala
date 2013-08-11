package models

import anorm.{NotAssigned, Pk}
import anorm.SqlParser._

case class Category(id: Pk[Long] = NotAssigned) extends NotNull

case class CategoryPath(ancestor: Long, descendant: Long, length: Int) extends NotNull {
  assert(length >= 0 && length <= Short.MaxValue, "length(= " + length + ") is invalid.")
}

case class CategoryName(locale: LocaleInfo, category_id: Long, name: String) extends NotNull {
  assert(name != null && name.length <= 32, "length(= " + name + ") is invalid.")
}

object Category {
  val simple = {
    get[Pk[Long]]("category.category_id") map {
      case id => Category(id)
    }
  }

//  def createNewCategory(
//    parent: Option[Category], names: Map[LocaleInfo, String]
//  ): (Category, Map[LocaleInfo, CategoryName]) = 
}

object CategoryName {
}
