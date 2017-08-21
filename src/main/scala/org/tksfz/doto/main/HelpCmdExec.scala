package org.tksfz.doto.main

/**
  * Created by thom on 8/17/17.
  */
object HelpCmdExec extends CmdExec[HelpCmd] {
  override def execute(c: Config, cmd: HelpCmd): Unit = {
    cmd.cmd map { cmd =>
      Main.parser.renderTwoColumnsUsage(cmd) map { cmdUsage =>
        println(cmdUsage)
      } getOrElse {
        println(s"'$cmd' is not a doto command. See 'doto help' for a list of common commands.")
      }
    } getOrElse {
      Main.parser.showUsage()
    }
  }
}
