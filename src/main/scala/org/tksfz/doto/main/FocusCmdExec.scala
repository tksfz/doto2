package org.tksfz.doto.main

import java.nio.file.Paths

import org.tksfz.doto.repo.Project

object FocusCmdExec extends CmdExec[Focus] {
  override def execute(c: Config, cmd: Focus): Unit = WithActiveProject { repo =>
    // TODO: support any type of node
    repo.threads.findByIdPrefix(cmd.id) map { thread =>
      repo.unsynced.putSingleton("focus", thread.id)
    } getOrElse {
      println("couldn't find thread with id starting with '" + cmd.id + "'")
    }
  }
}
