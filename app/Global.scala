import controllers.I18n.I18nFilter
import play.api.mvc._
import play.api._
import play.filters.csrf._

object Global extends WithFilters(CSRFFilter(), I18nFilter) with GlobalSettings

