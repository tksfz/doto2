package org.tksfz.doto.main

import java.util.UUID

import org.tksfz.doto._

object AddCmdExec extends CmdExec[Add] {
  override def execute(c: Config, add: Add): Unit = WithActiveProjectTxn { project =>
    val uuid = UUID.randomUUID()
    val result =
      project.threads.findByIdPrefix(add.parentId) map { parentThread =>
        val doc =
          parentThread match {
            case TaskThread(tt) =>
              val doc = Task(uuid, add.subject)
              val newParent = tt.copy(children = tt.children :+ ValueRef(doc))
              project.tasks.put(uuid, doc)
              project.threads.put(newParent.id, newParent)
              doc
            case EventThread(et) =>
              val doc = Event(uuid, add.subject)
              val newParent = et.copy(children = et.children :+ ValueRef(doc))
              project.events.put(uuid, doc)
              project.threads.put(newParent.id, newParent)
              doc
          }
        if (project.commitAllIfNonEmpty(c.originalCommandLine)) Right(doc) else Left("Nothing to commit")
      } orElse project.tasks.findByIdPrefix(add.parentId).map { parentTask =>
        val doc = Task(uuid, add.subject)
        val newParent = parentTask.copy(children = parentTask.children :+ ValueRef(doc))
        project.tasks.put(uuid, doc)
        project.tasks.put(newParent.id, newParent)
        if (project.commitAllIfNonEmpty(c.originalCommandLine)) Right(doc) else Left("Nothing to commit")
      } getOrElse {
        Left("Could not find parent with id starting with " + add.parentId)
      }
    val output =
      result.map { doc =>
        val sb = new StringBuilder
        val printer = new DefaultPrinter(project, sb)
        doc match {
          case task: Task => printer.printTaskLineItem(0, task)
          case event: Event => printer.printEventLineItem(0, event)
        }
        sb.toString
      }
    println(output.merge.stripLineEnd)
  }
}

