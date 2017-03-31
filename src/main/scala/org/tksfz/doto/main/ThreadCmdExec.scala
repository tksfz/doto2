package org.tksfz.doto.main

import java.nio.file.Paths
import java.util.UUID

import org.tksfz.doto._
import org.tksfz.doto.repo.Repo

object ThreadCmdExec extends CmdExec[ThreadCmd] {
  override def execute(c: Config, cmd: ThreadCmd): Unit = CommandWithActiveProject { repo =>
    val parentOpt = repo.threads.findByIdPrefix(cmd.parentId)
    parentOpt map { parent =>
      val uuid = UUID.randomUUID()
      val doc =
        if (cmd.isEvent) {
          Thread[Event](uuid, Some(IdRef(parent.id)), cmd.subject)
        } else {
          Thread[Task](uuid, Some(IdRef(parent.id)), cmd.subject)
        }
      repo.threads.put(uuid, doc)
    } getOrElse {
      println("Could not find parent with id starting with " + cmd.parentId)
    }
  }
}
