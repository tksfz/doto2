// change to File
val nativeImage = taskKey[Unit]("Build native-image binary")

nativeImage := {
  val graalHome=sys.env.get("GRAAL_HOME").getOrElse(sys.error("GRAAL_HOME environment variable must be defined"))
  import sys.process._
  val cmd = s"$graalHome/bin/native-image -jar ${assembly.value} -H:+ReportUnsupportedElementsAtRuntime " +
    s"-H:ReflectionConfigurationFiles=${baseDirectory.value}/project/jgit-graal-reflectconfig.json"
  println(s"Building binary with GraalVM's native-image: $cmd")
  val exitCode = cmd.!
  if (exitCode == 0) {
  } else {
    sys.error("native-image failed")
  }
}
