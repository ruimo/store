package functional

import org.specs2.mutable.Specification

import helpers.Helper.downloadBytes
import play.api.Play.current
import play.api.db.DB
import play.api.http.Status
import play.api.test.FakeApplication
import play.api.test.Helpers
import play.api.test.Helpers.inMemoryDatabase
import play.api.test.Helpers.running
import play.api.test.TestServer
import java.nio.file.Files

class ItemPicturesWithoutTempSpec extends Specification {
  val dir = Files.createTempDirectory(null)
  lazy val withTempDir = Map("item.picture.path" -> dir.toFile.getAbsolutePath)

  "ItemPicture" should {
    "If specified picture is not found, 'notfound.jpg' will be returned." in {
      val app = FakeApplication(additionalConfiguration = inMemoryDatabase() ++ withTempDir)
      running(TestServer(3333, app), Helpers.HTMLUNIT) { browser =>
        DB.withConnection { implicit conn =>
          downloadBytes(Some(-1),
            "http://localhost:3333" + controllers.routes.ItemPictures.getPicture(1, 0).url)._1 === Status.OK
        }
      }
    }
  }
}

