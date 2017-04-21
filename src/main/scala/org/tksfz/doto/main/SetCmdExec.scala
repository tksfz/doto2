package org.tksfz.doto.main

import org.tksfz.doto.{Event, IdRef, Task, Thread, Work, WorkTypeClass}
import org.tksfz.doto.project.Project

/**
  * Created by thom on 3/15/17.
  */
object SetCmdExec extends CmdExec[Set] {
  override def execute(c: Config, cmd: Set): Unit = WithActiveProjectTxn { project =>
    project.findNodeByIdPrefix(cmd.id) map { node =>
      cmd.newSubject foreach { newSubject =>
        val newTask = node.withSubject(newSubject)
        project.dynamicPut(newTask)
      }

      // Each type of object has its own slightly different move logic
      cmd.newParent foreach { p =>
        node match {
          case task: Task => moveTask(project, task, p)
          case thread: Thread[_] => moveThread(project, thread, p)
          case event: Event => println("Can't move events yet")
        }
      }

      project.commitAllIfNonEmpty(c.originalCommandLine)
    } getOrElse {
      println("Couldn't find node with id starting with '" + cmd.id + "'")
    }
  }

  private[this] def moveTask(project: Project, task: Task, newParentId: String) = {
    // new and old parent could both be either threads or tasks
    project.findTaskOrTaskThreadByIdPrefix(newParentId) map { newParent =>
      // TODO: local indexing
      project.findTaskParent(task.id) map { oldParent =>
        val oldParentModified = oldParent.modifyChildren(_.filterNot(_.id == task.id))
        val newParentModified = newParent.modifyChildren(_ :+ IdRef[Task](task.id))
        project.dynamicPut(newParentModified)
        project.dynamicPut(oldParentModified)
      }
    }
  }
  
  private[this] def moveThread[T <: Work](project: Project, thread: Thread[T], newParentId: String) = {
    // just validate that the parent exists
    project.threads.findByIdPrefix(newParentId) map { newParent =>
      val newThread = thread.copy(parent = Some(IdRef[Thread[_]](newParent.id)))(thread.`type`)
      project.threads.put(newThread.id, newThread)
    }
  }
}
