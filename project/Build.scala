import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {
  val appName         = "store"
  val appVersion      = "1.0-SNAPSHOT"

  lazy val s = Defaults.defaultSettings ++ Seq(ScctPlugin.instrumentSettings: _*)

  val appDependencies = Seq(
    // Add your project dependencies here,
    "postgresql" % "postgresql" % "9.1-901.jdbc4",
    jdbc,
    anorm,
    filters
  )

  val main = play.Project(appName, appVersion, appDependencies, settings = s).settings(
    scalacOptions ++= Seq("-feature")
  )
}
