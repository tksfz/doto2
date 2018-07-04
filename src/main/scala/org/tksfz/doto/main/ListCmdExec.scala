package org.tksfz.doto.main

import org.tksfz.doto.project.Project
import org.tksfz.doto.model._
import org.tksfz.doto.util.handy._

object ListCmdExec extends CmdExec[ListCmd] with ProjectExtensionsImplicits {
  override def execute(c: Config, cmd: ListCmd): Unit = WithActiveProject { repo =>
    val thread =
      (!cmd.ignoreFocus).thenSome {
        repo.focus.option.map { focusId =>
          repo.threads.get(focusId).toTry.get
        }
      }.flatten.getOrElse {
        repo.rootThread
      }
    // Even with --no-focus we respect the focus-excludes
    print(new DefaultPrinter(repo, repo.focusExcludes.option.getOrElse(Nil)).print(thread))
  }


}

/**
  * This class just lets us put `repo` and `sb` into scope so that every method doesn't need
  * to declare them
  */
abstract class Printer {
  // If we're being piped or redirected, suppress ansi colors
  // http://stackoverflow.com/questions/1403772/how-can-i-check-if-a-java-programs-input-output-streams-are-connected-to-a-term
  def allowAnsi = System.console() != null
}

private case class Path[T <: Work](path: Seq[Node[_]], t: T) {
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
class DefaultPrinter(project: Project, excludes: Seq[Id], sb: StringBuilder = new StringBuilder) extends Printer {
  lazy val statuses: Map[Id, Seq[Status]] = project.statuses.findAll.groupBy(_.nodeId)

  def print(thread: Thread[_ <: Work]): String = {
    printLineItem(0, thread, thread.icon, Console.BLUE + Console.BOLD)
    sb.append("\n")
    printScheduled(thread)
    printUnscheduled(thread)
    sb.toString
  }

  private[this] def printScheduled(thread: Thread[_ <: Work]): Unit = {
    thread.asTaskThread foreach { t =>
      if (allowAnsi) sb.append(Console.RESET)
      sb.append("         Scheduled:\n")
      /**
        * Note that findAllWorkPaths returns paths that start from below the initial thread (i.e. not
        * necessarily the full path from the root) since prefix = Nil.
        *
        * So if excludes contains an ancestor of the initial thread or the initial thread itself,
        * isExcluded will return false. (Since the paths passed into isExcluded won't contain
        * any ancestors.)
        *
        * Hence, excludes of an ancestor to the focus never apply. Or in other words, the focus
        * overrides the excludes.
        */
      val tasks = findAllWorkPaths(Nil, t, false).asInstanceOf[Seq[Path[Task]]].filterNot(isExcluded)

      // TODO: remove eventsOrdering and print events by thread instead

      // Note that a task can be scheduled for an event E where E lives in a thread outside of our current focus.
      // In fact, this can be common since event threads will typically live somewhere high up in the thread
      // hierarchy.
      // Hence here we gather event paths from the root
      val eventPaths = findAllWorkPaths(Nil, project.rootThread, true).groupBy(_.t.id).mapValues(_.head)

      val tasksByEvent = tasks.groupBy(_.t.targetEventRef.map(_.id))
      val scheduledTasksByEvent = tasksByEvent.collect({ case (Some(eventId), tasks) => eventId -> tasks })
      val sortedEvents = project.events.findByIds(scheduledTasksByEvent.keys.toSeq).sorted(project.eventsOrdering)
      for {
        event <- sortedEvents
        if !isExcluded(eventPaths(event.id))
        if !isEventTotallyDone(event, scheduledTasksByEvent(event.id).map(_.t))
      } {
        printEventWithTaskPaths(1, event, scheduledTasksByEvent(event.id))
      }
    }
  }

  /**
    * Displays the straightforward hierarchy style,
    * simply omitting all scheduled tasks
    */
  private[this] def printUnscheduled(thread: Thread[_ <: Work]) = {
    if (allowAnsi) sb.append(Console.RESET)
    sb.append("         Unscheduled:\n")
    printThreadWithUnscheduledTasks(0, thread, true)
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
      val statusStr = statuses.map { status =>
        val msg = if (status.message.nonEmpty) "[" + status.message + "]" else ""
        status.user.name + msg
      }.mkString(" ")
      sb.append(statusStr)
    }
  }

  private[this] def append(sb: StringBuilder, s: String): Int = {
    sb.append(s)
    s.length
  }

  private[this] def printThreadWithUnscheduledTasks(depth: Int, thread: Thread[_ <: Work], omitThreadLineItem: Boolean = false): Unit = {
    if (!omitThreadLineItem) {
      val icon = thread.workType.threadIcon
      printLineItem(depth, thread, icon, Console.BLUE + Console.BOLD)
      sb.append("\n")
    }
    thread.workType match {
      case TaskWorkType =>
        for {
          task <- project.tasks.findByIds(thread.children.toIds)
          if !isExcluded(task)
          if !task.isScheduled
        } {
          printTaskWithUnscheduledSubtasks(depth + 1, task)
        }
      case EventWorkType =>
        for {
          event <- project.events.findByIds(thread.children.toIds)
          if !isExcluded(event)
          if !isNodeTotallyDone(event)
        } {
          printEvent(depth + 1, event)
        }
    }
    for {
      subthread <- project.findSubThreads(thread.id)
      if !isExcluded(subthread)
      if !subthread.completed
    } {
      printThreadWithUnscheduledTasks(depth + 1, subthread)
    }
  }

  /**
    * Take an event and its subtasks, all tasks scheduled for that event, and all their
    * subtasks. Are they all marked as completed?
    *
    * This is a fairly stringent condition, and we may well want to relax it in
    * the future, but that requires more thought.
    */
  private[this] def isEventTotallyDone(event: Event, scheduledTasks: Seq[Work]) = {
    isNodeTotallyDone(event) && scheduledTasks.forall(isNodeTotallyDone)
  }

  // TODO: move this to a place for common Project-dependent functions
  private[this] def isNodeTotallyDone(node: Node[Task]): Boolean= {
    node.completed && project.tasks.findByRefs(node.children).forall(isNodeTotallyDone)
  }

  /**
    * @param events false to return tasks only; true for events only
    */
  private[this] def findAllWorkPaths(prefix: Seq[Node[_]], t: Thread[_ <: Work], events: Boolean): Seq[Path[_ <: Work]] = {
    val myWorkPaths =
      (t, events) match {
        case (EventThread(et), true) =>
          // TODO: tasks parented to events. In theory these could be scheduled for a different event - should that be supported?
          project.events.findByRefs(et.children).map(e => Path[Event](prefix, e))
        case (TaskThread(tt), false) => findAllTaskPaths(prefix, tt.children)
        case _ => Nil
      }
    val subthreadsWorkPaths =
      project.findSubThreads(t.id)
        .flatMap(tt => findAllWorkPaths(prefix :+ tt, tt, events))
    myWorkPaths ++ subthreadsWorkPaths
  }

  private[this] def findAllTaskPaths(prefix: Seq[Node[_]], xs: List[Ref[Task]]): Seq[Path[Task]] = {
    val taskPaths = project.tasks.findByRefs(xs).map(t => Path[Task](prefix, t))
    val recurse = taskPaths.flatMap(subtaskPath => findAllTaskPaths(prefix :+ subtaskPath.t, subtaskPath.t.children))
    taskPaths ++ recurse
  }

  private[this] def printTaskWithUnscheduledSubtasks(depth: Int, task: Task, prefix: String = ""): Unit = {
    printTaskLineItem(depth, task, prefix)
    val subtasks = project.tasks.findByRefs(task.children)
    subtasks
      .filter(!_.isScheduled) // BUGBUG: printTask() is invoked for scheduled section too
      .filterNot(isExcluded)
      .foreach(printTaskWithUnscheduledSubtasks(depth + 1, _))
  }

  def printTaskLineItem(depth: Int, task: Task, prefix: String = ""): Unit = {
    val icon = if (task.completed) "[x]" else "[ ]"
    val color = if (task.completed) Console.GREEN else Console.GREEN + Console.BOLD
    printLineItem(depth, task, icon, color, prefix)
    sb.append(task.description.map(d => s"${ifAnsi(Console.RESET)} … (${d.length} chars)").getOrElse(""))
    sb.append("\n")
  }

  private[this] def ifAnsi(s: String) = if (allowAnsi) s else ""

  private[this] def printTaskPathWithUnscheduledSubtasks(depth: Int, taskPath: Path[Task]): Unit = {
    val prefix = taskPath.pathString(allowAnsi)
    printTaskWithUnscheduledSubtasks(depth, taskPath.t, prefix)
  }

  private[this] def printEvent(depth: Int, event: Event): Unit = {
    printEventLineItem(depth, event)
    for(subtask <- project.tasks.findByRefs(event.children)
      if !isExcluded(subtask)
    ) {
      printTaskWithUnscheduledSubtasks(depth + 1, subtask)
    }
  }

  def printEventLineItem(depth: Int, event: Event) = {
    val icon = if (event.completed) "![x]" else "![ ]"
    val color = if (event.completed) Console.YELLOW else (Console.YELLOW + Console.BOLD)
    printLineItem(depth, event, icon, color)
    sb.append("\n")
  }

  private[this] def printEventWithTaskPaths(depth: Int, event: Event, taskPaths: Seq[Path[Task]]): Unit = {
    printEventLineItem(depth, event)
    for {
      subtask <- taskPaths
      if !isExcluded(subtask)
    } {
      printTaskPathWithUnscheduledSubtasks(depth + 1, subtask)
    }
  }

  /**
    * Since most of the tree is printed by recursive descent, we can exclude the branches of the tree
    * as we encounter them.
    */
  private[this] def isExcluded(node: Node[_]): Boolean = {
    excludes.contains(node.id) || (node match {
      case work: Work => work.target.flatMap(_.toOption).exists(ref => excludes.contains(ref.id))
      case _ => false
    })
  }

  private[this] def isExcluded(path: Path[_ <: Work]): Boolean = {
    path.path.exists(isExcluded) || isExcluded(path.t)
  }

}