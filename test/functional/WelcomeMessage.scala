package functional

import play.api.i18n.{Lang, Messages}

object WelcomeMessage {
  def welcomeMessage(implicit lang: Lang): String = Messages("login.welcome").format(Messages("guest"), "", "").replaceAll("  *", " ")
}
