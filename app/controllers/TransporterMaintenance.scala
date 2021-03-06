package controllers

import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.mvc.Controller
import controllers.I18n.I18nAware
import play.api.data.Form
import models._
import play.api.i18n.Messages
import play.api.db.DB
import play.api.Play.current
import models.CreateTransporter

case class ChangeTransporter(
  id: Long,
  langTable: Seq[(String, String)],
  transporterNameTableForm: Form[ChangeTransporterNameTable],
  newTransporterNameForm: Form[ChangeTransporterName]
)

object TransporterMaintenance extends Controller with I18nAware with NeedLogin with HasLogger {
  val createTransporterForm = Form(
    mapping(
      "langId" -> longNumber,
      "transporterName" -> text.verifying(nonEmpty, maxLength(64))
    ) (CreateTransporter.apply)(CreateTransporter.unapply)
  )

  val addTransporterNameForm = Form(
    mapping(
      "localeId" -> longNumber,
      "transporterName" -> text.verifying(nonEmpty, maxLength(64))
    ) (ChangeTransporterName.apply)(ChangeTransporterName.unapply)
  )

  val changeTransporterNameForm = Form(
    mapping(
      "transporterNames" -> seq(
        mapping(
          "localeId" -> longNumber,
          "transporterName" -> text.verifying(nonEmpty, maxLength(64))
        ) (ChangeTransporterName.apply)(ChangeTransporterName.unapply)
      )
    ) (ChangeTransporterNameTable.apply)(ChangeTransporterNameTable.unapply)
  )

  def index = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      Ok(views.html.admin.transporterMaintenance())
    }
  }

  def startCreateNewTransporter = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      Ok(views.html.admin.createNewTransporter(createTransporterForm, LocaleInfo.localeTable))
    }
  }

  def createNewTransporter = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      createTransporterForm.bindFromRequest.fold(
        formWithErrors => {
          logger.error("Validation error in TransporterMaintenance.createNewTransporter.")
          BadRequest(views.html.admin.createNewTransporter(formWithErrors, LocaleInfo.localeTable))
        },
        newTransporter => DB.withTransaction { implicit conn =>
          try {
            newTransporter.save
            Redirect(
              routes.TransporterMaintenance.startCreateNewTransporter
            ).flashing("message" -> Messages("transporterIsCreated"))
          }
          catch {
            case e: UniqueConstraintException =>
              BadRequest(
                views.html.admin.createNewTransporter(
                  createTransporterForm.fill(newTransporter).withError("transporterName", "unique.constraint.violation"),
                  LocaleInfo.localeTable
                )
              )
          }
        }
      )
    }
  }

  def editTransporter = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeSuperUser(login) {
      DB.withConnection { implicit conn =>
        Ok(views.html.admin.editTransporter(Transporter.listWithName))
      }
    }
  }

  def startChangeTransporter(id: Long) = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeAdmin(login) {
      Ok(views.html.admin.changeTransporter(
        ChangeTransporter(
          id,
          LocaleInfo.localeTable,
          createTransporterNameTable(id),
          addTransporterNameForm
        )
      ))
    }
  }

  def createTransporterNameTable(id: Long): Form[ChangeTransporterNameTable] = {
    DB.withConnection { implicit conn => {
      val transporterNames = TransporterName.list(id).values.map {
        n => ChangeTransporterName(n.localeId, n.transporterName)
      }.toSeq

      changeTransporterNameForm.fill(ChangeTransporterNameTable(transporterNames))
    }}
  }

  def removeTransporterName(id: Long, localeId: Long) = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeAdmin(login) {
      DB.withConnection { implicit conn =>
        TransporterName.remove(id, localeId)
      }

      Redirect(
        routes.TransporterMaintenance.startChangeTransporter(id)
      ).flashing("message" -> Messages("transporterIsUpdated"))
    }
  }

  def changeTransporterName(id: Long) = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeAdmin(login) {
      changeTransporterNameForm.bindFromRequest.fold(
        formWithErrors => {
          logger.error("Validation errro in TransporterMaintenance.changeTransporterName." + formWithErrors + ".")
          BadRequest(
            views.html.admin.changeTransporter(
              ChangeTransporter(
                id,
                LocaleInfo.localeTable,
                formWithErrors,
                addTransporterNameForm
              )
            )
          )
        },
        newTransporter => {
          DB.withTransaction { implicit conn =>
            newTransporter.update(id)
          }
          Redirect(
            routes.TransporterMaintenance.startChangeTransporter(id)
          ).flashing("message" -> Messages("transporterIsUpdated"))
        }
      )
    }
  }

  def addTransporterName(id: Long) = NeedAuthenticated { implicit request =>
    implicit val login = request.user
    assumeAdmin(login) {
      addTransporterNameForm.bindFromRequest.fold(
        formWithErrors => {
          logger.error("Validation error in TransporterMaintenance.addItemName." + formWithErrors + ".")
          BadRequest(
            views.html.admin.changeTransporter(
              ChangeTransporter(
                id,
                LocaleInfo.localeTable,
                createTransporterNameTable(id),
                formWithErrors
              )
            )
          )
        },
        newTransporter => {
          try {
            DB.withConnection { implicit conn =>
              newTransporter.add(id)
            }
            Redirect(
              routes.TransporterMaintenance.startChangeTransporter(id)
            ).flashing("message" -> Messages("transporterIsUpdated"))
          }
          catch {
            case e: UniqueConstraintException => {
              BadRequest(
                views.html.admin.changeTransporter(
                  ChangeTransporter(
                    id,
                    LocaleInfo.localeTable,
                    createTransporterNameTable(id),
                    addTransporterNameForm.fill(newTransporter).withError("localeId", "unique.constraint.violation")
                  )
                )
              )
            }
          }
        }
      )
    }
  }
}
