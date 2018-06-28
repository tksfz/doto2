// change to File
val nativeImage = taskKey[File]("Build native-image binary")

nativeImage := {
  val graalHome = sys.env.get("GRAAL_HOME").getOrElse(sys.error("GRAAL_HOME environment variable must be defined"))
  val baseDir = baseDirectory.value
  val assemblyParent = assembly.value.getParentFile
  val binaryName = "doto"
  val cachedFun = FileFunction.cached(streams.value.cacheDirectory / "nativeImage", FilesInfo.lastModified) {
    in: Set[File] =>
      import sys.process._
      val cmd =
        s"$graalHome/bin/native-image -jar ${in.head} -H:+ReportUnsupportedElementsAtRuntime " +
          s"-H:ReflectionConfigurationFiles=$baseDir/project/jgit-graal-reflectconfig.json " +
          s"-H:Path=$assemblyParent -H:Name=$binaryName"
      println(s"Building binary with GraalVM's native-image: $cmd")
      val exitCode = cmd.!
      if (exitCode == 0) {
        val f = assemblyParent / binaryName
        println(s"Built native-image binary at $f")
        Set(f)
      } else {
        sys.error("native-image failed")
      }
  }
  cachedFun(Set(assembly.value)).head
}

ghreleaseAssets := Seq(assembly.value, nativeImage.value)