package models

import anorm._
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import collection.immutable.{HashMap, IntMap}
import java.sql.{Timestamp, Connection}

case class EmployeeId(id: Long) extends AnyVal

case class Employee(id: Option[EmployeeId] = None, siteId: Long, userId: Long)

object Employee {
  val simple = {
    SqlParser.get[Option[Long]]("employee.employee_id") ~
    SqlParser.get[Long]("employee.site_id") ~
    SqlParser.get[Long]("employee.store_user_id") map {
      case id~siteId~userId => Employee(id.map(EmployeeId.apply), siteId, userId)
    }
  }

  def apply(id: Long)(implicit conn: Connection): Employee =
    SQL(
      "select * from employee where employee_id = {id}"
    ).on(
      'id -> id
    ).as(simple.single)

  def createNew(siteId: Long, userId: Long)(implicit conn: Connection): Employee = {
    SQL(
      """
      insert into employee (employee_id, site_id, store_user_id) values (
        (select nextval('employee_seq')), {siteId}, {userId}
      )
      """
    ).on(
      'siteId -> siteId,
      'userId -> userId
    ).executeUpdate()

    val employeeId = SQL("select currval('employee_seq')").as(SqlParser.scalar[Long].single)

    Employee(Some(EmployeeId(employeeId)), siteId, userId)
  }

  def getBelonging(userId: Long)(implicit conn: Connection): Option[Employee] = SQL(
    """
    select * from employee where store_user_id = {userId}
    """
  ).on(
    'userId -> userId
  ).as(
    simple.singleOpt
  )
}
