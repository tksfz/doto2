package org.tksfz.doto.main

import java.nio.file.Paths

import org.tksfz.doto.repo.Repo

/**
  * Created by thom on 3/15/17.
  */
object CompleteCmdExec extends CmdExec[Complete] {
  override def execute(c: Config, cmd: Complete): Unit = {
    val repo = new Repo(Paths.get(""))
    repo.findTaskOrEventByIdPrefix(cmd.id) map { task =>
      val newTask = task.withCompleted(true)
      repo.dynamicPut(newTask)
    } getOrElse {
      println("Couldn't find task or event with id starting with '" + cmd.id + "'")
    }
  }
}
