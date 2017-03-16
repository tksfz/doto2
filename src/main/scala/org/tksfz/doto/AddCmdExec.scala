package org.tksfz.doto

import java.nio.file.Paths
import java.util.UUID

import org.tksfz.doto.repo.Repo

object AddCmdExec extends CmdExec[Add] {
  override def execute(c: Config, add: Add): Unit = {
    val repo = new Repo(Paths.get(""))
    val parentOpt = repo.threads.findByIdPrefix(add.parentId)
    parentOpt map { parent =>
      val uuid = UUID.randomUUID()
      parent.`type`.apply match {
        case TaskWorkType =>
          val doc = Task(uuid, add.subject)
          val newParent = parent.asInstanceOf[Thread[Task]].copy[Task](children = parent.children.asInstanceOf[List[Ref[Task]]] :+ ValueRef(doc))
          repo.tasks.put(uuid, doc)
          repo.threads.put(newParent.id, newParent)
        case EventWorkType =>
          val doc = Event(uuid, add.subject)
          val newParent = parent.asInstanceOf[Thread[Event]].copy[Event](children = parent.children.asInstanceOf[List[Ref[Event]]] :+ ValueRef(doc))
          repo.events.put(uuid, doc)
          repo.threads.put(newParent.id, newParent)
      }
    } getOrElse {
      println("Could not find parent with id starting with " + add.parentId)
    }
  }
}

