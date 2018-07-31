package org.tksfz.doto

/**
  * Pray for top-level implicit definitions :)
  */
package object main {
  implicit val addCmdExec: CmdExec[Add] = AddCmdExec
  implicit val cloneCmdExec: CmdExec[Clone] = CloneCmdExec
  implicit val completeCmdExec: CmdExec[Complete] = CompleteCmdExec
  implicit val deleteCmdExec: CmdExec[Delete] = DeleteCmdExec
  implicit val editCmdExec: CmdExec[EditCmd] = EditCmdExec
  implicit val focusCmdExec: CmdExec[Focus] = FocusCmdExec
  implicit val helpCmdExec: CmdExec[HelpCmd] = HelpCmdExec
  implicit val initCmdExec: CmdExec[Init] = InitCmdExec
  implicit val listCmdExec: CmdExec[ListCmd] = ListCmdExec
  implicit val newCmdExec: CmdExec[New] = NewCmdExec
  implicit val projectCmdExec: CmdExec[ProjectCmd] = ProjectCmdExec
  implicit val scheduleCmdExec: CmdExec[Schedule] = ScheduleCmdExec
  implicit val setCmdExec: CmdExec[SetCmd] = SetCmdExec
  implicit val statusCmdExec: CmdExec[StatusCmd] = StatusCmdExec
  implicit val syncCmdExec: CmdExec[Sync] = SyncCmdExec
  implicit val threadCmdExec: CmdExec[ThreadCmd] = ThreadCmdExec
  implicit val viewCmdExec: CmdExec[ViewCmd] = ViewCmdExec
}
