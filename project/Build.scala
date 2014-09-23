import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {
  val appName         = "store"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    "postgresql" % "postgresql" % "9.1-901.jdbc4",
    "com.typesafe" %% "play-plugins-mailer" % "2.1.0",
    "org.mockito" % "mockito-all" % "1.9.5" % "test",
    "com.ruimo" %% "recoengplugin" % "1.0-SNAPSHOT",
    "com.ruimo" %% "csvparser" % "1.0-SNAPSHOT",
    "com.ruimo" %% "scoins" % "1.0-SNAPSHOT",
    jdbc,
    anorm,
    filters
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    scalacOptions ++= Seq("-feature"),
    javaOptions ++= sys.process.javaVmArguments.filter(
      a => Seq("-Xmx","-Xms","-XX").exists(a.startsWith)
    ),
    scalaVersion := "2.10.4",
    resolvers += "ruimo.com" at "http://www.ruimo.com/release"
  ).settings(
    net.virtualvoid.sbt.graph.Plugin.graphSettings: _*
  )
}
