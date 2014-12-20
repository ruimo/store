// Comment to get more information during initialization
logLevel := Level.Warn

resolvers += "play with latest fluentlenium" at "https://www.ruimo.com/jenkins/view/store/job/play-2.1.3-with-latest-fluentlenium/ws/repository/local"

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("play" % "sbt-plugin" % "2.1.3")

resolvers += Classpaths.typesafeResolver

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")
