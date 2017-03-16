package org.tksfz.doto.main

import java.nio.file.Paths

import org.tksfz.doto.repo.Repo

/**
  * Created by thom on 3/15/17.
  */
object SetCmdExec extends CmdExec[Set] {
  override def execute(c: Config, cmd: Set): Unit = {
    val repo = new Repo(Paths.get(""))
    repo.tasks.findByIdPrefix(cmd.id) map { task =>
      val newTask = task.copy(subject = cmd.newSubject)
      repo.tasks.put(task.id, newTask)
    } getOrElse {
      println("Couldn't find task with id starting with '" + cmd.id + "'")
    }
  }
}
