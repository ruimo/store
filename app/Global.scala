import play.api.mvc._
import play.api._
import play.filters.csrf._
import controllers.I18nFilter

object Global extends WithFilters(CSRFFilter(), I18nFilter) with GlobalSettings

