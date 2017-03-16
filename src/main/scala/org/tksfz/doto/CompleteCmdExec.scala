package org.tksfz.doto

import java.nio.file.Paths

import org.tksfz.doto.repo.Repo

/**
  * Created by thom on 3/15/17.
  */
object CompleteCmdExec extends CmdExec[Complete] {
  override def execute(c: Config, cmd: Complete): Unit = {
    val repo = new Repo(Paths.get(""))
    repo.tasks.findByIdPrefix(cmd.id) map { task =>
      val newTask = task.copy(completed = true)
      repo.tasks.put(task.id, newTask)
    } getOrElse {
      println("Couldn't find task with id starting with '" + cmd.id + "'")
    }
  }
}
