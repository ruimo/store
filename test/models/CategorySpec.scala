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
          Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
        )

        val root = Category.root
        root.size === 1
        root.head === cat

        CategoryName.get(LocaleInfo.Ja, cat) === "植木"
        CategoryName.get(LocaleInfo.En, cat) === "Plant"

        CategoryPath.parent(cat) === None
        CategoryPath.children(cat).size === 0

        CategoryPath.childrenNames(cat, LocaleInfo.Ja).size === 0
        CategoryPath.childrenNames(cat, LocaleInfo.En).size === 0
      }
    }

    "Parent child category" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        val parent = Category.createNew(
          Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
        )
        val child = Category.createNew(
          parent,
          Map(LocaleInfo.Ja -> "果樹", LocaleInfo.En -> "Fruit Tree")
        )

        val root = Category.root
        root.size === 1
        root.head === parent

        CategoryName.get(LocaleInfo.Ja, parent) === "植木"
        CategoryName.get(LocaleInfo.En, parent) === "Plant"
        CategoryName.get(LocaleInfo.Ja, child) === "果樹"
        CategoryName.get(LocaleInfo.En, child) === "Fruit Tree"

        CategoryPath.parent(parent) === None
        CategoryPath.children(parent).size === 1
        CategoryPath.parent(child) === Some(parent)
        CategoryPath.children(child).size === 0

        val jaChildNames = CategoryPath.childrenNames(parent, LocaleInfo.Ja)
        jaChildNames.size === 1
        val (jaCat, jaName) = jaChildNames.head
        jaCat === child
        jaName.locale === LocaleInfo.Ja
        jaName.name === "果樹"

        val enChildNames = CategoryPath.childrenNames(parent, LocaleInfo.En)
        enChildNames.size === 1
        val (enCat, enName) = enChildNames.head
        enCat === child
        enName.locale === LocaleInfo.En
        enName.name === "Fruit Tree"

        CategoryPath.childrenNames(child, LocaleInfo.Ja).size === 0
        CategoryPath.childrenNames(child, LocaleInfo.En).size === 0
      }
    }
  }
}
