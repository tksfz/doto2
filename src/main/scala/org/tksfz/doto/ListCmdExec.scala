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
    for(task <- repo.tasks.findByIds(thread.children.toIds)) {
      printTask(sb, depth + 1, task)
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
}