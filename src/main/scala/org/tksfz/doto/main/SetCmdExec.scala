package org.tksfz.doto.main

import java.nio.file.Paths

import org.tksfz.doto.{IdRef, Task}
import org.tksfz.doto.repo.Project

/**
  * Created by thom on 3/15/17.
  */
object SetCmdExec extends CmdExec[Set] {
  override def execute(c: Config, cmd: Set): Unit = WithActiveProjectTxn { repo =>
    repo.tasks.findByIdPrefix(cmd.id) map { task =>
      cmd.newSubject foreach { newSubject =>
        val newTask = task.copy(subject = newSubject)
        repo.put(newTask)
      }

      cmd.newParent foreach { move(repo, task, _) }
      repo.commitAllIfNonEmpty(c.originalCommandLine)
    } getOrElse {
      println("Couldn't find task with id starting with '" + cmd.id + "'")
    }
  }

  private[this] def move(repo: Project, task: Task, newParentId: String) = {
    // new and old parent could both be either threads or tasks
    repo.findTaskOrTaskThreadByIdPrefix(newParentId) map { newParent =>
      // TODO: local indexing
      repo.findTaskParent(task.id) map { oldParent =>
        val oldParentModified = oldParent.modifyChildren(_.filterNot(_.id == task.id))
        val newParentModified = newParent.modifyChildren(_ :+ IdRef[Task](task.id))
        repo.dynamicPut(newParentModified)
        repo.dynamicPut(oldParentModified)
      }
    }
  }
}
