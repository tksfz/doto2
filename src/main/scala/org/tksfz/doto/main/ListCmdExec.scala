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

  // If we're being piped or redirected, suppress ansi colors
  // http://stackoverflow.com/questions/1403772/how-can-i-check-if-a-java-programs-input-output-streams-are-connected-to-a-term
  def allowAnsi = System.console() != null
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

  private[this] def printLineItem(depth: Int, item: DrawableNode[_], icon: String, color: String): Unit = {
    val shortId = item.id.toString.substring(0, 6)
    if (allowAnsi) sb.append(Console.RESET)
    sb.append(shortId + indent(depth) + " ")
    if (allowAnsi) sb.append(color)
    sb.append(icon + " " + item.coloredSubject + "\n")
  }

  private[this] def printThread(depth: Int, thread: Thread[_ <: Work]): Unit = {
    val icon = thread.`type`.apply.threadIcon
    printLineItem(depth, thread, icon, Console.BLUE + Console.BOLD)
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
    printLineItem(depth, task, task.coloredIcon, task.iconColor)
    for(subtask <- repo.tasks.findByIds(task.children.toIds)) {
      printTask(sb, depth + 1, subtask)
    }
  }

  private[this] def printEvent(sb: StringBuilder, depth: Int, event: Event): Unit = {
    printEventWithTasks(sb, depth, event, repo.tasks.findByIds(event.children.toIds))
  }

  private[this] def printEventWithTasks(sb: StringBuilder, depth: Int, event: Event, tasks: Seq[Task]): Unit = {
    printLineItem(depth, event, event.coloredIcon, event.iconColor)
    for(subtask <- tasks) {
      printTask(sb, depth + 1, subtask)
    }
  }

}