package models

import play.api.db.DB
import anorm._
import play.api.Play.current

object TestHelper {
  def removePreloadedRecords() {
    DB.withConnection { implicit conn =>
      SQL("delete from item_numeric_metadata").executeUpdate()
      SQL("delete from item_description").executeUpdate()
      SQL("delete from site_item").executeUpdate()
      SQL("delete from item_name").executeUpdate()
      SQL("delete from item").executeUpdate()
      SQL("delete from category_name").executeUpdate()
      SQL("delete from category_path").executeUpdate()
      SQL("delete from site_category").executeUpdate()
      SQL("delete from category").executeUpdate()
      SQL("delete from site").executeUpdate()
      SQL("delete from tax").executeUpdate()
    }
  }
}
