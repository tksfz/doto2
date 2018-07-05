package org.tksfz.doto.main

import java.util.UUID

import org.tksfz.doto.model._
import ModelExtensionsImplicits.HasContentExtensionMethods

object ThreadCmdExec extends CmdExec[ThreadCmd] {
  override def execute(c: Config, cmd: ThreadCmd): Unit = WithActiveProjectTxn { project =>
    val parentOpt = project.threads.findByIdPrefix(cmd.parentId)
    parentOpt map { parent =>
      val uuid = UUID.randomUUID()
      val doc =
        if (cmd.isEvent) {
          Thread[Event](uuid, Some(IdRef(parent.id))).withSubject(cmd.subject)
        } else {
          Thread[Task](uuid, Some(IdRef(parent.id))).withSubject(cmd.subject)
        }
      project.threads.put(uuid, doc)
      project.commitAllIfNonEmpty(c.originalCommandLine)
    } getOrElse {
      println("Could not find parent with id starting with " + cmd.parentId)
    }
  }
}
