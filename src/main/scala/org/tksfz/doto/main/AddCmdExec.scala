package org.tksfz.doto.main

import java.nio.file.Paths
import java.util.UUID

import org.tksfz.doto.project.Project
import org.tksfz.doto._

object AddCmdExec extends CmdExec[Add] {
  override def execute(c: Config, add: Add): Unit = WithActiveProjectTxn { project =>
    val uuid = UUID.randomUUID()
    project.threads.findByIdPrefix(add.parentId) map { parentThread =>
      parentThread match {
        case TaskThread(tt) =>
          val doc = Task(uuid, add.subject)
          val newParent = tt.copy(children = tt.children :+ ValueRef(doc))
          project.tasks.put(uuid, doc)
          project.threads.put(newParent.id, newParent)
        case EventThread(et) =>
          val doc = Event(uuid, add.subject)
          val newParent = et.copy(children = et.children :+ ValueRef(doc))
          project.events.put(uuid, doc)
          project.threads.put(newParent.id, newParent)
      }
      project.commitAllIfNonEmpty(c.originalCommandLine)
    } orElse project.tasks.findByIdPrefix(add.parentId).map { parentTask =>
      val doc = Task(uuid, add.subject)
      val newParent = parentTask.copy(children = parentTask.children :+ ValueRef(doc))
      project.tasks.put(uuid, doc)
      project.tasks.put(newParent.id, newParent)
      project.commitAllIfNonEmpty(c.originalCommandLine)
    } getOrElse {
      println("Could not find parent with id starting with " + add.parentId)
    }
  }
}

