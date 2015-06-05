package models

import play.api.data.validation.ValidationError

class InvalidUserNameException(userName: String, errors: Seq[ValidationError]) extends Exception
