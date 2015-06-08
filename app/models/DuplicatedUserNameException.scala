package models

class DuplicatedUserNameException(val userName: String) extends Exception("User name '" + userName + "' is duplicated.")
