package org.tksfz.doto.main

import java.io.File
import java.net.URI

import org.tksfz.doto.repo.{Projects, Repo}

/**
  * Created by thom on 3/23/17.
  */
sealed trait Cmd
case class Init(location: Option[File]) extends Cmd
case class New(name: String) extends Cmd
case class Add(parentId: String, subject: String) extends Cmd
case class ThreadCmd(parentId: String, subject: String, isEvent: Boolean = false) extends Cmd
case class ListCmd(ignoreFocus: Boolean = false) extends Cmd
case class Complete(id: String) extends Cmd
case class Set(id: String, newSubject: Option[String] = None, newParent: Option[String] = None) extends Cmd
case class Plan(taskId: String, eventId: String) extends Cmd
case class Focus(id: String) extends Cmd

/* Project-related commands */
case class Project(list: Boolean = false, projectName: Option[String] = None) extends Cmd

// The following only make sense when using a git-backed repo
case class Clone(url: URI, location: Option[File] = None) extends Cmd

trait CmdExec[T] {
  def execute(c: Config, cmd: T): Unit

  def CommandWithActiveProject[T](f: Repo => T) = {
    Projects.activeProject.map(f).getOrElse {
      println("no active project")
    }
  }
}

object CmdExec {
  def execute(c: Config) = c match {
    case Config(Some(cmd)) => cmd match {
      case add: Add => AddCmdExec.execute(c, add)
      case thread: ThreadCmd => ThreadCmdExec.execute(c, thread)
      case ls: ListCmd => ListCmdExec.execute(c, ls)
      case init: Init => InitCmdExec.execute(c, init)
      case complete: Complete => CompleteCmdExec.execute(c, complete)
      case set: Set => SetCmdExec.execute(c, set)
      case plan: Plan => PlanCmdExec.execute(c, plan)
      case focus: Focus => FocusCmdExec.execute(c, focus)
      case project: Project => ProjectCmdExec.execute(c, project)
      case cmd: New => NewCmdExec.execute(c, cmd)
      case clone: Clone => CloneCmdExec.execute(c, clone)
      case _ => println(c)
    }
    case Config(None) => ()
  }
}