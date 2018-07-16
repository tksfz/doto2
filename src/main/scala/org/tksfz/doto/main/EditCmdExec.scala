package org.tksfz.doto.main

import better.files._
import org.tksfz.doto.model.Task
import org.tksfz.doto.project.Project

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

      val edited = editContent(header + task.descriptionStr)

      val newContents = stripEditingComment(edited).trim
      val newTask = task.copy(description = Some(newContents))
      project.put(newTask)
      ViewCmdExec.printTaskWithDescription(project, newTask)
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
    // parent is needed to work around a Graal bug with TempFileHelper.tmpdir
    val f = File.newTemporaryFile("doto-", ".md", Some(File(System.getProperty("java.io.tmpdir"))))
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
