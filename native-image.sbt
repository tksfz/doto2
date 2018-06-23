// change to File
val nativeImage = taskKey[Unit]("Build native-image binary")

nativeImage := {
  import sys.process._
  val exitCode = s"/Users/tk/Downloads/graalvm-ee-1.0.0-rc2/Contents/Home/bin/native-image -jar ${assembly.value.toString} -H:+ReportUnsupportedElementsAtRuntime -H:ReflectionConfigurationFiles=project/jgit-graal-reflectconfig.json".!
  if (exitCode == 0) {
  } else {
    sys.error("native-image failed")
  }
}
