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
  "io.circe" %% "circe-java8" // needed for date/time
).map(_ % circeVersion)

libraryDependencies += "io.circe" %% "circe-yaml" % "0.5.0"

libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "4.6.1.201703071140-r"

libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.2"
