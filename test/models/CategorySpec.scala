package models

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current
import anorm.Id
import java.util.Locale

class CategorySpec extends Specification {
  "Category" should {
    "Create new category" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        val cat = Category.createNew(
          None, 
          Map(LocaleInfo.ja -> "植木", LocaleInfo.en -> "Plant")
        )

        val root = Category.root
        root.size === 1
        root.head === cat

        CategoryName.get(LocaleInfo.ja, cat) === "植木"
        CategoryName.get(LocaleInfo.en, cat) === "Plant"

        CategoryPath.parent(cat) === None
      }
    }
  }
}
