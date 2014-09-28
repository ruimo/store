package controllers

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

class UserMaintenanceImpl extends Controller with I18nAware with NeedLogin with HasLogger {
  implicit val tokenGenerator: TokenGenerator = RandomTokenGenerator()
  lazy val config = play.api.Play.maybeApplication.map(_.configuration).get
  lazy val siteOwnerCanUploadUserCsv = config.getBoolean("siteOwnerCanUploadUserCsv").getOrElse(false)

  def modifyUserForm(implicit lang: Lang) = Form(
    mapping(
      "userId" -> longNumber,
      "userName" -> text.verifying(userNameConstraint: _*),
      "firstName" -> text.verifying(firstNameConstraint: _*),
      "middleName" -> optional(text),
      "lastName" -> text.verifying(lastNameConstraint: _*),
      "email" -> email.verifying(emailConstraint: _*),
      "password" -> tuple(
        "main" -> text.verifying(userNameConstraint: _*),
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
      "userName" -> text.verifying(userNameConstraint: _*),
      "firstName" -> text.verifying(firstNameConstraint: _*),
      "middleName" -> optional(text),
      "lastName" -> text.verifying(lastNameConstraint: _*),
      "email" -> email.verifying(emailConstraint: _*),
      "password" -> tuple(
        "main" -> text.verifying(userNameConstraint: _*),
        "confirm" -> text
      ).verifying(
        Messages("confirmPasswordDoesNotMatch"), passwords => passwords._1 == passwords._2
      ),
      "companyName" -> text.verifying(companyNameConstraint: _*)
    )(CreateSiteOwner.fromForm)(CreateSiteOwner.toForm)
  )

  def index = isAuthenticated { implicit login => forAdmin { implicit request =>
    if (siteOwnerCanUploadUserCsv || login.isSuperUser) {
      Ok(views.html.admin.userMaintenance())
    }
    else {
      Redirect(routes.Admin.index)
    }
  }}

  def startCreateNewSuperUser = isAuthenticated { implicit login => forSuperUser { implicit request =>
    Ok(views.html.admin.createNewSuperUser(Admin.createUserForm(FirstSetup.fromForm, FirstSetup.toForm)))
  }}

  def startCreateNewSiteOwner = isAuthenticated { implicit login => forSuperUser { implicit request =>
    DB.withConnection { implicit conn =>
      Ok(views.html.admin.createNewSiteOwner(newSiteOwnerForm, Site.tableForDropDown))
    }
  }}

  def startCreateNewNormalUser = isAuthenticated { implicit login => forSuperUser { implicit request =>
    Ok(views.html.admin.createNewNormalUser(Admin.createUserForm(CreateNormalUser.fromForm, CreateNormalUser.toForm)))
  }}

  def createNewSuperUser = isAuthenticated { implicit login => forSuperUser { implicit request =>
    Admin.createUserForm(FirstSetup.fromForm, FirstSetup.toForm).bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in UserMaintenance.createNewSuperUser.")
        BadRequest(views.html.admin.createNewSuperUser(formWithErrors))
      },
      newUser => DB.withConnection { implicit conn =>
        newUser.save
        Redirect(
          routes.UserMaintenance.startCreateNewSuperUser
        ).flashing("message" -> Messages("userIsCreated"))
      }
    )
  }}

  def createNewSiteOwner = isAuthenticated { implicit login => forSuperUser { implicit request =>
    newSiteOwnerForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in UserMaintenance.createNewSiteOwner.")
        DB.withConnection { implicit conn =>
          BadRequest(views.html.admin.createNewSiteOwner(formWithErrors, Site.tableForDropDown))
        }
      },
      newUser => DB.withTransaction { implicit conn =>
        newUser.save
        Redirect(
          routes.UserMaintenance.startCreateNewSiteOwner
        ).flashing("message" -> Messages("userIsCreated"))
      }
    )
  }}

  def createNewNormalUser = isAuthenticated { implicit login => forSuperUser { implicit request =>
    Admin.createUserForm(CreateNormalUser.fromForm, CreateNormalUser.toForm).bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in UserMaintenance.createNewNormalUser.")
        BadRequest(views.html.admin.createNewNormalUser(formWithErrors))
      },
      newUser => DB.withConnection { implicit conn =>
        newUser.save
        Redirect(
          routes.UserMaintenance.startCreateNewNormalUser
        ).flashing("message" -> Messages("userIsCreated"))
      }
    )
  }}

  def editUser(
    page: Int, pageSize: Int, orderBySpec: String
  ) = isAuthenticated { implicit login => forSuperUser { implicit request =>
    DB.withConnection { implicit conn =>
      Ok(views.html.admin.editUser(StoreUser.listUsers(page, pageSize, OrderBy(orderBySpec))))
    }
  }}

  def modifyUserStart(userId: Long) = isAuthenticated { implicit login => forSuperUser { implicit request =>
    DB.withConnection { implicit conn =>
      Ok(views.html.admin.modifyUser(modifyUserForm.fill(ModifyUser(StoreUser.withSite(userId)))))
    }
  }}

  def modifyUser = isAuthenticated { implicit login => forSuperUser { implicit request =>
    modifyUserForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in UserMaintenance.modifyUser.")
        BadRequest(views.html.admin.modifyUser(formWithErrors))
      },
      newUser => DB.withConnection { implicit conn =>
        newUser.update
        Redirect(
          routes.UserMaintenance.editUser()
        ).flashing("message" -> Messages("userIsUpdated"))
      }
    )
  }}

  def deleteUser(id: Long) = isAuthenticated { implicit login => forSuperUser { implicit request =>
    DB.withConnection { implicit conn =>
      StoreUser.delete(id)
    }
    Redirect(routes.UserMaintenance.editUser())
  }}

  def startAddUsersByCsv = isAuthenticated { implicit login => forAdmin { implicit request =>
    if (siteOwnerCanUploadUserCsv || login.isSuperUser) {
      DB.withConnection { implicit conn =>
//        Ok(views.html.admin.addUsersByCsv(Site.tableForDropDown))
        Ok(views.html.admin.addUsersByCsv())
      }
    }
    else {
      Redirect(routes.Admin.index)
    }
  }}

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
//          val siteId = request.body.dataParts("site").head.toLong

          request.body.file("attachment").map { csvFile =>
            val filename = csvFile.filename
            val contentType = csvFile.contentType
            logger.info("Users are uploaded. filename='" + filename + "', contentType='" + contentType + "'")
            if (contentType != Some("text/csv")) {
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
  )(implicit lang: Lang): PlainResult = {
    import com.ruimo.csv.Parser.parseLines
    import Files.newBufferedReader
    
    iteratorFromReader(newBufferedReader(path, Charset.forName("Windows-31j"))) {
      in: Iterator[Char] =>
        val z: Iterator[Try[CsvRecord]] = asHeaderedCsv(parseLines(in))
        DB.withConnection { implicit conn => 
          StoreUser.maintainByCsv(
            z,
            csvRecordFilter,
            deleteSqlSupplemental
          ) 
        }
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
        case t: Throwable =>
          logger.error("CSV general error", t)
          Redirect(
            routes.UserMaintenance.startAddUsersByCsv()
          ).flashing("errorMessage" -> Messages("general.error"))
      }
    }
  }
}
