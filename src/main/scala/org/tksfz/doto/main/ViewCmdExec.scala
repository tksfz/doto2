package org.tksfz.doto.main

import org.tksfz.doto.model.Task
import org.tksfz.doto.project.Project

object ViewCmdExec extends CmdExec[ViewCmd] {
  override def execute(c: Config, cmd: ViewCmd) = WithActiveProject { project =>
    project.findNodeByIdPrefix(cmd.id).map { node =>
      node match {
        case task@Task(id, subject, completed, target, children, description) =>
          printTaskWithDescription(project, task)
      }
    }
  }

  private[main] def printTaskWithDescription(project: Project, task: Task) = {
    val sb = new StringBuilder
    val printer = new DefaultPrinter(project, Nil, sb)
    printer.printTaskLineItem(0, task)
    sb.append(Console.RESET + task.descriptionStr)
    println(sb.toString)
  }


}
