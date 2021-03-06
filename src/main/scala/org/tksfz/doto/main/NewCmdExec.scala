package org.tksfz.doto.main

import org.tksfz.doto.project.{GitBackedProject, Projects}

/**
  * Created by thom on 3/30/17.
  */
object NewCmdExec extends CmdExec[New] {
  override def execute(c: Config, cmd: New): Unit = {
    val root = Projects.defaultProjectRoot(cmd.name)
    if (root.exists) {
      println(s"Project '${cmd.name}' already exists in ~/.doto")
    } else {
      InitCmdExec.init(root, cmd.name)
      Projects.setActiveProject(cmd.name)
      val project = GitBackedProject.init(root.toJava.toPath)
      project.commitAllIfNonEmpty(c.originalCommandLine)
      println(s"Initialized new doto project '${cmd.name}'.")
      println(s"Switched to project '${cmd.name}'")
    }
  }
}
