package org.tksfz.doto

import java.io.File

import scopt.{OptionDef, Read}

case class Config(cmd: Option[Command] = None)

sealed trait Command
case class Init(location: Option[File]) extends Command
case class ThreadCmd(parentId: String, subject: String) extends Command
case class ListCmd(verbose: Boolean = false) extends Command

trait CommandExec[T] {
  def execute(c: Config, t: T): Unit
}

object Main {

  def main(args: Array[String]): Unit = {
    parser.parse(args, Config()) match {
      case Some(c@Config(Some(cmd))) => cmd match {
        case add: ThreadCmd => ThreadCommandExec.execute(c, add)
        case ls: ListCmd => ListCommandExec.execute(c, ls)
        case _ => println(c)
      }
      case Some(config) =>
        println(config)
      case None =>
    }
  }

  val parser = new scopt.OptionParser[Config]("doto") {
    head("doto", "0.1")

    cmd("init").action((_, c) => c.copy(cmd = Some(Init(None))))
      .text("Initialize a new doto repo.")
      .children(
        arg[File]("location").optional().cmdaction[Init]((x, c) => c.copy(location = Some(x)))
      )

    cmd("add").action((_, c) => c.copy(cmd = Some(ThreadCmd("", ""))))
      .text("Add a task, event, or thread")
      .children(
        opt[String]('p', "parent").valueName("parent").cmdaction[ThreadCmd]((x, c) => c.copy(parentId = x)),
        arg[String]("subject").cmdaction[ThreadCmd]((x, c) => c.copy(subject = x))
      )

    cmd("ls").action((_, c) => c.copy(cmd = Some(ListCmd())))
      .text("List objects")
  }

  implicit class OptionDefExtensions[A: Read](d: OptionDef[A, Config]) {
    def cmdaction[T <: Command](f: (A, T) => T) = {
      d.action((x, c) =>
        c.copy(cmd = c.cmd.map(cmd => f(x, cmd.asInstanceOf[T])))
      )
    }
  }
}
