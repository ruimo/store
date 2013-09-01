package models

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.db.DB
import play.api.Play.current
import anorm.Id
import java.util.Locale
import play.api.i18n.Lang

class LocaleInfoSpec extends Specification {
  "LocaleInfo" should {
    "Japaness locale should exists." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        LocaleInfo(1L).toLocale === new Locale("ja")
        LocaleInfo(2L).toLocale === new Locale("en")
      }
    }

    "registry has all locale." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        val r = LocaleInfo.registry
        r(1L) === LocaleInfo.Ja
        r(2L) === LocaleInfo.En
      }
    }

    "byLang has all locale." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        val byLang = LocaleInfo.byLang
        byLang.size === 2
        byLang(new Lang("ja")) === LocaleInfo.Ja
        byLang(new Lang("en")) === LocaleInfo.En
      }
    }

    "Lang is correctly selected." in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        LocaleInfo.getDefault(new Lang("ja")) === LocaleInfo.Ja
        LocaleInfo.getDefault(new Lang("ja", "JP")) === LocaleInfo.Ja
        LocaleInfo.getDefault(new Lang("fr")) === LocaleInfo.En
      }
    }
  }
}
