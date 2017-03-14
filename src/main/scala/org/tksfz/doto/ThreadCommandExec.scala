package org.tksfz.doto

import java.nio.file.Paths
import java.util.UUID

import org.tksfz.doto.repo.Repo

object ThreadCommandExec extends CommandExec[ThreadCmd] {
  override def execute(c: Config, add: ThreadCmd): Unit = {
    val repo = new Repo(Paths.get(""))
    val parentOpt = repo.threads.findByIdPrefix(add.parentId)
    parentOpt map { parent =>
      val uuid = UUID.randomUUID()
      val doc = Thread[Task](uuid, false, Some(IdRef(parent.id)), add.subject)
      repo.threads.put(uuid, doc)
    } getOrElse {
      println("Could not find parent with id starting with " + add.parentId)
    }
  }
}
