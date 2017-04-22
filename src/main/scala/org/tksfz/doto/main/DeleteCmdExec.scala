package org.tksfz.doto.main

/**
  * Created by thom on 4/21/17.
  */
object DeleteCmdExec extends CmdExec[Delete] {
  override def execute(c: Config, cmd: Delete): Unit = WithActiveProjectTxn { project =>
    project.findNodeByIdPrefix(cmd.id) flatMap { node =>
      project.findParent(node.id) map { parent =>
        val newParent = parent.modifyChildren(_.filterNot(_.id == node.id))
        project.dynamicPut(newParent)
        project.tasks.remove(node.id)
        project.commitAllIfNonEmpty(c.originalCommandLine)
      }
    }
  }
}
