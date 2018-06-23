// change to File
val nativeImage = taskKey[File]("Build native-image binary")

nativeImage := {
  val graalHome = sys.env.get("GRAAL_HOME").getOrElse(sys.error("GRAAL_HOME environment variable must be defined"))
  val assemblyParent = assembly.value.getParentFile
  import sys.process._
  val cmd =
    s"$graalHome/bin/native-image -jar ${assembly.value} -H:+ReportUnsupportedElementsAtRuntime " +
      s"-H:ReflectionConfigurationFiles=${baseDirectory.value}/project/jgit-graal-reflectconfig.json " +
      s"-H:Path=$assemblyParent"
  println(s"Building binary with GraalVM's native-image: $cmd")
  val exitCode = cmd.!
  if (exitCode == 0) {
    new File(assemblyParent, "doto")
  } else {
    sys.error("native-image failed")
  }
}
