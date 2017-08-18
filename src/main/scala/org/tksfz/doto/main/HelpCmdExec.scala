package org.tksfz.doto.main

/**
  * Created by thom on 8/17/17.
  */
object HelpCmdExec extends CmdExec[HelpCmd] {
  override def execute(c: Config, cmd: HelpCmd): Unit = {
    cmd.cmd map { cmd =>
      println(Main.parser.renderTwoColumnsUsage(cmd))
    } getOrElse {
      Main.parser.showUsage()
    }
  }
}
