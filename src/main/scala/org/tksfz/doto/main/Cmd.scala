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
case class EditCmd(id: String) extends Cmd
case class ViewCmd(id: String) extends Cmd
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
  import magnolia._
  import scala.language.experimental.macros

  type Typeclass[T] = CmdExec[T]
  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = throw new UnsupportedOperationException
  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = new Typeclass[T] {
    override def execute(c: Config, cmd: T) = sealedTrait.dispatch(cmd) { subtype =>
      subtype.typeclass.execute(c, subtype.cast(cmd))
    }
  }

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  def execute[T <: Cmd](c: Config, cmd: T)(implicit cmdExec: CmdExec[T]): Unit = {
    cmdExec.execute(c, cmd)
  }

  def execute(c: Config): Unit = c match {
    case Config(_, Some(cmd)) => execute(c, cmd)(gen)
    case Config(_, None) => ()
  }
}