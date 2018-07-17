package org.tksfz.doto.main

import org.tksfz.doto.model.{Node, Task}
import org.tksfz.doto.project.Project

object ViewCmdExec extends CmdExec[ViewCmd] with Printer {
  override def execute(c: Config, cmd: ViewCmd) = WithActiveProject { project =>
    project.findNodeByIdPrefix(cmd.id).map { node =>
      printTaskWithDescription(project, node)
    }
  }

  private[main] def printTaskWithDescription(project: Project, node: Node[_]) = {
    val sb = new StringBuilder
    sb.append(ifAnsi(Console.WHITE + Console.BOLD) + node.id + "\n")
    new DefaultPrinter(project, Nil, sb).printNodeLineItem(0, node)
    node match {
      case task: Task => sb.append(ifAnsi(Console.RESET) + task.descriptionStr)
      case _ => // TODO: in the near future description should be available on all nodes
    }
    println(sb.toString)
  }


}
