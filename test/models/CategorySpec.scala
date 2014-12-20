package models

import org.specs2.mutable._

import anorm._
import anorm.{NotAssigned, Pk}
import anorm.SqlParser
import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current
import anorm.Id
import java.util.Locale

class CategorySpec extends Specification {
  "Category" should {
    "Can create new category." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()
        DB.withConnection { implicit conn => {
          val cat = Category.createNew(
            Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
          )

          val root = Category.root
          root.size === 1
          root.head === cat

          CategoryName.get(LocaleInfo.Ja, cat) === Some("植木")
          CategoryName.get(LocaleInfo.En, cat) === Some("Plant")

          CategoryPath.parent(cat) === None
          CategoryPath.children(cat).size === 0

          CategoryPath.childrenNames(cat, LocaleInfo.Ja).size === 0
          CategoryPath.childrenNames(cat, LocaleInfo.En).size === 0
        }}
      }
    }

    "Can select single category." in {
    running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()
        DB.withConnection { implicit conn => {
          val cat = Category.createNew(
            Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
          )
          Category.get(cat.id.get) === Some(cat)
          Category.get(cat.id.get+1000) === None
        }}
      }
    }


    "List category" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => {
          val cat = Category.createNew(
            Map(LocaleInfo.Ja -> "うえき", LocaleInfo.En -> "Plant")
          )

          val cat2 = Category.createNew(
            Map(LocaleInfo.Ja -> "はな", LocaleInfo.En -> "Flower")
          )

          val cat3 = Category.createNew(
            Map(LocaleInfo.Ja -> "きゅうこん", LocaleInfo.En -> "Bulb")
          )

          val page1 = Category.list(0, 10, LocaleInfo.Ja)
          page1.page === 0
          page1.offset === 0
          page1.total === 3
          page1.list(0)._2.name === "うえき"
          page1.list(1)._2.name === "きゅうこん"
          page1.list(2)._2.name === "はな"
          page1.prev === None
          page1.next === None

          val page2 = Category.list(0, 2, LocaleInfo.Ja)
          page2.page === 0
          page2.offset === 0
          page2.total === 3
          page2.list(0)._2.name === "うえき"
          page2.list(1)._2.name === "きゅうこん"
          page2.prev === None
          page2.next === Some(1)

          val page3 = Category.list(1, 2, LocaleInfo.Ja)
          page3.page === 1
          page3.offset === 2
          page3.total === 3
          page3.list(0)._2.name === "はな"
          page3.prev === Some(0)
          page3.next === None
        }}
      }
    }

    "Parent child category" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => {
          val parent = Category.createNew(
            Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
          )
          val child = Category.createNew(
            parent,
            Map(LocaleInfo.Ja -> "果樹", LocaleInfo.En -> "Fruit Tree")
          )
          val child2 = Category.createNew(
            parent,
            Map(LocaleInfo.En -> "English Only Tree")
          )
          val root = Category.root
          root.size === 1
          root.head === parent

          CategoryName.get(LocaleInfo.Ja, parent) === Some("植木")
          CategoryName.get(LocaleInfo.En, parent) === Some("Plant")
          CategoryName.get(LocaleInfo.Ja, child) === Some("果樹")
          CategoryName.get(LocaleInfo.En, child) === Some("Fruit Tree")
          CategoryName.get(LocaleInfo.Ja, child2) === None
          CategoryName.get(LocaleInfo.En, child2) === Some("English Only Tree")
          

          CategoryPath.parent(parent) === None
          CategoryPath.children(parent).size === 2
          CategoryPath.parent(child) === Some(parent)
          CategoryPath.children(child).size === 0

          val jaChildNames = CategoryPath.childrenNames(parent, LocaleInfo.Ja)
          jaChildNames.size === 1
          val (jaCat, jaName) = jaChildNames.head
          jaCat === child
          jaName.locale === LocaleInfo.Ja
          jaName.name === "果樹"

          val enChildNames = CategoryPath.childrenNames(parent, LocaleInfo.En)
          enChildNames.size === 2
          val (enCat, enName) = enChildNames.head
          enCat === child
          enName.locale === LocaleInfo.En
          enName.name === "Fruit Tree"

          CategoryPath.childrenNames(child, LocaleInfo.Ja).size === 0
          CategoryPath.childrenNames(child, LocaleInfo.En).size === 0

          var pathList = CategoryPath.listNamesWithParent(LocaleInfo.Ja)
          pathList.contains((parent,CategoryName(LocaleInfo.Ja,parent.id.get,"植木"))) === true
          pathList.contains((parent,CategoryName(LocaleInfo.Ja,child.id.get,"果樹"))) === true
          pathList.contains((child,CategoryName(LocaleInfo.Ja,child.id.get,"果樹"))) === true
          pathList.contains((child2,CategoryName(LocaleInfo.En,child2.id.get,"English Only Tree"))) === true



        }
      }}
    }

    "be able to rename category name." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()
        DB.withConnection { implicit conn => {
          val cat = Category.createNew(
            Map(LocaleInfo.Ja -> "植木", LocaleInfo.En -> "Plant")
          )
          
          Category.rename(cat, Map(LocaleInfo.Ja -> "うえき"))

          CategoryName.get(LocaleInfo.Ja, cat) === Some("うえき")
          CategoryName.get(LocaleInfo.En, cat) === Some("Plant")
        }}
      }
    }

    "be able to move category between different parent nodes" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => 
          val parent = Category.createNew(Map(LocaleInfo.Ja -> "生物") )
          val child1 = Category.createNew(parent, Map(LocaleInfo.Ja -> "植物"))
          val child2 = Category.createNew(parent, Map(LocaleInfo.Ja -> "動物"))

          val child11 = Category.createNew(child1, Map(LocaleInfo.Ja -> "歩く木"))

          Category.move(child11, Some(child2))

          CategoryPath.parent(child11).get === child2

          CategoryPath.children(child1).size === 0

          CategoryPath.children(child2).size === 1

        }
      }
    }
    
    "be able to move category under some parent node to root" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => 
          val parent = Category.createNew(Map(LocaleInfo.Ja -> "生物") )
          val child1 = Category.createNew(parent, Map(LocaleInfo.Ja -> "植物"))
          val child2 = Category.createNew(parent, Map(LocaleInfo.Ja -> "動物"))

          val child11 = Category.createNew(child1, Map(LocaleInfo.Ja -> "歩く木"))

          Category.move(child11, None)

          CategoryPath.parent(parent) === None

          CategoryPath.parent(child11) === None

          CategoryPath.children(child1).size === 0

          CategoryPath.children(child2).size === 0

        }
      }
    }


    "be able to move root category to under some parent" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => 
          val parent = Category.createNew(Map(LocaleInfo.Ja -> "生物") )
          val child1 = Category.createNew(parent, Map(LocaleInfo.Ja -> "植物"))
          val child2 = Category.createNew(parent, Map(LocaleInfo.Ja -> "動物"))

          val child11 = Category.createNew(child1, Map(LocaleInfo.Ja -> "歩く木"))

          Category.move(child11, Some(child2))

          CategoryPath.parent(child11).get === child2

          CategoryPath.children(child1).size === 0

          CategoryPath.children(child2).size === 1

        }
      }
    }
    
    "be able to move category under some parent node to root" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => 
          val parent = Category.createNew(Map(LocaleInfo.Ja -> "生物") )
          val child1 = Category.createNew(parent, Map(LocaleInfo.Ja -> "植物"))
          val child2 = Category.createNew(parent, Map(LocaleInfo.Ja -> "動物"))

          val child11 = Category.createNew(child1, Map(LocaleInfo.Ja -> "歩く木"))

          val parent2 = Category.createNew(Map(LocaleInfo.Ja -> "石油"))

          Category.move(parent2, Some(parent))

          CategoryPath.parent(parent2) === Some(parent)

          CategoryPath.children(parent2).size === 0

          CategoryPath.children(parent).size === 3

        }
      }
    }

    "reject when category and parent is same" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn =>
          val parent = Category.createNew(Map(LocaleInfo.Ja -> "生物") )
          val child1 = Category.createNew(parent, Map(LocaleInfo.Ja -> "植物"))
          val child2 = Category.createNew(parent, Map(LocaleInfo.Ja -> "動物"))

          val child11 = Category.createNew(child1, Map(LocaleInfo.Ja -> "歩く木"))

          Category.move(child1, Some(child1)) must throwA[Exception]
        
          CategoryPath.parent(child1) === Some(parent)

          CategoryPath.children(parent).size === 2

        }
      }
    }

    "reject when parent is one of descendant of category" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn =>
          val parent = Category.createNew(Map(LocaleInfo.Ja -> "生物") )
          val child1 = Category.createNew(parent, Map(LocaleInfo.Ja -> "植物"))
          val child2 = Category.createNew(parent, Map(LocaleInfo.Ja -> "動物"))

          val child11 = Category.createNew(child1, Map(LocaleInfo.Ja -> "歩く木"))

          Category.move(parent, Some(child1)) must throwA[Exception]

          CategoryPath.parent(child1) === Some(parent)

          CategoryPath.children(parent).size === 2

        }
      }
    }
  }
}
