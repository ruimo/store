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
          Map(LocaleInfo.ja -> "植木", LocaleInfo.en -> "Plant")
        )

        val root = Category.root
        root.size === 1
        root.head === cat

        CategoryName.get(LocaleInfo.ja, cat) === "植木"
        CategoryName.get(LocaleInfo.en, cat) === "Plant"

        CategoryPath.parent(cat) === None
        CategoryPath.children(cat).size === 0

        CategoryPath.childrenNames(cat, LocaleInfo.ja).size === 0
        CategoryPath.childrenNames(cat, LocaleInfo.en).size === 0
      }
    }

    "Parent child category" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        val parent = Category.createNew(
          Map(LocaleInfo.ja -> "植木", LocaleInfo.en -> "Plant")
        )
        val child = Category.createNew(
          parent,
          Map(LocaleInfo.ja -> "果樹", LocaleInfo.en -> "Fruit Tree")
        )

        val root = Category.root
        root.size === 1
        root.head === parent

        CategoryName.get(LocaleInfo.ja, parent) === "植木"
        CategoryName.get(LocaleInfo.en, parent) === "Plant"
        CategoryName.get(LocaleInfo.ja, child) === "果樹"
        CategoryName.get(LocaleInfo.en, child) === "Fruit Tree"

        CategoryPath.parent(parent) === None
        CategoryPath.children(parent).size === 1
        CategoryPath.parent(child) === Some(parent)
        CategoryPath.children(child).size === 0

        val jaChildNames = CategoryPath.childrenNames(parent, LocaleInfo.ja)
        jaChildNames.size === 1
        val (jaCat, jaName) = jaChildNames.head
        jaCat === child
        jaName.locale === LocaleInfo.ja
        jaName.name === "果樹"

        val enChildNames = CategoryPath.childrenNames(parent, LocaleInfo.en)
        enChildNames.size === 1
        val (enCat, enName) = enChildNames.head
        enCat === child
        enName.locale === LocaleInfo.en
        enName.name === "Fruit Tree"

        CategoryPath.childrenNames(child, LocaleInfo.ja).size === 0
        CategoryPath.childrenNames(child, LocaleInfo.en).size === 0
      }
    }
  }
}
