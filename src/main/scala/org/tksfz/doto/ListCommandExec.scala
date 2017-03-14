package org.tksfz.doto

import java.nio.file.Paths

import org.tksfz.doto.repo.Repo

object ListCommandExec extends CommandExec[Ls] {
  override def execute(c: Config, t: Ls): Unit = {
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

  private[this] def printThread(depth: Int, thread: Thread[_]): Unit = {
    sb.append(" " * (depth * 2))
    sb.append("~~ " + thread.id.toString.substring(0, 6) + " " + thread.subject + "\n")
    /*
    for(task <- findByIds(thread.children.toIds)) {
      printTask(sb, depth + 1, task)
    } */
    for(subthread <- repo.findSubThreads(thread.id)) {
      printThread(depth + 1, subthread)
    }
  }

  /*
  private[this] def printTask(sb: StringBuilder, depth: Int, task: Task) = {
    val check = if (task.completed) "x" else " "
    sb.append("[" + check + "] " + task.subject + "\n")
    for(subtask <- task.children) {
      printTask(sb, depth + 1, subtask)
    }
  } */
}