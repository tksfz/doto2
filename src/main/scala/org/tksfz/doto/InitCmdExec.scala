package org.tksfz.doto

import java.io.File
import java.util.UUID

import better.files.{File => ScalaFile, _}
import org.tksfz.doto.repo.Repo

/**
  * Created by thom on 3/14/17.
  */
object InitCmdExec extends CmdExec[Init] {
  override def execute(c: Config, init: Init): Unit = {
    val location = ScalaFile(init.location.getOrElse(new File(".")).toPath)

    Cmds.mkdir(location / "threads")
    Cmds.mkdir(location / "tasks")

    val rootId = UUID.randomUUID()
    val root = Thread[Task](rootId, None, "root")

    val repo = new Repo(location.toJava.toPath)
    repo.threads.put(rootId, root)

    val rootFile = location / "root"
    rootFile.overwrite(rootId.toString)
  }
}
