package org.tksfz.doto.main

import org.tksfz.doto.project.Project
import org.tksfz.doto.model._
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
    print(new DefaultPrinter(repo).print(thread))
  }
}



/**
  * This class just lets us put `repo` and `sb` into scope so that every method doesn't need
  * to declare them
  */
abstract class Printer(repo: Project, val sb: StringBuilder = new StringBuilder) {
  // If we're being piped or redirected, suppress ansi colors
  // http://stackoverflow.com/questions/1403772/how-can-i-check-if-a-java-programs-input-output-streams-are-connected-to-a-term
  def allowAnsi = System.console() != null
}

private case class TaskPath(path: Seq[Node[_]], task: Task) {
  def pathString(allowAnsi: Boolean) = {
    val color = if (allowAnsi) Console.BLUE + Console.BOLD else ""
    color + "(" + path.map { _ match {
        case thread: Thread[_] => s"${thread.icon} ${thread.subject}"
        case task: Task => s"${task.subject}"
        case event: Event => throw new IllegalStateException("displaying task paths with events not yet supported")
      }
    }.mkString(" > ") + ") "
  }
}

// List all threads and tasks hierarchically
// Within a thread:
//   - print tasks grouped by event
//   - print untargetted tasks
//   - print sub-threads and recurse
class DefaultPrinter(project: Project, sb: StringBuilder = new StringBuilder) extends Printer(project) {
  lazy val statuses: Map[Id, Seq[Status]] = project.statuses.findAll.groupBy(_.nodeId)

  def print(thread: Thread[_ <: Work]): String = {
    printLineItem(0, thread, thread.icon, Console.BLUE + Console.BOLD)
    printPlanned(thread)
    printUnplanned(thread)
    sb.toString
  }

  private[this] def printPlanned(thread: Thread[_ <: Work]): Unit = {
    thread.asTaskThread foreach { t =>
      if (allowAnsi) sb.append(Console.RESET)
      sb.append("         Planned:\n")
      val tasks = findAllTaskPaths(Nil, t)
      val tasksByEvent = tasks.groupBy(_.task.targetEventRef.map(_.id))
      val plannedTasksByEvent = tasksByEvent.collect({ case (Some(eventId), tasks) => eventId -> tasks })
      val sortedEvents = project.events.findByIds(plannedTasksByEvent.keys.toSeq).sorted(project.eventsOrdering)
      for(event <- sortedEvents) {
        if (!isEventPlanTotallyDone(event, plannedTasksByEvent(event.id).map(_.task))) {
          printEventWithTaskPaths(1, event, plannedTasksByEvent(event.id))
        }
      }
    }
  }

  /**
    * Displays the straightforward hierarchy style,
    * simply omitting all planned tasks
    */
  private[this] def printUnplanned(thread: Thread[_ <: Work]) = {
    if (allowAnsi) sb.append(Console.RESET)
    sb.append("         Unplanned:\n")
    printThreadWithUnplannedTasks(0, thread, true)
  }

  private[this] def indent(depth: Int) = " " * (depth * 2)

  private[this] def printLineItem(depth: Int, item: Node[_], icon: String, color: String, prefix: String = ""): Unit = {
    var len = 0
    val shortId = item.id.toString.substring(0, 6)
    if (allowAnsi) sb.append(Console.RESET)
    len += append(sb, shortId + indent(depth) + " ")
    if (allowAnsi) sb.append(color)
    len += append(sb, icon + " " + prefix)
    if (allowAnsi) sb.append(color)
    len += append(sb, item.subject)
    statuses.get(item.id).foreach { statuses =>
      sb.append(" " * (50 - len))
      if (allowAnsi) sb.append(Console.RESET)
      val statusStr = statuses.map(status => s"${status.user.name}[${status.message}]").mkString(" ")
      sb.append(statusStr)
    }
    sb.append("\n")
  }

  private[this] def append(sb: StringBuilder, s: String): Int = {
    sb.append(s)
    s.length
  }

  private[this] def printThreadWithUnplannedTasks(depth: Int, thread: Thread[_ <: Work], omitThreadLineItem: Boolean = false): Unit = {
    if (!omitThreadLineItem) {
      val icon = thread.workType.threadIcon
      printLineItem(depth, thread, icon, Console.BLUE + Console.BOLD)
    }
    thread.workType match {
      case TaskWorkType =>
        val tasks = project.tasks.findByIds(thread.children.toIds)
        val unplannedTasks = tasks.filter(!_.isPlanned)
        for(task <- unplannedTasks) {
          printTaskWithUnplannedSubtasks(depth + 1, task)
        }
      case EventWorkType =>
        for(event <- project.events.findByIds(thread.children.toIds)) {
          if (!isNodeTotallyDone(event)) {
            printEvent(depth + 1, event)
          }
        }
    }
    for {
      subthread <- project.findSubThreads(thread.id)
      if !subthread.completed
    } {
      printThreadWithUnplannedTasks(depth + 1, subthread)
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
  private[this] def findAllTaskPaths(prefix: Seq[Node[_]], t: Thread[Task]): Seq[TaskPath] = {
    val myTaskPaths = findAllTaskPaths(prefix, t.children)
    val subthreadsTaskPaths =
      project.findSubThreads(t.id)
        .collect { case TaskThread(tt) => tt }
        .flatMap(tt => findAllTaskPaths(prefix :+ tt, tt))
    myTaskPaths ++ subthreadsTaskPaths
  }

  private[this] def findAllTaskPaths(prefix: Seq[Node[_]], xs: List[Ref[Task]]): Seq[TaskPath] = {
    val taskPaths = project.tasks.findByRefs(xs).map(t => TaskPath(prefix, t))
    val recurse = taskPaths.flatMap(subtaskPath => findAllTaskPaths(prefix :+ subtaskPath.task, subtaskPath.task.children))
    taskPaths ++ recurse
  }

  private[this] def printTaskWithUnplannedSubtasks(depth: Int, task: Task, prefix: String = ""): Unit = {
    printTaskLineItem(depth, task, prefix)
    val subtasks = project.tasks.findByRefs(task.children)
    subtasks
      .filter(!_.isPlanned) // BUGBUG: printTask() is invoked for planned section too
      .foreach(printTaskWithUnplannedSubtasks(depth + 1, _))
  }

  def printTaskLineItem(depth: Int, task: Task, prefix: String = ""): Unit = {
    val icon = if (task.completed) "[x]" else "[ ]"
    val color = if (task.completed) Console.GREEN else Console.GREEN + Console.BOLD
    printLineItem(depth, task, icon, color, prefix)
  }

  private[this] def printTaskPathWithUnplannedSubtasks(depth: Int, taskPath: TaskPath): Unit = {
    val prefix = taskPath.pathString(allowAnsi)
    printTaskWithUnplannedSubtasks(depth, taskPath.task, prefix)
  }

  private[this] def printEvent(depth: Int, event: Event): Unit = {
    printEventLineItem(depth, event)
    for(subtask <- project.tasks.findByRefs(event.children)) {
      printTaskWithUnplannedSubtasks(depth + 1, subtask)
    }
  }

  def printEventLineItem(depth: Int, event: Event) = {
    val icon = if (event.completed) "![x]" else "![ ]"
    val color = if (event.completed) Console.YELLOW else (Console.YELLOW + Console.BOLD)
    printLineItem(depth, event, icon, color)
  }

  private[this] def printEventWithTaskPaths(depth: Int, event: Event, taskPaths: Seq[TaskPath]): Unit = {
    printEventLineItem(depth, event)
    for(subtask <- taskPaths) {
      printTaskPathWithUnplannedSubtasks(depth + 1, subtask)
    }
  }
}