package constraints

import scala.util.matching.Regex
import play.api.data.validation.Constraint
import play.api.data.validation.Constraints._
import play.api.data.validation.{Invalid, Valid, ValidationError}
import helpers.Cache

object FormConstraints extends FormConstraintsBase
