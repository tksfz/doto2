package org.tksfz.doto.main

import org.eclipse.jgit.api.errors.TransportException

import scala.util.{Failure, Success}

/**
  * Created by thom on 4/1/17.
  */
object SyncCmdExec extends CmdExec[Sync] {
  override def execute(c: Config, cmd: Sync): Unit = WithActiveGitBackedProject { project =>
    cmd.remote.map { uri =>
      project.setRemote(uri.toString)
    }
    if (!project.hasRemote) {
      println(s"No remote for ${project.name}")
    } else {
      println(s"Syncing with '${project.remote.get}'...")
      project.sync()
      println(s"Synced.")
      // TODO: report the number of synced objects
    }
  }
}
