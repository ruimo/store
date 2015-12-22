package constraints

import scala.util.matching.Regex
import play.api.data.validation.Constraint
import play.api.data.validation.Constraints._
import play.api.data.validation.{Invalid, Valid, ValidationError}
import helpers.Cache

trait FormConstraintsBase {
  def passwordMinLength: () => Int = Cache.config(_.getInt("password.min.length").getOrElse(6))
  val userNameMinLength = 6
  def userNameConstraint: () => Seq[Constraint[String]] =
    () => Seq(minLength(userNameMinLength), maxLength(24))
  def normalUserNameConstraint: () => Seq[Constraint[String]] = Cache.config(
    _.getString("normalUserNamePattern").map { patStr =>
      Seq(pattern(patStr.r, "normalUserNamePatternRule", "normalUserNamePatternError"))
    }.getOrElse(
      Seq(minLength(userNameMinLength), maxLength(24))
    )
  )

  val passwordConstraint = List(minLength(passwordMinLength()), maxLength(24), passwordCharConstraint)
  val firstNameConstraint = List(nonEmpty, maxLength(64))
  val lastNameConstraint = List(nonEmpty, maxLength(64))
  val emailConstraint = List(nonEmpty, maxLength(255))
  val companyNameConstraint = List(nonEmpty, maxLength(32))
  def passwordCharConstraint: Constraint[String] = Constraint[String]("constraint.password.char") { s =>
    if (s.forall(c => (0x21 <= c && c < 0x7e))) Valid else Invalid(ValidationError("error.pasword.char"))
  }
}
