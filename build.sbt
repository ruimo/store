name := """store"""

version := "1.1-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

sbt.Keys.fork in Test := false

resolvers += "ruimo.com" at "http://static.ruimo.com/release"

// For IntelliJ hack.
unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )

libraryDependencies ++= Seq(
  "postgresql" % "postgresql" % "9.1-901.jdbc4",
  "com.typesafe.play" %% "play-mailer" % "2.4.1",
  "org.mockito" % "mockito-all" % "1.9.5" % "test",
  "com.ruimo" %% "recoengplugin4play23" % "1.0-SNAPSHOT",
  "com.ruimo" %% "csvparser" % "1.0",
  "com.ruimo" %% "scoins" % "1.6",
  "org.twitter4j" % "twitter4j-core" % "4.0.4",
  "org.seleniumhq.selenium" % "selenium-java" % "2.52.0" % "test",
  jdbc,
//  anorm,
  "com.typesafe.play" %% "anorm" % "2.4.0",
  cache,
  ws,
  filters
)

