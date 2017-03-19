package org.tksfz.doto.main

import java.io.File

import org.eclipse.jgit.transport.URIish
import org.tksfz.doto.repo.GitBackedRepo

/**
  * Created by thom on 3/18/17.
  */
object CloneCmdExec extends CmdExec[Clone] {
  override def execute(c: Config, cmd: Clone): Unit = {
    // TODO: remove log4j messages
    val loc = cmd.location getOrElse {
      // By default JGIt does "uri.getHumanishName/.git", but the /.git part makes no sense
      val uri = new URIish(cmd.url.toURL)
      new File(uri.getHumanishName)
    }
    println("Cloning into '" + loc + "'...")
    GitBackedRepo.clone(cmd.url, loc)
    //println Cloned N threads, X tasks, Y events.
  }
}
