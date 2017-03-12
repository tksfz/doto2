package org.tksfz.doto

import java.io.File

import scopt.{OptionDef, Read}

trait CommandExec[T] {
  def execute(t: T): Unit
}

object Main {

  case class Config(cmd: Option[Command] = None)

  sealed trait Command
  case class Init(location: Option[File]) extends Command
  case class Add(subject: String) extends Command
  case object List extends Command

  def main(args: Array[String]): Unit = {
    parser.parse(args, Config()) match {
      case Some(Config(Some(Init(loc)))) =>
        println(loc)
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
        arg[File]("location").optional().cmdaction[Init]((x, c) =>
          c.copy(location = Some(x))
        )
      )
  }

  implicit class OptionDefExtensions[A: Read](d: OptionDef[A, Config]) {
    def cmdaction[T <: Command](f: (A, T) => T) = {
      d.action((x, c) =>
        c.copy(cmd = c.cmd.map(cmd => f(x, cmd.asInstanceOf[T])))
      )
    }
  }
}
