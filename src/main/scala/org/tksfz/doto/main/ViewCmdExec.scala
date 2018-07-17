package org.tksfz.doto.main

import org.tksfz.doto.model.{Event, Node, Task, Thread}
import org.tksfz.doto.project.Project

object ViewCmdExec extends CmdExec[ViewCmd] {
  override def execute(c: Config, cmd: ViewCmd) = WithActiveProject { project =>
    project.findNodeByIdPrefix(cmd.id).map { node =>
      printTaskWithDescription(project, node)
    }
  }

  private[main] def printTaskWithDescription(project: Project, node: Node[_]) = {
    val sb = new StringBuilder
    new DefaultPrinter(project, Nil, sb).printNodeLineItem(0, node)
    node match {
      case task: Task => sb.append(Console.RESET + task.descriptionStr)
      case _ => // TODO: in the near future description should be available on all nodes
    }
    println(sb.toString)
  }


}
