package org.tksfz.doto.main

import org.tksfz.doto.project.Projects

/**
  * Created by thom on 3/23/17.
  */
object ProjectCmdExec extends CmdExec[ProjectCmd] {
  override def execute(c: Config, cmd: ProjectCmd): Unit = {
    cmd.projectName.map { p =>
      if (Projects.setActiveProject(p)) {
        println(s"Switched to project '$p'")
      } else {
        println(s"No project '$p' found")
      }
    } getOrElse {
      Projects.listProjects match {
        case Nil => println("no projects found")
        case projects =>
          for (p <- projects) {
            if (Some(p) == Projects.activeProjectName) print("* ") else print("  ")
            println(p)
          }
      }
    }
  }
}
