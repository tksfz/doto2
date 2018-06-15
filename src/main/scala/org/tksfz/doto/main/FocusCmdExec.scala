package org.tksfz.doto.main

import org.tksfz.doto.model.Thread

object FocusCmdExec extends CmdExec[Focus] with ProjectExtensionsImplicits {

  override def execute(c: Config, cmd: Focus): Unit = WithActiveProject { project =>
    // TODO: support any type of node
    if (cmd.reset) {
      project.focus.remove()
    }
    cmd.id.map { id =>
      (cmd.exclude, project.findNodeByIdPrefix(id).get) match {
        case (false, thread: Thread[_]) =>
          project.focus.put(thread.id)
        case (false, _) =>
          println("couldn't find thread with id starting with '" + id + "'")
        case (true, node) =>
          val excludes = project.focusExcludes.option.getOrElse(Nil)
          project.focusExcludes.put(node.id +: excludes)
      }
    }.getOrElse {
      // List
      val sb = new StringBuilder
      val printer = new DefaultPrinter(project, Nil, sb)
      val focusExcludes = project.focusExcludes.option.getOrElse(Nil)
      project.focus.option.map { f =>
        val sb = new StringBuilder
        val printer = new DefaultPrinter(project, Nil, sb)
        project.findNodeByIdPrefix(f.toString).map { n =>
          // find paths
          printer.printLineItem(0, n)
        }
        print(sb.toString)
      }
      focusExcludes.map { x =>
        val sb = new StringBuilder
        val printer = new DefaultPrinter(project, Nil, sb)
        project.findNodeByIdPrefix(x.toString).map { n =>
          // find paths
          printer.printLineItem(0, n)
        }
        print("x " + sb.toString)
      }
      if (project.focus.option.isEmpty && focusExcludes.isEmpty) {
        println("no focus set")
      }
    }
  }
}
