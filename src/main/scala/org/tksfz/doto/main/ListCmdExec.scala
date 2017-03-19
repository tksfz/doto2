package org.tksfz.doto.main

import java.nio.file.Paths

import org.tksfz.doto.repo.Repo
import org.tksfz.doto._
import org.tksfz.doto.util.handy._

object ListCmdExec extends CmdExec[ListCmd] {
  override def execute(c: Config, cmd: ListCmd): Unit = {
    val repo = new Repo(Paths.get(""))
    val thread =
      (!cmd.ignoreFocus).thenSome {
        repo.getSingleton[Id]("focus") map { focusId =>
          repo.threads.get(focusId).toTry.get
        }
      }.flatten.getOrElse {
        repo.rootThread
      }
    print(new DefaultPrinter(repo, thread).get)
  }
}



/**
  * This class just lets us put `repo` and `sb` into scope so that every method doesn't need
  * to declare them
  */
abstract class Printer(repo: Repo, val sb: StringBuilder = new StringBuilder) {
  def get: String
}

// List all threads and tasks hierarchically
// Within a thread:
//   - print tasks grouped by event
//   - print untargetted tasks
//   - print sub-threads and recurse
class DefaultPrinter(repo: Repo, thread: Thread[_ <: Work]) extends Printer(repo) {
  override lazy val get = {
    printThread(0, thread)
    sb.toString
  }

  private[this] def indent(depth: Int) = " " * (depth * 2)

  private[this] def printThread(depth: Int, thread: Thread[_ <: Work]): Unit = {
    sb.append(indent(depth))
    val icon = thread.`type`.apply.threadIcon
    sb.append(icon + " " + thread.id.toString.substring(0, 6) + " " + thread.subject + "\n")
    thread.`type`.apply match {
      case TaskWorkType =>
        val tasks = repo.tasks.findByIds(thread.children.toIds)
        val tasksByTarget = tasks.groupBy(_.target.map(_.id))
        val plannedTasksByTarget = tasksByTarget.collect({ case (Some(eventRef), tasks) => eventRef -> tasks })
        val sortedTargets = repo.events.findByIds(plannedTasksByTarget.keys.toSeq).sorted(repo.eventsOrdering)
        for(event <- sortedTargets) {
          printEventWithTasks(sb, depth + 1, event, plannedTasksByTarget(event.id))
        }
        val unplannedTasks = tasksByTarget.getOrElse(None, Nil)
        for(task <- unplannedTasks) {
          printTask(sb, depth + 1, task)
        }
      case EventWorkType =>
        for(event <- repo.events.findByIds(thread.children.toIds)) {
          printEvent(sb, depth + 1, event)
        }
    }
    for(subthread <- repo.findSubThreads(thread.id)) {
      printThread(depth + 1, subthread)
    }
  }

  private[this] def printTask(sb: StringBuilder, depth: Int, task: Task): Unit = {
    sb.append(" " * (depth * 2))
    val check = if (task.completed) "x" else " "
    sb.append("[" + check + "] " + task.id.toString.substring(0, 6) + " " + task.subject + "\n")
    for(subtask <- repo.tasks.findByIds(task.children.toIds)) {
      printTask(sb, depth + 1, subtask)
    }
  }

  private[this] def printEvent(sb: StringBuilder, depth: Int, event: Event): Unit = {
    printEventWithTasks(sb, depth, event, repo.tasks.findByIds(event.children.toIds))
  }

  private[this] def printEventWithTasks(sb: StringBuilder, depth: Int, event: Event, tasks: Seq[Task]): Unit = {
    sb.append(" " * (depth * 2))
    val check = if (event.completed) "x" else " "
    sb.append("![" + check + "] " + event.id.toString.substring(0, 6) + " " + event.subject + "\n")
    for(subtask <- tasks) {
      printTask(sb, depth + 1, subtask)
    }
  }

}