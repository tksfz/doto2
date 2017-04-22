package org.tksfz.doto.main

import org.tksfz.doto.{Event, EventThread, HasChildren, HasId, IdRef, Node, Ref, Task, TaskThread, Thread, Work, WorkTypeClass}
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
          case task: Task => moveTaskOrEvent(project, task, p)
          case thread: Thread[_] => moveThread(project, thread, p)
          case event: Event => moveTaskOrEvent(project, event, p)
        }
      }

      project.commitAllIfNonEmpty(c.originalCommandLine)
    } getOrElse {
      println("Couldn't find node with id starting with '" + cmd.id + "'")
    }
  }

  private[this] def moveTaskOrEvent(project: Project, node: Node[_ <: HasId], newParentId: String) = {
    // new and old parent could both be either threads or tasks
    project.findNodeByIdPrefix(newParentId) flatMap { newParent =>
      // TODO: local indexing
      project.findParent(node.id) flatMap { oldParent =>
        val oldParentModified = oldParent.modifyChildren(_.filterNot(_.id == node.id))
        val newParentModifiedOpt = (newParent, node) match {
          case (parentTask: Task, t: Task) => Some(parentTask.modifyChildren(_ :+ IdRef[Task](node.id)))
          case (parentEvent: Event, t: Task) => Some(parentEvent.modifyChildren(_ :+ IdRef[Task](node.id)))
          case (EventThread(et), e: Event) => Some(et.modifyChildren(_ :+ IdRef[Event](node.id)))
          case (TaskThread(tt), t: Task) => Some(tt.modifyChildren(_ :+ IdRef[Task](node.id)))
          case (_, e: Event) =>
            println("Events can only be parented to event threads")
            None
          case (EventThread(et), t: Task) =>
            println("Tasks cannot be parented to event threads")
            None
        }
        newParentModifiedOpt map { newParentModified =>
          project.dynamicPut(newParentModified)
          project.dynamicPut(oldParentModified)
        }
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
