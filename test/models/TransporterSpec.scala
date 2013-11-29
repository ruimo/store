package models

import org.specs2.mutable._

import anorm._
import anorm.{NotAssigned, Pk}
import anorm.SqlParser
import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current
import anorm.Id
import java.util.Locale

class TransporterSpec extends Specification {
  "Transporter" should {
    "Can create new transporter." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => {
          val trans0 = Transporter.createNew
          val trans1 = Transporter.createNew

          val list = Transporter.list
          list.size === 2
          list(0) === trans0
          list(1) === trans1
        }}
      }
    }

    "Can create new transporter with name." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        TestHelper.removePreloadedRecords()

        DB.withConnection { implicit conn => {
          val trans0 = Transporter.createNew
          val transName00 = TransporterName.createNew(
            trans0.id.get, LocaleInfo.Ja, "トマト運輸"
          )
          val transName01 = TransporterName.createNew(
            trans0.id.get, LocaleInfo.En, "Tomato"
          )
          val trans1 = Transporter.createNew
          val transName10 = TransporterName.createNew(
            trans1.id.get, LocaleInfo.En, "Hedex"
          )

          val list = Transporter.tableForDropDown(LocaleInfo.Ja)
          list.size === 1
          list(0) === (trans0.id.get.toString, "トマト運輸")

          val listEn = Transporter.tableForDropDown(LocaleInfo.En)
          listEn.size === 2
          listEn(0) === (trans1.id.get.toString, "Hedex")
          listEn(1) === (trans0.id.get.toString, "Tomato")
        }}
      }
    }
  }
}
