import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "wahlprogrammometer"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    "com.google.gdata" % "core" % "1.47.1",
    "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
    "eu.henkelmann" % "actuarius_2.10.0" % "0.2.6"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
