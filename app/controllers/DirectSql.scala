package controllers

import scala.util.Try
import scala.util.Success
import scala.util.Failure
import anorm._
import anorm.{NotAssigned, Pk}
import anorm.SqlParser
import play.api._
import db.DB
import play.api.mvc._
import controllers.I18n.I18nAware
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import models.DirectSqlExec

object DirectSql extends Controller with I18nAware with NeedLogin with HasLogger {
  val SqlPattern = """(?m);$""".r

  val directSqlForm = Form(
    mapping(
      "sql" -> text
    )(DirectSqlExec.apply)(DirectSqlExec.unapply)
  )

  def index = isAuthenticated { implicit login => forSuperUser { implicit request =>
    Ok(views.html.admin.directSqlIndex(directSqlForm, List()))
  }}

  def execute = isAuthenticated { implicit login => forSuperUser { implicit request =>
    directSqlForm.bindFromRequest.fold(
      formWithErrors => {
        logger.error("Validation error in DirectSql.execute.")
        BadRequest(views.html.admin.directSqlIndex(formWithErrors, List()))
      },
      sql => {
        val results = executeSql(sql.sql)
        if (errorExists(results)) {
          BadRequest(
            views.html.admin.directSqlIndex(
              directSqlForm.fill(sql).withError("sql", "SQL error"),
              results
            )
          )
        }
        else {
          Redirect(routes.DirectSql.index).flashing("message" -> "OK")
        }
      }
    )
  }}

  def executeSql(sql: String): Seq[(String, Try[Boolean])] = {
    val sqlTable = SqlPattern.split(sql)

    DB.withConnection { implicit conn =>
      sqlTable.map { s =>
        (s, Try(SQL(s).execute()))
      }.toSeq
    }
  }

  def errorExists(result: Seq[(String, Try[Boolean])]): Boolean =
    result.exists(t => t._2.isFailure || t._2.get == false)
}
