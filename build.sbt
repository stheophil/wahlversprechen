import play.Project._

name := "wahlversprechen2013"

version := "1.0"

libraryDependencies ++= Seq(
  jdbc, 
  anorm, 
  cache,
  "com.google.gdata" % "core" % "1.47.1",
  "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
  "eu.henkelmann" % "actuarius_2.10.0" % "0.2.6"
)

playScalaSettings