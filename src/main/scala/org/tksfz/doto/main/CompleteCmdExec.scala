package org.tksfz.doto.main

import java.nio.file.Paths

import org.tksfz.doto.repo.Repo

/**
  * Created by thom on 3/15/17.
  */
object CompleteCmdExec extends CmdExec[Complete] {
  override def execute(c: Config, cmd: Complete): Unit = {
    val repo = new Repo(Paths.get(""))
    // TODO: how do we abstract this code so that it can operate over
    // both tasks and events?
    repo.tasks.findByIdPrefix(cmd.id) map { task =>
      val newTask = task.copy(completed = true)
      repo.tasks.put(task.id, newTask)
    } orElse repo.events.findByIdPrefix(cmd.id).map { event =>
      val newEvent = event.copy(completed = true)
      repo.events.put(event.id, newEvent)
    } getOrElse {
      println("Couldn't find task or event with id starting with '" + cmd.id + "'")
    }
  }
}
