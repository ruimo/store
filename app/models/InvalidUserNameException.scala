package models

import play.api.data.validation.ValidationError

class InvalidUserNameException(val userName: String, val errors: Seq[ValidationError]) extends Exception
