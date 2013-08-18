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
    "com.google.gdata" % "core" % "1.47.1"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
