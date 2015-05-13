package constraints

import play.api.data.validation.Constraint
import play.api.data.validation.Constraints._
import play.api.data.validation.{Invalid, Valid, ValidationError}

object FormConstraints {
  val cfg = play.api.Play.maybeApplication.map(_.configuration).get
  lazy val passwordMinLength = cfg.getInt("password.min.length").getOrElse(6)

  val userNameMinLength = 6
  val userNameConstraint = List(minLength(userNameMinLength), maxLength(24))
  val passwordConstraint = List(minLength(passwordMinLength), maxLength(24), passwordCharConstraint)
  val firstNameConstraint = List(nonEmpty, maxLength(64))
  val lastNameConstraint = List(nonEmpty, maxLength(64))
  val emailConstraint = List(nonEmpty, maxLength(255))
  val companyNameConstraint = List(nonEmpty, maxLength(32))
  def passwordCharConstraint: Constraint[String] = Constraint[String]("constraint.password.char") { s =>
    if (s.forall(c => (0x21 <= c && c < 0x7e))) Valid else Invalid(ValidationError("error.pasword.char"))
  }
}
