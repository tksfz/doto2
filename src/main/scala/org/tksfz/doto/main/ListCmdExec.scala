package org.tksfz.doto

import java.nio.file.Paths

import org.tksfz.doto.Ref.RefList
import org.tksfz.doto.repo.Repo

object ListCmdExec extends CmdExec[ListCmd] {
  override def execute(c: Config, t: ListCmd): Unit = {
    val repo = new Repo(Paths.get(""))
    print(new DefaultPrinter(repo).get)
  }
}

abstract class Printer(repo: Repo, val sb: StringBuilder = new StringBuilder) {
  def get: String
}

// List all threads and tasks hierarchically
// Within a thread:
//   - print tasks grouped by event
//   - print untargetted tasks
//   - print sub-threads and recurse
class DefaultPrinter(repo: Repo) extends Printer(repo) {
  override lazy val get = {
    printThread(0, repo.rootThread)
    sb.toString
  }

  private[this] def printThread(depth: Int, thread: Thread[_ <: Work]): Unit = {
    sb.append(" " * (depth * 2))
    val icon = thread.`type`.apply.threadIcon
    sb.append(icon + " " + thread.id.toString.substring(0, 6) + " " + thread.subject + "\n")
    thread.`type`.apply match {
      case TaskWorkType =>
        for(task <- repo.tasks.findByIds(thread.children.toIds)) {
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
    sb.append(" " * (depth * 2))
    val check = if (event.completed) "x" else " "
    sb.append("![" + check + "] " + event.id.toString.substring(0, 6) + " " + event.subject + "\n")
    for(subtask <- repo.tasks.findByIds(event.children.toIds)) {
      printTask(sb, depth + 1, subtask)
    }
  }
}