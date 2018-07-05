package org.tksfz.doto.main

import java.io.File
import java.util.UUID

import better.files.{File => ScalaFile, _}
import better.files.Dsl._
import org.tksfz.doto.model._
import org.tksfz.doto.project.Project

import ModelExtensionsImplicits.HasContentExtensionMethods

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
    mkdirs(syncedRoot)
    mkdir(syncedRoot / "threads")
    mkdir(syncedRoot / "tasks")
    (syncedRoot / "tasks" / ".gitignore").touch() // otherwise a git push/pull round-trip won't recreate the directory
    mkdir(syncedRoot / "events")
    (syncedRoot / "events" / ".gitignore").touch()

    val gitignore = syncedRoot / ".gitignore"
    gitignore.overwrite("local/\n")

    val unsyncedRoot = location / "local"
    mkdir(unsyncedRoot)

    val rootId = UUID.randomUUID()
    val root = Thread[Task](rootId, None).withSubject("root")

    val project = new Project(location.toJava.toPath)
    project.threads.put(rootId, root)

    val rootFile = syncedRoot / "root"
    rootFile.overwrite(rootId.toString)
  }
}
