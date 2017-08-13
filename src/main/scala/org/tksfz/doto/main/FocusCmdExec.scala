package org.tksfz.doto.main

import java.nio.file.Paths

import org.tksfz.doto.project.Project

object FocusCmdExec extends CmdExec[Focus] {
  override def execute(c: Config, cmd: Focus): Unit = WithActiveProject { project =>
    // TODO: support any type of node
    if (cmd.reset) {
      project.unsynced.remove("focus")
    }
    cmd.id.foreach { id =>
      project.threads.findByIdPrefix(id) map { thread =>
        project.unsynced.putSingleton("focus", thread.id)
      } getOrElse {
        println("couldn't find thread with id starting with '" + cmd.id + "'")
      }
    }
  }
}
