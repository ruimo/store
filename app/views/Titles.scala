package views

import play.api.i18n.{Lang, Messages}

object Titles {
  def top(implicit lang: Lang): String = Messages("company.name")
}
