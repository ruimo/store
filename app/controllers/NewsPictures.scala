package controllers

object NewsPictures Controller with I18nAware with NeedLogin with HasLogger {
  def upload(newsId: Long, no: Int) = Action(parse.multipartFormData) { implicit request =>
    retrieveLoginSession(request) match {
      case None => onUnauthorized(request)
      case Some(user) =>
        if (user.isBuyer) onUnauthorized(request)
        else {
          
        }
    }
  }
}

