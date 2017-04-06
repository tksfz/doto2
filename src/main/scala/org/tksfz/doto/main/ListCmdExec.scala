package org.tksfz.doto.main

import java.nio.file.Paths

import org.tksfz.doto.project.Project
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

case class TaskPath(path: Seq[Node[_]], task: Task) {
  def pathString(allowAnsi: Boolean) = {
    val color = if (allowAnsi) Console.BLUE + Console.BOLD else ""
    color + "(" + path.map { _ match {
        case thread: Thread[_] => s"${thread.icon} ${thread.subject}"
      }
    }.mkString(" > ") + ") "
  }
}

// List all threads and tasks hierarchically
// Within a thread:
//   - print tasks grouped by event
//   - print untargetted tasks
//   - print sub-threads and recurse
class DefaultPrinter(project: Project, thread: Thread[_ <: Work]) extends Printer(project) {
  override lazy val get = {
    printLineItem(0, thread, thread.icon, Console.BLUE + Console.BOLD)
    printPlanned()
    printUnplanned()
    sb.toString
  }

  private[this] def printPlanned(): Unit = {
    thread.asTaskThread foreach { t =>
      if (allowAnsi) sb.append(Console.RESET)
      sb.append("Planned:\n")
      val tasks = findAllTaskPaths(t, Nil)
      val tasksByEvent = tasks.groupBy(_.task.target.map(_.id))
      val plannedTasksByEvent = tasksByEvent.collect({ case (Some(eventRef), tasks) => eventRef -> tasks })
      val sortedEvents = project.events.findByIds(plannedTasksByEvent.keys.toSeq).sorted(project.eventsOrdering)
      for(event <- sortedEvents) {
        if (!isEventPlanTotallyDone(event, plannedTasksByEvent(event.id).map(_.task))) {
          printEventWithTaskPaths(1, event, plannedTasksByEvent(event.id))
        }
      }
      sb.append("\n")
    }
  }

  /**
    * Displays the straightforward hierarchy style,
    * simply omitting all planned tasks
    */
  private[this] def printUnplanned() = {
    if (allowAnsi) sb.append(Console.RESET)
    sb.append("Unplanned:\n")
    printThread(0, thread, true)
  }

  private[this] def indent(depth: Int) = " " * (depth * 2)

  private[this] def printLineItem(depth: Int, item: Node[_], icon: String, color: String, prefix: String = ""): Unit = {
    val shortId = item.id.toString.substring(0, 6)
    if (allowAnsi) sb.append(Console.RESET)
    sb.append(shortId + indent(depth) + " ")
    if (allowAnsi) sb.append(color)
    sb.append(icon + " " + prefix)
    if (allowAnsi) sb.append(color)
    sb.append(item.subject + "\n")
  }

  private[this] def printThread(depth: Int, thread: Thread[_ <: Work], omitThreadLineItem: Boolean = false): Unit = {
    if (!omitThreadLineItem) {
      val icon = thread.workType.threadIcon
      printLineItem(depth, thread, icon, Console.BLUE + Console.BOLD)
    }
    thread.workType match {
      case TaskWorkType =>
        val tasks = project.tasks.findByIds(thread.children.toIds)
        val unplannedTasks = tasks.filter(!_.isPlanned)
        for(task <- unplannedTasks) {
          printTask(depth + 1, task)
        }
      case EventWorkType =>
        for(event <- project.events.findByIds(thread.children.toIds)) {
          if (!isNodeTotallyDone(event)) {
            printEvent(depth + 1, event)
          }
        }
    }
    for(subthread <- project.findSubThreads(thread.id)) {
      printThread(depth + 1, subthread)
    }
  }

  /**
    * Take an event and its subtasks, all tasks planned for that event, and all their
    * subtasks. Are they all marked as completed?
    *
    * This is a fairly stringent condition, and we may well want to relax it in
    * the future, but that requires more thought.
    */
  private[this] def isEventPlanTotallyDone(event: Event, plannedTasks: Seq[Task]) = {
    isNodeTotallyDone(event) && plannedTasks.forall(isNodeTotallyDone)
  }

  // TODO: move this to a place for common Project-dependent functions
  private[this] def isNodeTotallyDone(node: Node[Task]): Boolean= {
    node.completed && project.tasks.findByRefs(node.children).forall(isNodeTotallyDone)
  }

  // what about event threads?
  private[this] def findAllTaskPaths(t: Thread[Task], prefix: Seq[Node[_]]): Seq[TaskPath] = {
    val myTaskPaths = findAllTasks(t.children).map(t => TaskPath(prefix, t))
    val subthreadsTaskPaths =
      project.findSubThreads(t.id)
        .collect { case TaskThread(tt) => tt }
        .flatMap(tt => findAllTaskPaths(tt, prefix :+ tt))
    myTaskPaths ++ subthreadsTaskPaths
  }

  private[this] def findAllTasks(xs: List[Ref[Task]]): Seq[Task] = {
    val tasks = project.tasks.findByRefs(xs)
    val recurse = tasks.flatMap(c => findAllTasks(c.children))
    tasks ++ recurse
  }

  private[this] def printTask(depth: Int, task: Task, prefix: String = ""): Unit = {
    val icon = if (task.completed) "[x]" else "[ ]"
    val color = if (task.completed) Console.GREEN else (Console.GREEN + Console.BOLD)
    printLineItem(depth, task, icon, color, prefix)
    val subtasks = project.tasks.findByRefs(task.children)
    subtasks
      .filter(!_.isPlanned) // BUGBUG: printTask() is invoked for planned section too
      .foreach(printTask(depth + 1, _))
  }

  private[this] def printTaskPath(depth: Int, taskPath: TaskPath): Unit = {
    val prefix = taskPath.pathString(allowAnsi)
    printTask(depth, taskPath.task, prefix)
  }

  private[this] def printEvent(depth: Int, event: Event): Unit = {
    printEventLineItem(depth, event)
    for(subtask <- project.tasks.findByRefs(event.children)) {
      printTask(depth + 1, subtask)
    }
  }

  private[this] def printEventLineItem(depth: Int, event: Event) = {
    val icon = if (event.completed) "![x]" else "![ ]"
    val color = if (event.completed) Console.YELLOW else (Console.YELLOW + Console.BOLD)
    printLineItem(depth, event, icon, color)
  }

  private[this] def printEventWithTaskPaths(depth: Int, event: Event, taskPaths: Seq[TaskPath]): Unit = {
    printEventLineItem(depth, event)
    for(subtask <- taskPaths) {
      printTaskPath(depth + 1, subtask)
    }
  }
}