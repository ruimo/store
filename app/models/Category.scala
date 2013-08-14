package models

import anorm._
import anorm.{NotAssigned, Pk}
import anorm.SqlParser._
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps

case class Foo(i: Int) {
}

case class CategoryPath(ancestor: Long, descendant: Long, pathLength: Int) extends NotNull {
  assert(pathLength >= 0 && pathLength <= Short.MaxValue, "length(= " + pathLength + ") is invalid.")
}

case class CategoryName(locale: LocaleInfo, categoryId: Long, name: String) extends NotNull {
  assert(name != null && name.length <= 32, "length(= " + name + ") is invalid.")
}

case class Category(id: Pk[Long] = NotAssigned) extends NotNull

object Category {
  val simple = {
    get[Pk[Long]]("category.category_id") map {
      case id => Category(id)
    }
  }

  def createNew(
    parent: Option[Category], names: Map[LocaleInfo, String]
  ): Category = DB.withTransaction { implicit conn => {
    SQL(
      """
      insert into category values (
        (select next value for category_seq)
      )
      """
    ).executeUpdate()

    val categoryId = SQL("select currval('category_seq')").as(scalar[Long].single)

    names.foreach { e =>
      SQL(
        """
        insert into category_name
          (locale_id, category_name, category_id)
          values
          ({locale_id}, {category_name}, {category_id})
        """
      ).on(
        'locale_id -> e._1.id.get,
        'category_name -> e._2,
        'category_id -> categoryId
      ).executeUpdate()
    }

    /*
      cat1(id1) -+- cat2(id2)
                 +- cat3(id3)
    
      |----------+------------+-------------|
      | ancestor | descendant | path_length |
      |----------+------------+-------------|
      | id1      | id1        |           0 |
      | id1      | id2        |           1 |
      | id1      | id3        |           1 |
      | id2      | id2        |           0 |
      | id3      | id3        |           0 |
      |----------+------------+-------------|

      Add cat(id4) under cat3(id3)

      cat1(id1) -+- cat2(id2)
                 +- cat3(id3) - cat4(id4)
                 
      The following records should be created.
      |----------+------------+-------------|
      | ancestor | descendant | path_length |
      |----------+------------+-------------|
      | id4      | id4        |           0 |
      | id3      | id4        |           1 |
      | id1      | id4        |           2 |
      |----------+------------+-------------|

      */

    SQL(
      """
      insert into category_path
        (ancestor, descendant, path_length)
        values
        ({category_id}, {category_id}, 0)
      """
    ).on(
      'category_id -> categoryId
    ).executeUpdate()

    parent.map {cat => {
      SQL(
        """
        insert into category_path
          (ancestor, descendant, path_length)
          select ancestor, {category_id}, path_length + 1
          from category_path
          where descendant = {parent}
        """
      ).on(
        'descendant -> cat.id.get,
        'category_id -> categoryId
      ).executeUpdate()
    }}

    Category(Id(categoryId))
  }}
}

object CategoryName {
  val simple = {
    get[Long]("category_name.locale_id") ~
    get[Long]("category_name.category_id") ~
    get[String]("category_name.category_name") map {
      case localeId~categoryId~categoryName =>
        CategoryName(LocaleInfo(localeId), categoryId, categoryName)
    }
  }
}

object CategoryPath {
  val simple = {
    get[Long]("category_path.ancestor") ~
    get[Long]("category_path.descendant") ~
    get[Int]("category_path.path_length") map {
      case ancestor~descendant~pathLength =>
        CategoryPath(ancestor, descendant, pathLength)
    }
  }
}
