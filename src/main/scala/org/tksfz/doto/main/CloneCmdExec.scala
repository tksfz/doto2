package org.tksfz.doto.main

import java.io.File

import org.eclipse.jgit.transport.URIish
import org.tksfz.doto.repo.{GitBackedRepo, Projects}

/**
  * Created by thom on 3/18/17.
  */
object CloneCmdExec extends CmdExec[Clone] {
  override def execute(c: Config, cmd: Clone): Unit = {
    // TODO: remove log4j messages
    val uri = new URIish(cmd.url.toURL)
    val projectName = uri.getHumanishName
    // TODO: check if project already exists
    val location = Projects.defaultProjectRoot(projectName)
    println(s"Cloning project '$projectName'...")
    GitBackedRepo.clone(cmd.url, location.toJava)
    //println Cloned N threads, X tasks, Y events.
  }
}
