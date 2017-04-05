package org.tksfz.doto.main

import java.nio.file.Paths

import org.tksfz.doto.repo.Project
import org.tksfz.doto._
import org.tksfz.doto.util.handy._

object ListCmdExec extends CmdExec[ListCmd] {
  override def execute(c: Config, cmd: ListCmd): Unit = WithActiveProject { repo =>
    val thread =
      (!cmd.ignoreFocus).thenSome {
        repo.unsynced.getSingleton[Id]("focus") map { focusId =>
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
abstract class Printer(repo: Project, val sb: StringBuilder = new StringBuilder) {
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
class DefaultPrinter(project: Project, thread: Thread[_ <: Work]) extends Printer(project) {
  override lazy val get = {
    printThread(0, thread)
    sb.toString
  }

  private[this] def indent(depth: Int) = " " * (depth * 2)

  private[this] def printLineItem(depth: Int, item: Node[_], icon: String, color: String): Unit = {
    val shortId = item.id.toString.substring(0, 6)
    if (allowAnsi) sb.append(Console.RESET)
    sb.append(shortId + indent(depth) + " ")
    if (allowAnsi) sb.append(color)
    sb.append(icon + " " + item.subject + "\n")
  }

  private[this] def printThread(depth: Int, thread: Thread[_ <: Work]): Unit = {
    val icon = thread.`type`.apply.threadIcon
    printLineItem(depth, thread, icon, Console.BLUE + Console.BOLD)
    thread.`type`.apply match {
      case TaskWorkType =>
        val tasks = project.tasks.findByIds(thread.children.toIds)
        val tasksByTarget = tasks.groupBy(_.target.map(_.id))
        val plannedTasksByTarget = tasksByTarget.collect({ case (Some(eventRef), tasks) => eventRef -> tasks })
        val sortedTargets = project.events.findByIds(plannedTasksByTarget.keys.toSeq).sorted(project.eventsOrdering)
        for(event <- sortedTargets) {
          printEventWithTasks(sb, depth + 1, event, plannedTasksByTarget(event.id))
        }
        val unplannedTasks = tasksByTarget.getOrElse(None, Nil)
        for(task <- unplannedTasks) {
          printTask(sb, depth + 1, task)
        }
      case EventWorkType =>
        for(event <- project.events.findByIds(thread.children.toIds)) {
          printEvent(sb, depth + 1, event)
        }
    }
    for(subthread <- project.findSubThreads(thread.id)) {
      printThread(depth + 1, subthread)
    }
  }

  private[this] def printTask(sb: StringBuilder, depth: Int, task: Task): Unit = {
    val icon = if (task.completed) "[x]" else "[ ]"
    val color = if (task.completed) Console.GREEN else (Console.GREEN + Console.BOLD)
    printLineItem(depth, task, icon, color)
    for(subtask <- project.tasks.findByIds(task.children.toIds)) {
      printTask(sb, depth + 1, subtask)
    }
  }

  private[this] def printEvent(sb: StringBuilder, depth: Int, event: Event): Unit = {
    printEventWithTasks(sb, depth, event, project.tasks.findByIds(event.children.toIds))
  }

  private[this] def printEventWithTasks(sb: StringBuilder, depth: Int, event: Event, tasks: Seq[Task]): Unit = {
    val icon = if (event.completed) "![x]" else "![ ]"
    val color = if (event.completed) Console.YELLOW else (Console.YELLOW + Console.BOLD)
    printLineItem(depth, event, icon, color)
    for(subtask <- tasks) {
      printTask(sb, depth + 1, subtask)
    }
  }

}