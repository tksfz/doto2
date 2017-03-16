package org.tksfz.doto.main

import java.nio.file.Paths
import java.util.UUID

import org.tksfz.doto.repo.Repo
import org.tksfz.doto.{Event, EventWorkType, Ref, Task, TaskWorkType, Thread, ValueRef}

object AddCmdExec extends CmdExec[Add] {
  override def execute(c: Config, add: Add): Unit = {
    val uuid = UUID.randomUUID()
    val repo = new Repo(Paths.get(""))
    repo.threads.findByIdPrefix(add.parentId) map { parentThread =>
      parentThread.`type`.apply match {
        case TaskWorkType =>
          val doc = Task(uuid, add.subject)
          val newParent = parentThread.asInstanceOf[Thread[Task]].copy[Task](children = parentThread.children.asInstanceOf[List[Ref[Task]]] :+ ValueRef(doc))
          repo.tasks.put(uuid, doc)
          repo.threads.put(newParent.id, newParent)
        case EventWorkType =>
          val doc = Event(uuid, add.subject)
          val newParent = parentThread.asInstanceOf[Thread[Event]].copy[Event](children = parentThread.children.asInstanceOf[List[Ref[Event]]] :+ ValueRef(doc))
          repo.events.put(uuid, doc)
          repo.threads.put(newParent.id, newParent)
      }
    } orElse repo.tasks.findByIdPrefix(add.parentId).map { parentTask =>
      val doc = Task(uuid, add.subject)
      val newParent = parentTask.copy(children = parentTask.children :+ ValueRef(doc))
      repo.tasks.put(uuid, doc)
      repo.tasks.put(newParent.id, newParent)
    } getOrElse {
      println("Could not find parent with id starting with " + add.parentId)
    }
  }
}

