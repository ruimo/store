package controllers

import play.api.i18n.Lang
import play.api.Play.current
import play.api.mvc.{Filter, Result, Controller, RequestHeader}

object I18n {
  def langByQueryParam(request: RequestHeader): Option[Lang] =
    request.getQueryString("lang").map(Lang.apply)

  trait I18nAware extends Controller with HasLogger {
    override implicit def lang(implicit request: RequestHeader): Lang =
      langByQueryParam(request) match {
        case None => super.lang(request)
        case Some(lang) => lang
      }
  }

  object I18nFilter extends Filter {
    override def apply(next: RequestHeader => Result)(request: RequestHeader): Result =
      langByQueryParam(request).foldLeft(next(request)) {
        (res: Result, lang: Lang) => res.withLang(lang)
      }
  }
}
