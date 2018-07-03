package org.tksfz.doto.main

import better.files._

object EditCmdExec extends CmdExec[EditCmd] {
  override def execute(c: Config, cmd: EditCmd) = WithActiveProject { project =>
    project.tasks.findByIdPrefix(cmd.id).map { task =>
      // https://stackoverflow.com/questions/4823468/comments-in-markdown
      val header =
        s"""<!---
           |You are editing the description field of task ${task.id}. You can use Markdown here.
           |
           |    ${task.subject}
           |-->
           |
           |
           |""".stripMargin

      val edited = editContent(header + task.description.getOrElse(""))

      val newContents = stripEditingComment(edited).trim
      project.put(task.copy(description = Some(newContents)))
    }.getOrElse {
      println(s"Couldn't find task with id ${cmd.id}")
    }
  }

  private def stripEditingComment(edited: String) = {
    val marker = "-->"
    val iMarker = edited.indexOf(marker)
    if (iMarker < 0) {
      edited
    } else {
      edited.substring(iMarker + marker.length)
    }
  }

  private def editContent(content: String): String = {
    val f = File.newTemporaryFile("doto-", ".md")
    f.overwrite(content)
    runEditor(f.pathAsString)
    val newContents = f.contentAsString
    f.delete()
    newContents
  }

  private def runEditor(filename: String) = {
    val editor = sys.env.getOrElse("EDITOR", "vi")
    runConsoleProcess(editor, filename)
  }

  // https://stackoverflow.com/questions/9690702/how-can-i-launch-vi-from-within-java-under-commons-exec
  private def runConsoleProcess(command: String, args: String*) = {
    import scala.collection.JavaConverters.seqAsJavaListConverter
    val processBuilder = new ProcessBuilder((command +: args).asJava)
    processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
    processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
    processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT)

    val p = processBuilder.start()
    // wait for termination.
    p.waitFor()
  }
}
