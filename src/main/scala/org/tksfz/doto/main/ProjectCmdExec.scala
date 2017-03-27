package org.tksfz.doto.main

import org.tksfz.doto.repo.Projects

/**
  * Created by thom on 3/23/17.
  */
object ProjectCmdExec extends CmdExec[Project] {
  override def execute(c: Config, cmd: Project): Unit = {
    if (cmd.list) {
      Projects.listProjects match {
        case Nil => println("no projects found")
        case projects =>
          for(p <- projects) {
            if (p == Projects.activeProjectName) print("* ") else print ("  ")
            println(p)
          }
      }
    } else {
      Projects.activeProjectName map { p =>
        println("* " + p)
      } getOrElse {
        println("no active project")
      }
    }
  }
}
