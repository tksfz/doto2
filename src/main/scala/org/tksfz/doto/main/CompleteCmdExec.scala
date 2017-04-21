package org.tksfz.doto.main

/**
  * Created by thom on 3/15/17.
  */
object CompleteCmdExec extends CmdExec[Complete] {
  override def execute(c: Config, cmd: Complete): Unit = WithActiveProjectTxn { project =>
    project.findTaskOrEventByIdPrefix(cmd.id) map { taskOrEvent =>
      val newTask = taskOrEvent.withCompleted(true)
      project.dynamicPut(newTask)
      project.commitAllIfNonEmpty(c.originalCommandLine)
    } getOrElse {
      println("Couldn't find task or event with id starting with '" + cmd.id + "'")
    }
  }
}
