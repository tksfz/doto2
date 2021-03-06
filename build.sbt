enablePlugins(JavaAppPackaging)

name := "doto"

scalaVersion := "2.12.3"

mainClass in assembly := Some("org.tksfz.doto.main.Main")

libraryDependencies += "com.github.scopt" %% "scopt" % "3.5.0"

libraryDependencies += "com.github.pathikrit" %% "better-files" % "3.4.0"

val circeVersion = "0.7.0"
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-java8" // needed for date/time
).map(_ % circeVersion)

libraryDependencies ++= Seq(
  "io.circe" %% "circe-yaml" % "0.5.0",
  "org.eclipse.jgit" % "org.eclipse.jgit" % "4.6.1.201703071140-r" exclude("org.eclipse.jgit", "org.eclipse.jgit"),
  "org.slf4j" % "slf4j-nop" % "1.7.2",
  "com.propensive" %% "magnolia" % "0.8.0"
)

// sbt-github-release

ghreleaseRepoOrg := "tksfz"

ghreleaseRepoName := "doto2"

