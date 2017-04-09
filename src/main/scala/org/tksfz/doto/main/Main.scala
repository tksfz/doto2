package org.tksfz.doto.main

import java.io.File
import java.net.URI

import scopt.{OptionDef, Read}

case class Config(args: Array[String], cmd: Option[Cmd] = None) {
  def originalCommandLine = {
    "doto " + args.map(mkShellArgString).mkString(" ")
  }

  /**
    * not using http://stackoverflow.com/questions/5187242/encode-a-string-to-be-used-as-shell-argument
    * instead just doing something quick and dirty
    */
  private[this] def mkShellArgString(s: String) = {
    val triggers = Seq(" ", "\t", "\"", "'")
    if (triggers.exists(s.contains(_))) {
      var esc = s.replaceAllLiterally("'", "\'")
      esc = esc.replaceAllLiterally("\"", "\\\"")
      "\"" + esc + "\""
    } else {
      s
    }
  }
}

object Main {

  def main(args: Array[String]): Unit = {
    parser.parse(args, Config(args)) match {
      case Some(c@Config(_, Some(_))) =>
        CmdExec.execute(c)
      case Some(config) =>
        parser.showUsage()
      case None =>
    }
  }

  val parser = new scopt.OptionParser[Config]("doto") {
    head("doto", "0.1")

    help("help") text("hi")

    // invoking hidden() doesn't quite work because it ends up mutating the OptionDef's contained in addParser
    options ++= addParser.optionsForRender.map(_.hidden())
    options ++= helpParser.optionsForRender.map(_.hidden())
  }

  lazy val addParser = new scopt.OptionParser[Config]("doto") {
    head("doto", "0.1")

    cmd("add").action((_, c) => c.copy(cmd = Some(Add("", ""))))
      .text("Add a task or event")
      .children(
        opt[String]('p', "parent").valueName("<parent>").cmdaction[Add]((x, c) => c.copy(parentId = x)),
        arg[String]("<subject>").cmdaction[Add]((x, c) => c.copy(subject = x))
      )
  }

  lazy val helpParser = new scopt.OptionParser[Config]("doto") {
    cmd("help").action((_, c) => c.copy(cmd = Some(Help(""))))
      .text("Show usage for a subcommand")
      .children(
        arg[String]("<command>").cmdaction[Help]((x, c) => c.copy(cmd = x))
      )
  }

  implicit class OptionDefExtensions[A: Read](d: OptionDef[A, Config]) {
    def cmdaction[T <: Cmd](f: (A, T) => T) = {
      d.action((x, c) =>
        c.copy(cmd = c.cmd.map(cmd => f(x, cmd.asInstanceOf[T])))
      )
    }
  }
}
