package org.tksfz.doto.main

import org.tksfz.doto.IdRef

/**
  * Created by thom on 3/15/17.
  */
object PlanCmdExec extends CmdExec[Plan] {
  override def execute(c: Config, cmd: Plan): Unit = WithActiveProjectTxn { project =>
    project.tasks.findByIdPrefix(cmd.taskId) map { task =>
      project.events.findByIdPrefix(cmd.eventId) map { event =>
        val newTask = task.copy(target = Some(IdRef(event.id)))
        project.tasks.put(task.id, newTask)
        project.commitAllIfNonEmpty(c.originalCommandLine)
      }
    }
  }
}