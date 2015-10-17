package controllers

import java.sql.Connection
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.mvc.Controller
import controllers.I18n.I18nAware
import play.api.data.Form
import models.{ LocaleInfo, CreateCategory, Category, CategoryPath, CategoryName, OrderBy, PagedRecords }
import play.api.i18n.Messages
import play.api.db.DB
import play.api.Play.current
import scala.collection.immutable
import models.{RemoveCategoryName, UpdateCategoryName, UpdateCategoryNameTable, UniqueConstraintException}

import play.api.libs.json._

object CategoryMaintenance extends Controller with I18nAware with NeedLogin with HasLogger {
  val createCategoryForm = Form(
    mapping(
      "langId" -> longNumber,
      "categoryName" -> text.verifying(nonEmpty, maxLength(32)),
      "parent" -> optional(longNumber)
    )(CreateCategory.apply)(CreateCategory.unapply)
  )

  val updateCategoryNameForm = Form(
    mapping(
      "categoryNames" -> seq(
        mapping(
          "categoryId" -> longNumber,
          "localeId" -> longNumber,
          "name" -> text.verifying(nonEmpty, maxLength(32))
        )(UpdateCategoryName.apply)(UpdateCategoryName.unapply)
      )
    )(UpdateCategoryNameTable.apply)(UpdateCategoryNameTable.unapply)
  )

  val createCategoryNameForm = Form(
    mapping(
      "categoryId" -> longNumber,
      "localeId" -> longNumber,
      "name" -> text.verifying(nonEmpty, maxLength(32))
    )(UpdateCategoryName.apply)(UpdateCategoryName.unapply)
  )

  val removeCategoryNameForm = Form(
    mapping(
      "categoryId" -> longNumber,
      "localeId" -> longNumber
    )(RemoveCategoryName.apply)(RemoveCategoryName.unapply)
  )

  def index = NeedAuthenticated { implicit request =>
    implicit val login = request.user

    assumeSuperUser(login) {
      Ok(views.html.admin.categoryMaintenance())
    }
  }

  def startCreateNewCategory = NeedAuthenticated { implicit request =>
    implicit val login = request.user

    assumeSuperUser(login) {
      Ok(views.html.admin.createNewCategory(createCategoryForm, LocaleInfo.localeTable))
    }
  }

  def createNewCategory = NeedAuthenticated { implicit request =>
    implicit val login = request.user

    assumeSuperUser(login) {
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

  def editCategory(
    langSpec: Option[Long], start: Int, size: Int, orderBySpec: String
  ) = NeedAuthenticated { implicit request =>
    implicit val login = request.user

    assumeSuperUser(login) {
      val locale: LocaleInfo = langSpec.map(LocaleInfo.apply).getOrElse(LocaleInfo.getDefault)

      DB.withConnection { implicit conn =>
        {
          val categories: PagedRecords[(Category, Option[CategoryName])] = Category.listWithName(
            page = start, pageSize = size, locale = locale, OrderBy(orderBySpec)
          )
          Ok(views.html.admin.editCategory(locale, LocaleInfo.localeTable, categories))
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

  def categoryPathTree = NeedAuthenticated { implicit request =>
    implicit val login = request.user

    assumeSuperUser(login) {
      DB.withConnection { implicit conn =>
        {
          val locale = LocaleInfo.getDefault

          // list of parent-name pairs
          val pns: Seq[(Long, CategoryName)] = CategoryPath.listNamesWithParent(locale)

          val idToName: Map[Long, CategoryName] = pns.map { t => (t._2.categoryId, t._2) }.toMap

          //map of Category to list of its child CategoryNames
          val pnSubTrees: Map[Long, Seq[Long]] =
            pns.foldLeft(Map[Long, Seq[Long]]()) { (subTree, pn) =>
              val name = pn._2
              val pid = pn._1
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

          Ok(Json.toJson(pathTree))
        }
      }
    }
  }

  val moveCategoryForm = Form(tuple(
    "categoryId" -> longNumber,
    "parentCategoryId" -> optional(longNumber)))

  def moveCategory = NeedAuthenticated { implicit request =>
    implicit val login = request.user

    assumeSuperUser(login) {
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

  def createUpdateForms(categoryId: Long)(implicit conn: Connection): Form[UpdateCategoryNameTable] =
    updateCategoryNameForm.fill(
      UpdateCategoryNameTable(
        CategoryName.all(categoryId).values.toSeq.map { cn =>
          UpdateCategoryName(cn.categoryId, cn.locale.id, cn.name)
        }
      )
    )

  def editCategoryName(categoryId: Long) = NeedAuthenticated { implicit request =>
    implicit val login = request.user

    assumeSuperUser(login) {
      DB.withConnection { implicit conn =>
        Ok(
          views.html.admin.editCategoryName(
            categoryId,
            createCategoryNameForm,
            createUpdateForms(categoryId),
            removeCategoryNameForm,
            LocaleInfo.localeTable
          )
        )
      }
    }
  }

  def updateCategoryName(categoryId: Long) = NeedAuthenticated { implicit request =>
    implicit val login = request.user

    assumeSuperUser(login) {
      updateCategoryNameForm.bindFromRequest.fold(
        formWithErrors => {
          logger.error("Validation error in CategoryMaintenance.updateCategoryName.")
          BadRequest(
            views.html.admin.editCategoryName(
              categoryId,
              createCategoryNameForm,
              formWithErrors,
              removeCategoryNameForm,
              LocaleInfo.localeTable
            )
          )
        },
        newCategoryName => DB.withConnection { implicit conn =>
          newCategoryName.save()
          Redirect(
            routes.CategoryMaintenance.editCategory(None)
          ).flashing("message" -> Messages("categoryIsUpdated"))
        }
      )
    }
  }

  def createCategoryName(categoryId: Long) = NeedAuthenticated { implicit request =>
    implicit val login = request.user

    assumeSuperUser(login) {
      createCategoryNameForm.bindFromRequest.fold(
        formWithErrors => {
          logger.error("Validation error in CategoryMaintenance.createCategoryName.")
          DB.withConnection { implicit conn =>
            BadRequest(
              views.html.admin.editCategoryName(
                categoryId,
                formWithErrors,
                createUpdateForms(categoryId),
                removeCategoryNameForm,
                LocaleInfo.localeTable
              )
            )
          }
        },
        newCategoryName => DB.withConnection { implicit conn =>
          try {
            newCategoryName.create()
            Redirect(
              routes.CategoryMaintenance.editCategoryName(categoryId)
            ).flashing("message" -> Messages("categoryIsUpdated"))
          }
          catch {
            case e: UniqueConstraintException => {
              BadRequest(
                views.html.admin.editCategoryName(
                  categoryId,
                  createCategoryNameForm.fill(newCategoryName).withError("localeId", "unique.constraint.violation"),
                  createUpdateForms(categoryId),
                  removeCategoryNameForm,
                  LocaleInfo.localeTable
                )
              )
            }
          }
        }
      )
    }
  }

  def removeCategoryName(categoryId: Long) = NeedAuthenticated { implicit request =>
    implicit val login = request.user

    assumeSuperUser(login) {
      removeCategoryNameForm.bindFromRequest.fold(
        formWithErrors => {
          logger.error("Validation error in CategoryMaintenance.createCategoryName.")
          DB.withConnection { implicit conn =>
            BadRequest(
              views.html.admin.editCategoryName(
                categoryId,
                createCategoryNameForm,
                createUpdateForms(categoryId),
                removeCategoryNameForm,
                LocaleInfo.localeTable
              )
            )
          }
        },
        categoryName => DB.withConnection { implicit conn =>
          categoryName.remove()
          Redirect(
            routes.CategoryMaintenance.editCategoryName(categoryId)
          ).flashing("message" -> Messages("categoryIsRemoved"))
        }
      )
    }
  }
}
