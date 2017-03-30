package org.tksfz.doto.main

import java.io.File

import org.eclipse.jgit.transport.URIish
import org.tksfz.doto.repo.{GitBackedRepo, Projects}

/**
  * Created by thom on 3/18/17.
  */
object CloneCmdExec extends CmdExec[Clone] {
  override def execute(c: Config, cmd: Clone): Unit = {
    val uri = new URIish(cmd.url.toURL)
    val projectName = cmd.name getOrElse uri.getHumanishName
    val location = Projects.defaultProjectRoot(projectName)
    if (location.exists) {
      println(s"'$projectName' already exists in ~/.doto. Use -n to clone with a different project name.")
    } else {
      println(s"Cloning project '$projectName'...")
      GitBackedRepo.clone(cmd.url, location.toJava)
      //println Cloned N threads, X tasks, Y events.

      // Make it the active project
      println(s"Setting '$projectName' as active project.")
      Projects.setActiveProject(projectName)
    }
  }
}
