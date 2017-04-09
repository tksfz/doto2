package org.tksfz.doto.main

/**
  * Created by thom on 4/9/17.
  */
object HelpCmdExec extends CmdExec[Help] {


  override def execute(c: Config, cmd: Help): Unit = {
    val parser = Main.addParser
    parser.showUsage()
  }
}
