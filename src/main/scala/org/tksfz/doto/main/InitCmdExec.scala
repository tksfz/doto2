package org.tksfz.doto.main

import java.io.File
import java.util.UUID

import better.files.{File => ScalaFile, _}
import org.tksfz.doto._
import org.tksfz.doto.repo.Project

/**
  * Created by thom on 3/14/17.
  */
object InitCmdExec extends CmdExec[Init] {
  override def execute(c: Config, cmd: Init): Unit = {
    val location = ScalaFile(cmd.location.getOrElse(new File(".")).toPath)
    init(location)
  }

  def init(location: ScalaFile) = {
    val syncedRoot = location
    Cmds.mkdir(syncedRoot)
    Cmds.mkdir(syncedRoot / "threads")
    Cmds.mkdir(syncedRoot / "tasks")
    Cmds.mkdir(syncedRoot / "events")

    val gitignore = syncedRoot / ".gitignore"
    gitignore.overwrite("local/\n")

    val unsyncedRoot = location / "local"
    Cmds.mkdir(unsyncedRoot)

    val rootId = UUID.randomUUID()
    val root = Thread[Task](rootId, None, "root")

    val repo = new Project(location.toJava.toPath)
    repo.threads.put(rootId, root)

    val rootFile = syncedRoot / "root"
    rootFile.overwrite(rootId.toString)
  }
}
