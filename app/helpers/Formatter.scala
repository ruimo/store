package helpers

import org.joda.time.format.DateTimeFormat
import play.api.data.validation.ValidationError
import play.api.i18n.Messages
import play.api.data.validation.Constraint
import play.api.data.validation.Invalid

object Formatter {
  val yyyy_mm_dd = DateTimeFormat.forPattern("yyyy-MM-dd")
  def validationErrorsToString(errors: Seq[ValidationError]): String =
    errors.map {e => Messages(e.message, e.args: _*)}.mkString(Messages("PeriodSymbol"))
  def validationErrorString[T](constraints: Seq[Constraint[T]], value: T): String =
    validationErrorsToString(
      constraints.map(_.apply(value)).collect {
        case Invalid(errors) => errors
      }.flatten
    )
}
