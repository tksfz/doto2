package org.tksfz.doto.main

import org.tksfz.doto.model.Thread

object FocusCmdExec extends CmdExec[Focus] with ProjectExtensionsImplicits {

  override def execute(c: Config, cmd: Focus): Unit = WithActiveProject { project =>
    // TODO: support any type of node
    if (cmd.reset) {
      project.focus.remove()
    }
    cmd.id.foreach { id =>
      (cmd.exclude, project.findNodeByIdPrefix(id)) match {
        case (false, Some(thread: Thread[_])) =>
          project.focus.put(thread.id)
        case (true, Some(node)) =>
          val excludes = project.focusExcludes.option.getOrElse(Nil)
          project.focusExcludes.put(node.id +: excludes)
        case _ =>
          println("couldn't find node with id starting with '" + id + "'")
      }
    }
  }
}
