// change to File
val nativeImage = taskKey[Unit]("Build native-image binary")

nativeImage := {
  val graalHome=sys.env.get("GRAAL_HOME").getOrElse(sys.error("GRAAL_HOME environment variable must be defined"))
  import sys.process._
  val exitCode = s"$graalHome/bin/native-image -jar ${assembly.value.toString} -H:+ReportUnsupportedElementsAtRuntime -H:ReflectionConfigurationFiles=project/jgit-graal-reflectconfig.json".!
  if (exitCode == 0) {
  } else {
    sys.error("native-image failed")
  }
}
