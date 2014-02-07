package controllers

import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.mvc.Controller
import controllers.I18n.I18nAware
import play.api.data.Form
import models.{ LocaleInfo, CreateCategory, Category, CategoryPath, CategoryName }
import play.api.i18n.Messages
import play.api.db.DB
import play.api.Play.current

import play.api.libs.json._

object CategoryMaintenance extends Controller with I18nAware with NeedLogin with HasLogger {
  val createCategoryForm = Form(
    mapping(
      "langId" -> longNumber,
      "categoryName" -> text.verifying(nonEmpty, maxLength(32)),
      "parent" -> optional(longNumber))(CreateCategory.apply)(CreateCategory.unapply))

  def index = isAuthenticated { implicit login =>
    forSuperUser { implicit request =>
      Ok(views.html.admin.categoryMaintenance())
    }
  }

  def startCreateNewCategory = isAuthenticated { implicit login =>
    forSuperUser { implicit request =>
      {
        Ok(views.html.admin.createNewCategory(createCategoryForm, LocaleInfo.localeTable))
      }
    }
  }

  def createNewCategory = isAuthenticated { implicit login =>
    forSuperUser { implicit request =>
      createCategoryForm.bindFromRequest.fold(
        formWithErrors => {
          logger.error("Validation error in CategoryMaintenance.createNewCategory.")
          BadRequest(views.html.admin.createNewCategory(formWithErrors, LocaleInfo.localeTable))
        },
        newCategory => DB.withConnection { implicit conn =>
          newCategory.save
          Redirect(
            routes.CategoryMaintenance.startCreateNewCategory).flashing("message" -> Messages("categoryIsCreated"))
        })
    }
  }

  def editCategory(start: Int = 0, size: Int = 10) = isAuthenticated { implicit login =>
    forSuperUser { implicit request =>
      DB.withConnection { implicit conn =>
        {
          val p = Category.list(page = start, pageSize = size, locale = LocaleInfo.byLang(lang))
          Ok(views.html.admin.editCategory(p))
        }
      }
    }
  }

  /* Seq[CategoryNode] to be folded into recursive JSON structure and vice versa:
        [ {"key":      category_id,
           "title":    category_name,
           "isFolder": true,
           "children": [ 
             ... 
           ] },
             ... ] 
  */
  case class CategoryNode(key: Long, title: String, isFolder: Boolean = true, children: Seq[CategoryNode])

  implicit object CategoryNodeFormat extends Format[CategoryNode] {
    def reads(json: JsValue): JsResult[CategoryNode] = JsSuccess(CategoryNode(
      (json \ "key").as[Long],
      (json \ "title").as[String],
      (json \ "isFolder").as[Boolean],
      (json \ "chidlren").as[Seq[CategoryNode]]))

    def writes(ct: CategoryNode): JsValue = JsObject(List(
      "key" -> JsNumber(ct.key),
      "title" -> JsString(ct.title),
      "isFolder" -> JsBoolean(ct.isFolder),
      "children" -> JsArray(ct.children.map(CategoryNodeFormat.writes))))
  }

  def categoryPathTree = isAuthenticated { implicit login =>
    forSuperUser { implicit request =>
      DB.withConnection { implicit conn =>
        {
          val locale = LocaleInfo.byLang(lang)

          // list of parent-name pairs
          val pns: Seq[(Category, CategoryName)] = CategoryPath.listNamesWithParent(locale)

          val idToName: Map[Long, CategoryName] = pns.map { t => (t._2.categoryId, t._2) }.toMap

          //map of Category to list of its child CategoryNames
          val pnSubTrees: Map[Long, Seq[Long]] =
            pns.foldLeft(Map[Long, Seq[Long]]()) { (subTree, pn) =>
              val p = pn._1
              val name = pn._2
              val pid = p.id.get
              val myid = name.categoryId
              val childrenIds = subTree.get(pid).getOrElse(Seq())

              subTree + (pid -> (if (myid == pid) childrenIds else childrenIds :+ myid))
            }

          def categoryChildren(categoryIds: Seq[Long]): Seq[CategoryNode] =
            categoryIds map { id =>
              CategoryNode(
                key = id,
                title = idToName(id).name,
                children = categoryChildren(pnSubTrees(id)))
            }

          val roots: Seq[Long] = Category.root(locale) map { _.id.get }

          val pathTree = categoryChildren(roots)

          //val pathTree = categoryChildren(Category.root,locale)
          Ok(Json.toJson(pathTree))
        }
      }
    }
  }

  val moveCategoryForm = Form(tuple(
    "categoryId" -> longNumber,
    "parentCategoryId" -> optional(longNumber)))
  def moveCategory = isAuthenticated { implicit login =>
    forSuperUser { implicit request =>
      val (categoryId, parentCategoryId) = moveCategoryForm.bindFromRequest.get;
      try {
        DB.withConnection { implicit conn =>
          Category.move(
            Category.get(categoryId).get,
            parentCategoryId map { Category.get(_).get })
        }
        Ok
      } catch {
        case e: Throwable => BadRequest
      }
    }
  }
}
