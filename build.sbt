import play.Project._

lazy val relatedtexts = project.in(file("modules/relatedtexts"))

lazy val wahlversprechen2013 = project.in(file("."))
    .aggregate(relatedtexts)
    .dependsOn(relatedtexts)

name := "wahlversprechen2013"

version := "1.0"

libraryDependencies ++= Seq(
	// Add your project dependencies here,
    jdbc,
    anorm,
    cache,
    filters,
    "com.google.gdata" % "core" % "1.47.1",
    "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
    "eu.henkelmann" % "actuarius_2.10.0" % "0.2.6",
    "com.github.spullara.mustache.java" % "compiler" % "0.8.16",
    "com.amazonaws" % "aws-java-sdk-s3" % "1.9.3"
)

playScalaSettings

routesImport ++= Seq("binders.DateBinder._")

lessEntryPoints <<= baseDirectory(_ / "app" / "assets" / "stylesheets" ** "main.less")
