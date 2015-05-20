package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.i18n.Lang
import play.api.Play.current
import play.api.mvc.{Filter, Result, Controller, RequestHeader}

object I18n {
  def langByQueryParam(request: RequestHeader): Option[Lang] = {
    request.getQueryString("lang").map(Lang.apply)
  }

  trait I18nAware extends Controller with HasLogger {
    override implicit def request2lang(implicit request: RequestHeader): Lang =
      langByQueryParam(request) match {
        case None => super.request2lang(request)
        case Some(lang) => lang
      }
  }

  object I18nFilter extends Filter {
    override def apply(next: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] =
      langByQueryParam(request).foldLeft(next(request)) {
        (resf: Future[Result], lang: Lang) => {
          resf.map(res => res.withLang(lang))
        }
      }
  }
}
