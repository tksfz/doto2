package org.tksfz.doto.main

/**
  * Created by thom on 4/1/17.
  */
object SyncCmdExec extends CmdExec[Sync] {
  override def execute(c: Config, cmd: Sync): Unit = WithActiveGitBackedProject { project =>
    if (project.sync()) {
      println("Synced")
    } else {
      println(s"No remote for ${project.name}")
    }
  }
}
