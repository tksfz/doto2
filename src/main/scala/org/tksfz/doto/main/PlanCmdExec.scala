package org.tksfz.doto.main

import org.tksfz.doto.project.Project
import org.tksfz.doto.model.{EventTarget, IdRef, Never}

/**
  * Created by thom on 3/15/17.
  */
object PlanCmdExec extends CmdExec[Plan] {
  override def execute(c: Config, cmd: Plan): Unit = WithActiveProjectTxn { project =>
    findTarget(project, cmd.eventId) map { target =>
      cmd.taskIds map { taskId =>
        project.tasks.findByIdPrefix(taskId) map { task =>
            val newTask = task.copy(target = Some(target))
            project.tasks.put(task.id, newTask)
            project.commitAllIfNonEmpty(c.originalCommandLine)
          }
        }
    }
  }

  private[this] def findTarget(project: Project, eventId: String) = eventId match {
    case "never" => Some(Never)
    case _ => project.events.findByIdPrefix(eventId).map(e => EventTarget(IdRef(e.id)))
  }
}