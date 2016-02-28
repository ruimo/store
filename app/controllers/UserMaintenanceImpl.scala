package controllers

import helpers.Formatter
import models.InvalidUserNameException
import java.sql.Connection
import helpers.{PasswordHash, TokenGenerator, RandomTokenGenerator}
import constraints.FormConstraints._
import java.nio.file.Path
import scala.util.{Try, Failure, Success}
import java.nio.file.Files
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models._
import play.api.data.Form
import controllers.I18n.I18nAware
import play.api.mvc._
import play.api.db.DB
import play.api.i18n.{Lang, Messages}
import play.api.Play.current
import helpers.{QueryString, TokenGenerator, RandomTokenGenerator}
import com.ruimo.scoins.LoanPattern.iteratorFromReader
import java.nio.charset.Charset
import com.ruimo.csv.CsvParseException
import com.ruimo.csv.CsvRecord
import com.ruimo.csv.Parser._
import play.api.Play
import helpers.Cache

class UserMaintenanceImpl extends Controller with I18nAware with NeedLogin with HasLogger {
  import NeedLogin._
  val EmployeeCsvRegistration: () => Boolean = Cache.config(
    _.getBoolean("employee.csv.registration").getOrElse(false)
  )
  val SiteOwnerCanEditEmployee: () => Boolean = Cache.config(
    _.getBoolean("siteOwnerCanEditEmployee").getOrElse(false)
  )
  val MaxCountOfSupplementalEmail: () => Int = Cache.config(
    _.getInt("maxCountOfSupplementalEmail").getOrElse(0)
  )

  implicit val tokenGenerator: TokenGenerator = RandomTokenGenerator()
  val SiteOwnerCanUploadUserCsv: () => Boolean = Cache.config(
    _.getBoolean("siteOwnerCanUploadUserCsv").getOrElse(false)
  )

  def createEmployeeForm(implicit lang: Lang) = Form(
    mapping(
      "userName" -> text.verifying(normalUserNameConstraint(): _*),
      "password" -> tuple(
        "main" -> text.verifying(passwordConstraint: _*),
        "confirm" -> text
      ).verifying(
        Messages("confirmPasswordDoesNotMatch"), passwords => passwords._1 == passwords._2
      )
    )(CreateEmployee.apply)(CreateEmployee.unapply)
  )

  def modifyUserForm(implicit lang: Lang) = Form(
    mapping(
      "userId" -> longNumber,
      "userName" -> text.verifying(userNameConstraint(): _*),
      "firstName" -> text.verifying(firstNameConstraint: _*),
      "middleName" -> optional(text),
      "lastName" -> text.verifying(lastNameConstraint: _*),
      "email" -> email.verifying(emailConstraint: _*),
      "supplementalEmails" -> seq(optional(email.verifying(optionalEmailConstraint))),
      "password" -> tuple(
        "main" -> text.verifying(passwordConstraint: _*),
        "confirm" -> text
      ).verifying(
        Messages("confirmPasswordDoesNotMatch"), passwords => passwords._1 == passwords._2
      ),
      "companyName" -> text.verifying(companyNameConstraint: _*),
      "sendNoticeMail" -> boolean
    )(ModifyUser.fromForm)(ModifyUser.toForm)
  )

  def newSiteOwnerForm(implicit lang: Lang) = Form(
    mapping(
      "siteId" -> longNumber,
      "userName" -> text.verifying(userNameConstraint(): _*),
      "firstName" -> text.verifying(firstNameConstraint: _*),
      "middleName" -> optional(text),
      "lastName" -> text.verifying(lastNameConstraint: _*),
      "email" -> email.verifying(emailConstraint: _*),
      "supplementalEmails" -> seq(optional(email.verifying(optionalEmailConstraint))),
      "password" -> tuple(
        "main" -> text.verifying(passwordConstraint: _*),
        "confirm" -> text
      ).verifying(
        Messages("confirmPasswordDoesNotMatch"), passwords => passwords._1 == passwords._2
      ),
      "companyName" -> text.verifying(companyNameConstraint: _*)
    )(CreateSiteOwner.fromForm)(CreateSiteOwner.toForm)
  )

  def index = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeAdmin(login) {
      if (SiteOwnerCanUploadUserCsv() || login.isSuperUser) {
        Ok(views.html.admin.userMaintenance())
      }
      else {
        Redirect(routes.Admin.index)
      }
    }
  }

  def startCreateNewSuperUser = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      Ok(
        views.html.admin.createNewSuperUser(
          Admin.createUserForm(FirstSetup.fromForm, FirstSetup.toForm),
          MaxCountOfSupplementalEmail()
        )
      )
    }
  }

  def startCreateNewSiteOwner = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      DB.withConnection { implicit conn =>
        Ok(
          views.html.admin.createNewSiteOwner(
            newSiteOwnerForm, Site.tableForDropDown, MaxCountOfSupplementalEmail()
          )
        )
      }
    }
  }

  def startCreateNewNormalUser = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      Ok(
        views.html.admin.createNewNormalUser(
          Admin.createNormalUserForm(CreateNormalUser.fromForm, CreateNormalUser.toForm),
          MaxCountOfSupplementalEmail()
        )
      )
    }
  }

  def startCreateNewEmployeeUser = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSiteOwner(login) {
      if (SiteOwnerCanEditEmployee()) {
        val siteId = login.siteUser.map(_.siteId).get
        DB.withConnection { implicit conn =>
          Ok(views.html.admin.createNewEmployeeUser(Site(siteId), createEmployeeForm))
        }
      }
      else {
        Redirect(routes.Application.index)
      }
    }
  }

  def createNewEmployeeUser = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSiteOwner(login) {
      createEmployeeForm.bindFromRequest.fold(
        formWithErrors => {
          logger.error("Validation error in UserMaintenance.createNewEmployeeUser." + formWithErrors)
          val siteId = login.siteUser.map(_.siteId).get
          DB.withConnection { implicit conn =>
            BadRequest(views.html.admin.createNewEmployeeUser(Site(siteId), formWithErrors))
          }
        },
        newUser => {
          if (SiteOwnerCanEditEmployee()) {
            val siteId = login.siteUser.map(_.siteId).get
            val salt = tokenGenerator.next
            DB.withConnection { implicit conn =>
              val createdUser = StoreUser.create(
                userName = siteId + "-" + newUser.userName,
                firstName = "",
                middleName = None,
                lastName = "",
                email = "",
                passwordHash = PasswordHash.generate(newUser.passwords._1, salt),
                salt = salt,
                userRole = UserRole.NORMAL,
                companyName = Some(Site(siteId).name)
              )

              Employee.createNew(siteId, createdUser.id.get)
            }

            Redirect(
              routes.UserMaintenance.startCreateNewEmployeeUser()
            ).flashing("message" -> Messages("userIsCreated"))
          }
          else {
            Redirect(routes.Application.index)
          }
        }
      )
    }
  }

  def createNewSuperUser = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      Admin.createUserForm(FirstSetup.fromForm, FirstSetup.toForm).bindFromRequest.fold(
        formWithErrors => {
          logger.error("Validation error in UserMaintenance.createNewSuperUser." + formWithErrors)
          BadRequest(
            views.html.admin.createNewSuperUser(
              formWithErrors, MaxCountOfSupplementalEmail()
            )
          )
        },
        newUser => DB.withConnection { implicit conn =>
          newUser.save
          Redirect(
            routes.UserMaintenance.startCreateNewSuperUser
          ).flashing("message" -> Messages("userIsCreated"))
        }
      )
    }
  }

  def createNewSiteOwner = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      newSiteOwnerForm.bindFromRequest.fold(
        formWithErrors => {
          logger.error("Validation error in UserMaintenance.createNewSiteOwner." + formWithErrors)
          DB.withConnection { implicit conn =>
            BadRequest(
              views.html.admin.createNewSiteOwner(
                formWithErrors, Site.tableForDropDown, MaxCountOfSupplementalEmail()
              )
            )
          }
        },
        newUser => DB.withTransaction { implicit conn =>
          newUser.save
          Redirect(
            routes.UserMaintenance.startCreateNewSiteOwner
          ).flashing("message" -> Messages("userIsCreated"))
        }
      )
    }
  }

  def createNewNormalUser = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      Admin.createNormalUserForm(CreateNormalUser.fromForm, CreateNormalUser.toForm).bindFromRequest.fold(
        formWithErrors => {
          logger.error("Validation error in UserMaintenance.createNewNormalUser." + formWithErrors)
          BadRequest(
            views.html.admin.createNewNormalUser(
              formWithErrors, MaxCountOfSupplementalEmail()
            )
          )
        },
        newUser => DB.withConnection { implicit conn =>
          newUser.save
          Redirect(
            routes.UserMaintenance.startCreateNewNormalUser
          ).flashing("message" -> Messages("userIsCreated"))
        }
      )
    }
  }

  def editUser(
    page: Int, pageSize: Int, orderBySpec: String
  ) = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeAdmin(login) {
      DB.withConnection { implicit conn =>
        if (login.isSuperUser) {
          Ok(
            views.html.admin.editUser(
              StoreUser.listUsers(page, pageSize, OrderBy(orderBySpec))
            )
          )
        }
        else { // Store owner
          if (SiteOwnerCanEditEmployee()) {
            val siteId = login.siteUser.map(_.siteId).get
            Ok(
              views.html.admin.editUser(
                StoreUser.listUsers(page, pageSize, OrderBy(orderBySpec), employeeSiteId = Some(siteId))
              )
            )
          }
          else {
            Redirect(routes.Application.index)
          }
        }
      }
    }
  }

  def modifyUserStart(userId: Long) = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeAdmin(login) {
      DB.withConnection { implicit conn =>
        val user: ListUserEntry = StoreUser.withSite(userId)
        if (login.isSuperUser) {
          Ok(
            views.html.admin.modifyUser(
              user,
              modifyUserForm.fill(
                ModifyUser(user, SupplementalUserEmail.load(userId).toSeq)
              ),
              MaxCountOfSupplementalEmail()
            )
          )
        }
        else { // Store owner
          if (canEditEmployee(userId, login.siteUser.map(_.siteId).get)) {
            Ok(
              views.html.admin.modifyUser(
                user, modifyUserForm.fill(
                  ModifyUser(user, SupplementalUserEmail.load(userId).toSeq)
                ),
                MaxCountOfSupplementalEmail()
              )
            )
          }
          else {
            Redirect(routes.Application.index)
          }
        }
      }
    }
  }

  def modifyUser(userId: Long) = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeAdmin(login) {
      modifyUserForm.bindFromRequest.fold(
        formWithErrors => {
          logger.error("Validation error in UserMaintenance.modifyUser." + formWithErrors)
          DB.withConnection { implicit conn =>
            val user: ListUserEntry = StoreUser.withSite(userId)
            BadRequest(
              views.html.admin.modifyUser(user, formWithErrors, MaxCountOfSupplementalEmail())
            )
          }
        },
        newUser => DB.withTransaction { implicit conn =>
          if (login.isSuperUser || canEditEmployee(newUser.userId, login.siteUser.map(_.siteId).get)) {
            newUser.update
            Redirect(
              routes.UserMaintenance.editUser()
            ).flashing("message" -> Messages("userIsUpdated"))
          }
          else {
            Redirect(routes.Application.index)
          }
        }
      )
    }
  }

  def deleteUser(id: Long) = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeAdmin(login) {
      DB.withConnection { implicit conn =>
        if (login.isSuperUser || canEditEmployee(id, login.siteUser.map(_.siteId).get)) {
          StoreUser.delete(id)
          Redirect(routes.UserMaintenance.editUser())
        }
        else {
          Redirect(routes.Application.index)
        }
      }
    }
  }

  def canEditEmployee(userId: Long, siteId: Long)(implicit conn: Connection): Boolean =
    StoreUser(userId).isEmployeeOf(siteId) && SiteOwnerCanEditEmployee()

  def startAddUsersByCsv = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeAdmin(login) {
      if (SiteOwnerCanUploadUserCsv() || login.isSuperUser) {
        DB.withConnection { implicit conn =>
          Ok(views.html.admin.addUsersByCsv())
        }
      }
      else {
        Redirect(routes.Admin.index)
      }
    }
  }

  def addUsersByCsv = maintainUsersByCsv(
    csvRecordFilter = (_, _) => true,
    deleteSqlSupplemental = _ => None
  )

  def maintainUsersByCsv(
    csvRecordFilter: (Map[String, Seq[String]], CsvRecord) => Boolean,
    deleteSqlSupplemental: Map[String, Seq[String]] => Option[String]
  ) = Action(parse.multipartFormData) { implicit request =>
    retrieveLoginSession(request) match {
      case None => onUnauthorized(request)
      case Some(user) =>
        if (user.isBuyer) onUnauthorized(request)
        else {
          request.body.file("attachment").map { csvFile =>
            val filename = csvFile.filename
            val contentType = csvFile.contentType
            logger.info("Users are uploaded. filename='" + filename + "', contentType='" + contentType + "'")
            if (contentType != Some("text/csv") && contentType != Some("application/vnd.ms-excel")) {
              Redirect(
                routes.UserMaintenance.startAddUsersByCsv()
              ).flashing("errorMessage" -> Messages("csv.needed"))
            }
            else {
              createResultFromUserCsvFile(
                csvFile.ref.file.toPath,
                csvRecordFilter(request.body.dataParts, _: CsvRecord),
                deleteSqlSupplemental(request.body.dataParts)
              )
            }
          }.getOrElse {
            logger.error("Users are uploaded. But no attachment found.")
            Redirect(routes.UserMaintenance.startAddUsersByCsv()).flashing(
              "errorMessage" -> Messages("file.not.found")
            )
          }.withSession(
            request.session + 
            (LoginUserKey -> user.withExpireTime(System.currentTimeMillis + SessionTimeout).toSessionString)
          )
        }
    }
  }  

  def createResultFromUserCsvFile(
    path: Path,
    csvRecordFilter: CsvRecord => Boolean,
    deleteSqlSupplemental: Option[String]
  )(implicit lang: Lang): Result = {
    import com.ruimo.csv.Parser.parseLines
    import Files.newBufferedReader
    
    iteratorFromReader(newBufferedReader(path, Charset.forName("Windows-31j"))) { in: Iterator[Char] =>
      val z: Iterator[Try[CsvRecord]] = asHeaderedCsv(parseLines(in))
      StoreUser.maintainByCsv(
        z,
        csvRecordFilter,
        deleteSqlSupplemental,
        EmployeeCsvRegistration()
      )
    } match {
      case Success(updatedColumnCount) =>
        Redirect(
          routes.UserMaintenance.startAddUsersByCsv()
        ).flashing("message" -> Messages("usersAreUpdated", updatedColumnCount._1, updatedColumnCount._2))

      case Failure(e) => e match {
        case cpe: CsvParseException =>
          logger.error("CSV format error", cpe)
          Redirect(
            routes.UserMaintenance.startAddUsersByCsv()
          ).flashing("errorMessage" -> Messages("csv.error", cpe.lineNo))
        case e: InvalidUserNameException =>
          logger.error("User name '" + e.userName + "' in CSV is invalid." + e.errors)
          Redirect(
            routes.UserMaintenance.startAddUsersByCsv()
          ).flashing(
            "errorMessage" -> (Formatter.validationErrorsToString(e.errors) + s"'${e.userName}'")
          )
        case e: DuplicatedUserNameException =>
          logger.error("User name '" + e.userName + "' is duplicated in CSV.")
          Redirect(
            routes.UserMaintenance.startAddUsersByCsv()
          ).flashing(
            "errorMessage" -> Messages("userNameDuplicated", e.userName)
          )

        case t: Throwable =>
          logger.error("CSV general error", t)
          Redirect(
            routes.UserMaintenance.startAddUsersByCsv()
          ).flashing("errorMessage" -> Messages("general.error"))
      }
    }
  }

  def showRegisteredEmployeeCount = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeAdmin(login) {
      DB.withConnection { implicit conn =>
        Ok(
          views.html.admin.showRegisteredEmployeeCount(
            Site.listAsMap,
            StoreUser.registeredEmployeeCount
          )
        )
      }
    }
  }
}
