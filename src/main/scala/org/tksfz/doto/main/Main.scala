package org.tksfz.doto.main

import java.io.File
import java.net.URI

import scopt.{CmdUsageOptionParser, OptionDef, Read}

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
      esc = esc.replaceAllLiterally("\\", "\\\\")
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
        HelpCmdExec.execute(config, HelpCmd(None))
      case None =>
    }
  }

  val parser = new CmdUsageOptionParser[Config]("doto") {
    head("doto", "0.1")

    setUsage(
      """
        |Usage: doto <command> [<args>]
        |
        |Common doto commands:
        |
        |manage projects
        |  new        Create a new project
        |  get        Fetch a project from a remote repo
        |  sync       Sync a project with a remote repo
        |  project    List projects or switch the active project
        |
        |manage tasks, events, and threads
        |  add        Add a task or event
        |  thread     Create a thread
        |  ls         List work
        |  complete   Mark a task, event, or thread as completed
        |  set        Update a task, event, or thread
        |  schedule   Schedule a task for an event
        |  focus      Switch focus
        |
        |people
        |  status     Set your status
        |
        |Use 'doto help <command>' to learn about a particular subcommand.
      """.stripMargin)

    cmd("init").action((_, c) => c.copy(cmd = Some(Init(None))))
      .text("Initialize a new doto repo.")
      .children(
        arg[File]("<location>").optional().cmdaction[Init]((x, c) => c.copy(location = Some(x)))
      )

    note("")
    cmd("add").action((_, c) => c.copy(cmd = Some(Add("", ""))))
      .text("Add a task or event")
      .children(
        opt[String]('p', "parent").valueName("<parent>").cmdaction[Add]((x, c) => c.copy(parentId = x)),
        arg[String]("<subject>").cmdaction[Add]((x, c) => c.copy(subject = x))
      )

    note("")
    cmd("thread").action((_, c) => c.copy(cmd = Some(ThreadCmd("", ""))))
      .text("Add a thread")
      .children(
        opt[String]('p', "parent").valueName("<parent>").cmdaction[ThreadCmd]((x, c) => c.copy(parentId = x)),
        opt[Unit]('e', "event").cmdaction[ThreadCmd]((x, c) => c.copy(isEvent = true)),
        arg[String]("<subject>").cmdaction[ThreadCmd]((x, c) => c.copy(subject = x))
      )

    note("")
    cmd("ls").action((_, c) => c.copy(cmd = Some(ListCmd())))
      .text("List work")
      .children(
        opt[Unit]("no-focus").cmdaction[ListCmd]((x, c) => c.copy(ignoreFocus = true))
      )

    note("")
    cmd("complete").action((_, c) => c.copy(cmd = Some(Complete(""))))
      .text("Mark a task, event, or thread as completed")
      .children(
        arg[String]("<id>").cmdaction[Complete]((x, c) => c.copy(id = x))
      )

    note("")
    cmd("set").action((_, c) => c.copy(cmd = Some(SetCmd(""))))
      .text("Set a new subject on a task, event, or thread")
      .children(
        arg[String]("<id>").cmdaction[SetCmd]((x, c) => c.copy(id = x)),
        opt[String]('p', "parent").valueName("<parent>").cmdaction[SetCmd]((x, c) => c.copy(newParent = Some(x))),
        arg[String]("<subject>").optional().cmdaction[SetCmd]((x, c) => c.copy(newSubject = Some(x)))
      )


    note("")
    cmd("schedule").action((_, c) => c.copy(cmd = Some(Schedule(Nil, ""))))
      .text("Schedule a task to be completed for the specified event")
      .children(
        arg[String]("<taskId>...").unbounded().cmdaction[Schedule]((x, c) => c.copy(taskIds = c.taskIds :+ x)),
        opt[String]('e', "event").valueName("<eventId>").cmdaction[Schedule]((x, c) => c.copy(eventId = x))
      )

    // TODO: is there a way to do this without copy-pasting?
    cmd("sched").action((_, c) => c.copy(cmd = Some(Schedule(Nil, ""))))
      .text("Schedule a task to be completed for the specified event")
      .children(
        arg[String]("<taskId>...").unbounded().cmdaction[Schedule]((x, c) => c.copy(taskIds = c.taskIds :+ x)),
        opt[String]('e', "event").valueName("<eventId>").cmdaction[Schedule]((x, c) => c.copy(eventId = x))
      )

    note("")
    cmd("focus").action((_, c) => c.copy(cmd = Some(Focus(None))))
      .text("Modify focus")
      .children(
        arg[String]("<id>").optional().cmdaction[Focus]((x, c) => c.copy(id = Some(x)))
          .text("Set focus to the specified thread"),
        opt[Unit]('x', "exclude").cmdaction[Focus]((x, c) => c.copy(exclude = true)),
        opt[Unit]('r', "reset").cmdaction[Focus]((x, c) => c.copy(reset = true))
          .text("Clear focus")
      )

    note("")
    cmd("project").action((_, c) => c.copy(cmd = Some(ProjectCmd())))
      .text("Manage projects")
      .children(
        arg[String]("<project>").optional().cmdaction[ProjectCmd]((x, c) => c.copy(projectName = Some(x)))
      )

    // TOOD: can this be consolidated with sync?
    note("")
    cmd("get").action((_, c) => c.copy(cmd = Some(Clone(null))))
      .text("Clone a doto project from a git repo")
      .children(
        arg[String]("url").cmdaction[Clone]((x, c) => c.copy(url = x)),
        opt[String]('n', "name").cmdaction[Clone]((x, c) => c.copy(name = Some(x)))
      )

    note("")
    cmd("new").action((_, c) => c.copy(cmd = Some(New(""))))
      .text("Create a new project")
      .children(
        arg[String]("<project>").cmdaction[New]((x, c) => c.copy(name = x))
      )

    note("")
    cmd("sync").action((_, c) => c.copy(cmd = Some(Sync())))
      .text("Sync project with remote")
      .children(
        opt[String]('r', "remote").cmdaction[Sync]((x, c) => c.copy(remote = Some(x)))
      )

    note("")
    cmd("delete").action((_, c) => c.copy(cmd = Some(Delete(""))))
      .text("Delete a task or event")
      .children(
        arg[String]("<id>").cmdaction[Delete]((x, c) => c.copy(id = x))
      )

    note("")
    cmd("status").action((_, c) => c.copy(cmd = Some(StatusCmd(Map(), Nil))))
      .text("Set your status")
      .children(
        arg[Map[String, String]]("<id>=<message>...").optional().cmdaction[StatusCmd]((x, c) => c.copy(activities = x)),
        opt[String]('r', "remove").valueName("<id>...").cmdaction[StatusCmd]((x, c) => c.copy(remove = c.remove :+ x))
          .children(arg[String]("").optional().unbounded().cmdaction[StatusCmd]((x, c) => c.copy(remove = c.remove :+ x)))
      )

    cmd("help").action((_, c) => c.copy(cmd = Some(HelpCmd(None))))
      .text("Help")
      .children(
        arg[String]("<command>").optional().cmdaction[HelpCmd]((x, c) => c.copy(cmd = Some(x)))
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
