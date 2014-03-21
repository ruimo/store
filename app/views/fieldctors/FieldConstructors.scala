package views.fieldctors

import views.html.helper.FieldConstructor
import views.html.helper.FieldElements
import play.api.i18n.Lang

object FieldConstructors {
  implicit def showOnlyRequired(implicit lang: Lang) = new FieldConstructor {
    def apply(e: FieldElements) = views.html.fieldctors.onlyRequired(e)
  }
}
