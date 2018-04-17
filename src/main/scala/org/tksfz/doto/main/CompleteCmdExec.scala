package org.tksfz.doto.main

/**
  * Created by thom on 3/15/17.
  */
object CompleteCmdExec extends CmdExec[Complete] {
  override def execute(c: Config, cmd: Complete): Unit = WithActiveProjectTxn { project =>
    project.findNodeByIdPrefix(cmd.id) map { node =>
      val newTask = node.withCompleted(true)
      project.dynamicPut(newTask)
      project.commitAllIfNonEmpty(c.originalCommandLine)
    } getOrElse {
      println("Couldn't find task or event or thread with id starting with '" + cmd.id + "'")
    }
  }
}
