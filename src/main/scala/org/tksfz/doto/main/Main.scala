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

    note("")
    cmd("add").action((_, c) => c.copy(cmd = Some(Add("", ""))))
      .text("Add a task or event")
      .children(
        opt[String]('p', "parent").valueName("parent").cmdaction[Add]((x, c) => c.copy(parentId = x)),
        arg[String]("subject").cmdaction[Add]((x, c) => c.copy(subject = x))
      )

    note("")
    cmd("thread").action((_, c) => c.copy(cmd = Some(ThreadCmd("", ""))))
      .text("Add a thread")
      .children(
        opt[String]('p', "parent").valueName("parent").cmdaction[ThreadCmd]((x, c) => c.copy(parentId = x)),
        opt[Unit]('e', "event").cmdaction[ThreadCmd]((x, c) => c.copy(isEvent = true)),
        arg[String]("subject").cmdaction[ThreadCmd]((x, c) => c.copy(subject = x))
      )

    note("")
    cmd("ls").action((_, c) => c.copy(cmd = Some(ListCmd())))
      .text("List objects")
      .children(
        opt[Unit]("no-focus").cmdaction[ListCmd]((x, c) => c.copy(ignoreFocus = true))
      )

    note("")
    cmd("complete").action((_, c) => c.copy(cmd = Some(Complete(""))))
      .text("Mark a task, event, or thread as completed")
      .children(
        arg[String]("id").cmdaction[Complete]((x, c) => c.copy(id = x))
      )

    note("")
    cmd("set").action((_, c) => c.copy(cmd = Some(Set(""))))
      .text("Set a new subject on a task, event, or thread")
      .children(
        arg[String]("id").cmdaction[Set]((x, c) => c.copy(id = x)),
        opt[String]('p', "parent").valueName("parent").cmdaction[Set]((x, c) => c.copy(newParent = Some(x))),
        arg[String]("subject").optional().cmdaction[Set]((x, c) => c.copy(newSubject = Some(x)))
      )

    note("")
    cmd("plan").action((_, c) => c.copy(cmd = Some(Plan("", ""))))
      .text("Target a task to be completed for the specified event")
      .children(
        arg[String]("taskId").cmdaction[Plan]((x, c) => c.copy(taskId = x)),
        arg[String]("eventId").cmdaction[Plan]((x, c) => c.copy(eventId = x))
      )

    note("")
    cmd("focus").action((_, c) => c.copy(cmd = Some(Focus(""))))
      .text("Put a thread into focus")
      .children(
        arg[String]("id").cmdaction[Focus]((x, c) => c.copy(id = x))
      )

    note("")
    cmd("project").action((_, c) => c.copy(cmd = Some(Project())))
      .text("Manage projects")
      .children(
        arg[String]("project").optional().cmdaction[Project]((x, c) => c.copy(projectName = Some(x)))
      )

    note("")
    cmd("get").action((_, c) => c.copy(cmd = Some(Clone(null))))
      .text("Clone a doto project from a git repo")
      .children(
        arg[URI]("url").cmdaction[Clone]((x, c) => c.copy(url = x)),
        opt[String]('n', "name").cmdaction[Clone]((x, c) => c.copy(name = Some(x)))
      )

    note("")
    cmd("new").action((_, c) => c.copy(cmd = Some(New(""))))
      .text("Create a new project")
      .children(
        arg[String]("project").cmdaction[New]((x, c) => c.copy(name = x))
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
