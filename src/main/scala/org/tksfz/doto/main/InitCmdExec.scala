package org.tksfz.doto.main

import java.io.File
import java.util.UUID

import better.files.{File => ScalaFile, _}
import org.tksfz.doto._
import org.tksfz.doto.repo.Repo

/**
  * Created by thom on 3/14/17.
  */
object InitCmdExec extends CmdExec[Init] {
  override def execute(c: Config, init: Init): Unit = {
    val location = ScalaFile(init.location.getOrElse(new File(".")).toPath)

    val syncedRoot = location / "synced"
    Cmds.mkdir(syncedRoot / "threads")
    Cmds.mkdir(syncedRoot / "tasks")
    Cmds.mkdir(syncedRoot / "events")

    val rootId = UUID.randomUUID()
    val root = Thread[Task](rootId, None, "root")

    val repo = new Repo(location.toJava.toPath)
    repo.threads.put(rootId, root)

    val rootFile = syncedRoot / "root"
    rootFile.overwrite(rootId.toString)
  }
}
