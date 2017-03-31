package org.tksfz.doto.main

import java.nio.file.Paths

import org.tksfz.doto.IdRef
import org.tksfz.doto.repo.Repo

/**
  * Created by thom on 3/15/17.
  */
object PlanCmdExec extends CmdExec[Plan] {
  override def execute(c: Config, cmd: Plan): Unit = CommandWithActiveProject { repo =>
    repo.tasks.findByIdPrefix(cmd.taskId) map { task =>
      repo.events.findByIdPrefix(cmd.eventId) map { event =>
        val newTask = task.copy(target = Some(IdRef(event.id)))
        repo.tasks.put(task.id, newTask)
      }
    }
  }
}