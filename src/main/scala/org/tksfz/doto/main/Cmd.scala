package org.tksfz.doto.main

import java.io.File
import java.net.URI

import org.tksfz.doto.project.{GitBackedProject, Project, Projects, Transactional}

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
case class SetCmd(id: String, newSubject: Option[String] = None, newParent: Option[String] = None) extends Cmd
case class Schedule(taskIds: Seq[String], eventId: String) extends Cmd
case class Focus(id: Option[String], exclude: Boolean = false, reset: Boolean = false) extends Cmd
case class Delete(id: String) extends Cmd
case class HelpCmd(cmd: Option[String]) extends Cmd

/* Non-core commands */
case class StatusCmd(id: String, status: String, remove: Seq[String]) extends Cmd

/* Project-related commands */
case class ProjectCmd(projectName: Option[String] = None) extends Cmd

// The following only make sense when using a git-backed repo
case class Clone(url: String, name: Option[String] = None) extends Cmd
case class Sync(remote: Option[String] = None) extends Cmd

trait CmdExec[T] {
  def execute(c: Config, cmd: T): Unit

  protected final def WithActiveProject[T](f: Project => T) = {
    Projects.activeProject.map(f).getOrElse {
      println("no active project")
    }
  }

  protected final def WithActiveProjectTxn[T](f: Project with Transactional => T) = {
    // TODO: check for uncommitted after, and throw exception
    Projects.activeProject.map(f).getOrElse {
      println("no active project")
    }
  }

  protected final def WithActiveGitBackedProject[T](f: GitBackedProject => T) = {
    Projects.activeProject.map(f).getOrElse {
      println("no active project")
    }
  }
}

object CmdExec {
  def execute(c: Config) = c match {
    case Config(_, Some(cmd)) => cmd match {
      case add: Add => AddCmdExec.execute(c, add)
      case thread: ThreadCmd => ThreadCmdExec.execute(c, thread)
      case ls: ListCmd => ListCmdExec.execute(c, ls)
      case init: Init => InitCmdExec.execute(c, init)
      case complete: Complete => CompleteCmdExec.execute(c, complete)
      case set: SetCmd => SetCmdExec.execute(c, set)
      case plan: Schedule => ScheduleCmdExec.execute(c, plan)
      case focus: Focus => FocusCmdExec.execute(c, focus)
      case project: ProjectCmd => ProjectCmdExec.execute(c, project)
      case cmd: New => NewCmdExec.execute(c, cmd)
      case clone: Clone => CloneCmdExec.execute(c, clone)
      case sync: Sync => SyncCmdExec.execute(c, sync)
      case delete: Delete => DeleteCmdExec.execute(c, delete)
      case status: StatusCmd => StatusCmdExec.execute(c, status)
      case help: HelpCmd => HelpCmdExec.execute(c, help)
      case _ => println(c)
    }
    case Config(_, None) => ()
  }
}