enablePlugins(JavaAppPackaging)

name := "doto"

scalaVersion := "2.12.1"

libraryDependencies += "com.github.scopt" %% "scopt" % "3.5.0"

libraryDependencies += "com.github.pathikrit" %% "better-files" % "2.17.1"

val circeVersion = "0.7.0"
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-java8"
).map(_ % circeVersion)

libraryDependencies += "io.circe" %% "circe-yaml" % "0.5.0"
